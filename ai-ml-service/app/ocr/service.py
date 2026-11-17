import base64
import io
import logging
import os
import shutil
from typing import List, Optional, Tuple

import httpx
from fastapi import HTTPException, UploadFile, status

from app.core.config import settings
from app.ocr.card_parser import parse_cards_from_text, parse_llm_json_payload
from app.ocr.schemas import ExtractedCardItem, OcrExtractResponse, OcrTextBox

logger = logging.getLogger(__name__)

ALLOWED_MIME_TYPES = {
    "image/jpeg",
    "image/jpg",
    "image/png",
    "image/webp",
    "image/bmp",
    "image/tiff",
}

ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp", ".bmp", ".tiff"}

_PLACEHOLDER_KEYS = {"", "your_gemini_api_key_here", "your_openai_api_key_here", "changeme"}


def _is_configured_key(api_key: Optional[str]) -> bool:
    return bool(api_key and api_key.strip() and api_key.strip().lower() not in _PLACEHOLDER_KEYS)


def _infer_mime_type(filename: str, content_type: Optional[str]) -> str:
    if content_type and content_type.strip().lower() in ALLOWED_MIME_TYPES:
        normalized = content_type.strip().lower()
        return "image/jpeg" if normalized == "image/jpg" else normalized

    ext = os.path.splitext(filename.lower())[1]
    mapping = {
        ".jpg": "image/jpeg",
        ".jpeg": "image/jpeg",
        ".png": "image/png",
        ".webp": "image/webp",
        ".bmp": "image/bmp",
        ".tiff": "image/tiff",
    }
    return mapping.get(ext, "image/jpeg")


async def validate_image_upload(file: UploadFile) -> Tuple[bytes, str, str]:
    """Validate uploaded image file type and size; return (bytes, filename, mime_type)."""
    if file is None or not file.filename:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Vui lòng chọn tệp ảnh hợp lệ để quét OCR.",
        )

    filename = file.filename
    ext = os.path.splitext(filename.lower())[1]
    content_type = (file.content_type or "").strip().lower()

    is_valid_mime = content_type in ALLOWED_MIME_TYPES
    is_valid_ext = ext in ALLOWED_EXTENSIONS
    if not (is_valid_mime or (content_type in ("", "application/octet-stream") and is_valid_ext)):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Định dạng tệp không hợp lệ. Chỉ hỗ trợ ảnh JPG, PNG, WEBP, BMP hoặc TIFF.",
        )

    image_bytes = await file.read()
    if not image_bytes:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Tệp ảnh tải lên bị rỗng.",
        )

    max_bytes = settings.OCR_MAX_IMAGE_BYTES
    if len(image_bytes) > max_bytes:
        max_mb = max_bytes / (1024 * 1024)
        raise HTTPException(
            status_code=getattr(status, "HTTP_413_CONTENT_TOO_LARGE", 413),
            detail=f"Dung lượng ảnh vượt quá giới hạn tối đa {max_mb:.0f}MB.",
        )

    return image_bytes, filename, _infer_mime_type(filename, content_type)


def _build_vision_prompt(source_lang: Optional[str], target_lang: Optional[str]) -> str:
    lang_hint = ""
    if source_lang or target_lang:
        lang_hint = f" Ngôn ngữ thuật ngữ ưu tiên: {source_lang or 'auto'}, định nghĩa: {target_lang or 'auto'}."
    return (
        "Hãy đọc toàn bộ văn bản trong ảnh tài liệu học tập này và tách thành danh sách cặp flashcard "
        "(term - definition)." + lang_hint + " "
        "Trả về DUY NHẤT JSON hợp lệ theo đúng schema sau (không thêm markdown hay giải thích): "
        '{"raw_text": "...", "cards": [{"term": "...", "definition": "..."}]}'
    )


async def _extract_with_gemini_vision(
    image_bytes: bytes,
    mime_type: str,
    source_lang: Optional[str],
    target_lang: Optional[str],
) -> Optional[Tuple[str, List[ExtractedCardItem]]]:
    if not _is_configured_key(settings.GEMINI_API_KEY):
        return None

    url = (
        f"https://generativelanguage.googleapis.com/v1beta/models/"
        f"{settings.GEMINI_MODEL}:generateContent?key={settings.GEMINI_API_KEY}"
    )
    payload = {
        "contents": [
            {
                "parts": [
                    {"text": _build_vision_prompt(source_lang, target_lang)},
                    {
                        "inline_data": {
                            "mime_type": mime_type,
                            "data": base64.b64encode(image_bytes).decode("ascii"),
                        }
                    },
                ]
            }
        ],
        "generationConfig": {"temperature": 0.1},
    }

    try:
        async with httpx.AsyncClient(timeout=12.0) as client:
            resp = await client.post(url, json=payload)
            resp.raise_for_status()
            body = resp.json()
        text = (
            body.get("candidates", [{}])[0]
            .get("content", {})
            .get("parts", [{}])[0]
            .get("text", "")
        )
        return parse_llm_json_payload(text)
    except Exception as exc:
        logger.warning("Gemini Vision OCR failed, falling back: %s", exc)
        return None


async def _extract_with_openai_vision(
    image_bytes: bytes,
    mime_type: str,
    source_lang: Optional[str],
    target_lang: Optional[str],
) -> Optional[Tuple[str, List[ExtractedCardItem]]]:
    if not _is_configured_key(settings.OPENAI_API_KEY):
        return None

    b64_image = base64.b64encode(image_bytes).decode("ascii")
    payload = {
        "model": settings.OPENAI_MODEL,
        "temperature": 0.1,
        "messages": [
            {
                "role": "user",
                "content": [
                    {"type": "text", "text": _build_vision_prompt(source_lang, target_lang)},
                    {
                        "type": "image_url",
                        "image_url": {"url": f"data:{mime_type};base64,{b64_image}"},
                    },
                ],
            }
        ],
    }

    try:
        async with httpx.AsyncClient(timeout=12.0) as client:
            resp = await client.post(
                "https://api.openai.com/v1/chat/completions",
                headers={"Authorization": f"Bearer {settings.OPENAI_API_KEY}"},
                json=payload,
            )
            resp.raise_for_status()
            body = resp.json()
        text = body.get("choices", [{}])[0].get("message", {}).get("content", "")
        return parse_llm_json_payload(text)
    except Exception as exc:
        logger.warning("OpenAI Vision OCR failed, falling back: %s", exc)
        return None


def _extract_with_paddleocr(image_bytes: bytes) -> Optional[str]:
    try:
        from paddleocr import PaddleOCR  # type: ignore
        import numpy as np  # type: ignore
        from PIL import Image  # type: ignore
    except ImportError:
        return None

    try:
        image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
        ocr = PaddleOCR(use_angle_cls=True, lang="en", show_log=False)
        result = ocr.ocr(np.array(image), cls=True)
        lines: List[str] = []
        for page in result or []:
            for line in page or []:
                if len(line) >= 2 and isinstance(line[1], (list, tuple)) and line[1]:
                    lines.append(str(line[1][0]))
        return "\n".join(lines).strip()
    except Exception as exc:
        logger.warning("PaddleOCR failed, falling back: %s", exc)
        return None


_rapidocr_engine = None
_vietocr_predictor = None
_vietocr_init_attempted = False


def _get_vietocr_predictor():
    global _vietocr_predictor, _vietocr_init_attempted
    if _vietocr_init_attempted:
        return _vietocr_predictor
    _vietocr_init_attempted = True
    try:
        import tempfile
        from vietocr.tool.config import Cfg  # type: ignore
        from vietocr.tool.predictor import Predictor  # type: ignore

        local_weights = os.path.join(tempfile.gettempdir(), "vgg_transformer.pth")
        weights_source = (
            local_weights
            if os.path.exists(local_weights) and os.path.getsize(local_weights) > 10_000_000
            else "https://vocr.vn/data/vietocr/vgg_transformer.pth"
        )

        cfg = Cfg(
            {
                "vocab": "aAàÀảẢãÃáÁạẠăĂằẰẳẲẵẴắẮặẶâÂầẦẩẨẫẪấẤậẬbBcCdDđĐeEèÈẻẺẽẼéÉẹẸêÊềỀểỂễỄếẾệỆfFgGhHiIìÌỉỈĩĨíÍịỊjJkKlLmMnNoOòÒỏỎõÕóÓọỌôÔồỒổỔỗỖốỐộỘơƠờỜởỞỡỠớỚợỢpPqQrRsStTuUùÙủỦũŨúÚụỤưƯừỪửỬữỮứỨựỰvVwWxXyYỳỲỷỶỹỸýÝỵỴzZ0123456789!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~ ",
                "device": "cpu",
                "seq_modeling": "transformer",
                "transformer": {
                    "d_model": 256,
                    "nhead": 8,
                    "num_encoder_layers": 6,
                    "num_decoder_layers": 6,
                    "dim_feedforward": 2048,
                    "max_seq_length": 1024,
                    "pos_dropout": 0.1,
                    "trans_dropout": 0.1,
                },
                "dataset": {
                    "name": "data",
                    "image_height": 32,
                    "image_min_width": 32,
                    "image_max_width": 512,
                },
                "predictor": {"beamsearch": False},
                "quiet": True,
                "pretrain": weights_source,
                "weights": weights_source,
                "backbone": "vgg19_bn",
                "cnn": {
                    "pretrained": False,
                    "ss": [[2, 2], [2, 2], [2, 1], [2, 1], [1, 1]],
                    "ks": [[2, 2], [2, 2], [2, 1], [2, 1], [1, 1]],
                    "hidden": 256,
                },
            }
        )
        _vietocr_predictor = Predictor(cfg)
        logger.info("Initialized VietOCR (vgg_transformer) offline from %s", weights_source)
        return _vietocr_predictor
    except Exception as exc:
        logger.warning("VietOCR unavailable, falling back to RapidOCR rec: %s", exc)
        _vietocr_predictor = None
        return None


def _recognize_crop_text(engine, vietocr_pred, crop_rgb, fallback_text: str = "") -> str:
    """Recognize a horizontal RGB crop using VietOCR (full Vietnamese diacritics) with RapidOCR fallback."""
    if vietocr_pred is not None:
        try:
            from PIL import Image  # type: ignore

            pil_crop = Image.fromarray(crop_rgb)
            text = str(vietocr_pred.predict(pil_crop)).strip()
            if text:
                return text
        except Exception as exc:
            logger.debug("VietOCR crop predict failed, using RapidOCR rec: %s", exc)

    if fallback_text:
        return fallback_text
    rec_res, _ = engine(crop_rgb, use_det=False, use_cls=False, use_rec=True)
    if rec_res and len(rec_res) > 0 and rec_res[0]:
        return str(rec_res[0][0]).strip()
    return ""


def _get_rapidocr_engine():
    global _rapidocr_engine
    if _rapidocr_engine is not None:
        return _rapidocr_engine
    try:
        from rapidocr_onnxruntime import RapidOCR  # type: ignore

        _rapidocr_engine = RapidOCR(
            det_db_thresh=0.16,
            det_db_box_thresh=0.28,
            det_db_unclip_ratio=1.15,
            use_angle_cls=True,
        )
        return _rapidocr_engine
    except Exception as exc:
        logger.warning("Failed to initialize RapidOCR: %s", exc)
        return None


def _estimate_dominant_tilt_angle(img_rgb) -> float:
    """Estimate dominant text/line tilt angle in degrees (-75..75) using Hough lines."""
    import cv2  # type: ignore
    import numpy as np  # type: ignore

    gray = cv2.cvtColor(img_rgb, cv2.COLOR_RGB2GRAY)
    h, w = gray.shape
    scale = min(1.0, 800.0 / max(w, h))
    if scale < 1.0:
        gray = cv2.resize(gray, (int(w * scale), int(h * scale)))

    edges = cv2.Canny(gray, 50, 150, apertureSize=3)
    lines = cv2.HoughLinesP(
        edges,
        1,
        np.pi / 180.0,
        threshold=60,
        minLineLength=max(40, int(min(gray.shape) * 0.12)),
        maxLineGap=15,
    )
    if lines is None or len(lines) == 0:
        return 0.0

    angles = []
    weights = []
    for x1, y1, x2, y2 in lines.reshape(-1, 4):
        dx = float(x2 - x1)
        dy = float(y2 - y1)
        length = (dx * dx + dy * dy) ** 0.5
        if length < 20:
            continue
        angle = float(np.degrees(np.arctan2(dy, dx)))
        # Normalize to [-90, 90]
        if angle > 90:
            angle -= 180
        elif angle < -90:
            angle += 180
        # Ignore near-vertical page edges (> 72 deg) unless dominant
        if abs(angle) <= 72:
            angles.append(angle)
            weights.append(length)

    if not angles:
        return 0.0

    # Weighted histogram in 3-degree bins to find dominant baseline angle
    bins = np.arange(-75, 78, 3)
    hist, edges_bin = np.histogram(angles, bins=bins, weights=weights)
    best_idx = int(np.argmax(hist))
    dominant = float((edges_bin[best_idx] + edges_bin[best_idx + 1]) / 2.0)
    return dominant if abs(dominant) >= 5.0 else 0.0


def _rotate_image_with_matrix(img_rgb, angle_deg: float):
    """Rotate image by angle_deg (counter-clockwise) with expanded canvas and return (rotated_img, M_forward)."""
    import cv2  # type: ignore
    import numpy as np  # type: ignore

    h, w = img_rgb.shape[:2]
    cx, cy = w / 2.0, h / 2.0
    M = cv2.getRotationMatrix2D((cx, cy), angle_deg, 1.0)
    cos = abs(M[0, 0])
    sin = abs(M[0, 1])
    new_w = int(round((h * sin) + (w * cos)))
    new_h = int(round((h * cos) + (w * sin)))
    M[0, 2] += (new_w / 2.0) - cx
    M[1, 2] += (new_h / 2.0) - cy
    rotated = cv2.warpAffine(
        img_rgb,
        M,
        (new_w, new_h),
        flags=cv2.INTER_CUBIC,
        borderMode=cv2.BORDER_CONSTANT,
        borderValue=(255, 255, 255),
    )
    return rotated, M


def _preprocess_image_for_ocr(image_bytes: bytes):
    """EXIF-orient, upscale small images, and enhance contrast via CLAHE while preserving exact aspect/coordinates."""
    import cv2  # type: ignore
    import numpy as np  # type: ignore
    from PIL import Image, ImageOps  # type: ignore

    pil_img = ImageOps.exif_transpose(Image.open(io.BytesIO(image_bytes))).convert("RGB")
    img_np = np.array(pil_img)
    h, w = img_np.shape[:2]
    if w <= 0 or h <= 0:
        return None

    max_dim = max(w, h)
    if max_dim < 1400:
        scale = min(2.0, 1400.0 / max_dim)
        img_np = cv2.resize(
            img_np,
            (int(round(w * scale)), int(round(h * scale))),
            interpolation=cv2.INTER_CUBIC,
        )

    # CLAHE on L channel in LAB space improves uneven lighting & faint handwriting
    lab = cv2.cvtColor(img_np, cv2.COLOR_RGB2LAB)
    l_chan, a_chan, b_chan = cv2.split(lab)
    clahe = cv2.createCLAHE(clipLimit=2.2, tileGridSize=(8, 8))
    l_enhanced = clahe.apply(l_chan)
    merged = cv2.merge((l_enhanced, a_chan, b_chan))
    enhanced_rgb = cv2.cvtColor(merged, cv2.COLOR_LAB2RGB)

    # Mild unsharp mask for crisp handwritten strokes
    blurred = cv2.GaussianBlur(enhanced_rgb, (0, 0), sigmaX=1.0)
    sharpened = cv2.addWeighted(enhanced_rgb, 1.35, blurred, -0.35, 0)
    return sharpened


def _clamp01(value: float) -> float:
    return max(0.0, min(1.0, float(value)))


def _split_line_into_word_boxes(
    line_index: int,
    text: str,
    x: float,
    y: float,
    width: float,
    height: float,
    confidence: float,
) -> List[OcrTextBox]:
    """Split a recognized line box into word-level boxes for smooth swipe-to-select."""
    import re

    words = [w.strip() for w in re.split(r"\s+|(?<=,)(?=\S)", text.strip()) if w.strip()]
    if not words:
        return []
    if len(words) == 1:
        return [
            OcrTextBox(
                id=f"box_{line_index}_0",
                text=words[0],
                x=_clamp01(x),
                y=_clamp01(y),
                width=_clamp01(width),
                height=_clamp01(height),
                confidence=_clamp01(confidence),
            )
        ]

    total_chars = sum(len(w) for w in words) + max(0, len(words) - 1) * 0.45
    if total_chars <= 0:
        total_chars = len(words)

    boxes: List[OcrTextBox] = []
    cursor_x = x
    for w_idx, word in enumerate(words):
        word_ratio = len(word) / total_chars
        space_ratio = 0.45 / total_chars
        word_w = width * word_ratio
        boxes.append(
            OcrTextBox(
                id=f"box_{line_index}_{w_idx}",
                text=word,
                x=_clamp01(cursor_x),
                y=_clamp01(y),
                width=_clamp01(word_w),
                height=_clamp01(height),
                confidence=_clamp01(confidence),
            )
        )
        cursor_x += word_w + (width * space_ratio)
    return boxes


def _split_wide_glued_line_by_ink(
    engine,
    vietocr_pred,
    cand_img,
    min_x: float,
    min_y: float,
    max_x: float,
    max_y: float,
    text: str,
):
    """If RapidOCR glued multiple words without spaces in a wide box, split by vertical ink gaps and re-recognize."""
    import cv2  # type: ignore
    import numpy as np  # type: ignore

    line_w = max_x - min_x
    line_h = max(1.0, max_y - min_y)
    if line_w / line_h < 3.8 or len(text) < 8:
        return None

    h_img, w_img = cand_img.shape[:2]
    x1 = max(0, int(round(min_x)))
    y1 = max(0, int(round(min_y)))
    x2 = min(w_img, int(round(max_x)))
    y2 = min(h_img, int(round(max_y)))
    if x2 - x1 < 40 or y2 - y1 < 12:
        return None

    crop = cand_img[y1:y2, x1:x2]
    gray = cv2.cvtColor(crop, cv2.COLOR_RGB2GRAY)
    # Adaptive threshold to isolate dark ink strokes
    bw = cv2.adaptiveThreshold(
        gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY_INV, 15, 8
    )
    col_ink = np.sum(bw > 0, axis=0)
    empty_cols = col_ink <= max(1, int(0.05 * (y2 - y1)))

    # Find contiguous empty-column runs wide enough to be word spaces (>= 0.32 * line_h)
    min_gap_px = max(6, int(0.32 * (y2 - y1)))
    gaps = []
    run_start = None
    for idx, is_empty in enumerate(empty_cols):
        if is_empty and run_start is None:
            run_start = idx
        elif not is_empty and run_start is not None:
            if idx - run_start >= min_gap_px and run_start > 4 and idx < (x2 - x1 - 4):
                gaps.append((run_start + idx) / 2.0)
            run_start = None

    if not gaps:
        return None

    split_xs = [0.0] + gaps + [float(x2 - x1)]
    word_polys = []
    for i in range(len(split_xs) - 1):
        sx1 = int(round(split_xs[i]))
        sx2 = int(round(split_xs[i + 1]))
        if sx2 - sx1 < 8:
            continue
        sub_crop = crop[:, sx1:sx2]
        sub_text = _recognize_crop_text(engine, vietocr_pred, sub_crop)
        if not sub_text:
            continue
        wx1 = min_x + sx1
        wx2 = min_x + sx2
        poly = np.array(
            [[wx1, min_y], [wx2, min_y], [wx2, max_y], [wx1, max_y]],
            dtype=np.float32,
        )
        word_polys.append((sub_text, poly))

    return word_polys if len(word_polys) >= 2 else None


def _split_line_polygon_into_word_polygons(
    engine, vietocr_pred, cand_img, pts_arr, text: str
) -> List[Tuple[str, object]]:
    """Split a horizontal/deskewed 4-point line polygon into individual word polygons BEFORE inverse-rotating."""
    import re
    import numpy as np  # type: ignore

    words = [w.strip() for w in re.split(r"\s+|(?<=[,;:])(?=\S)", text.strip()) if w.strip()]
    if not words:
        return []

    xs = [float(p[0]) for p in pts_arr]
    ys = [float(p[1]) for p in pts_arr]
    min_x, max_x = min(xs), max(xs)
    min_y, max_y = min(ys), max(ys)
    line_w = max(1.0, max_x - min_x)

    if len(words) == 1:
        ink_split = _split_wide_glued_line_by_ink(
            engine, vietocr_pred, cand_img, min_x, min_y, max_x, max_y, words[0]
        )
        if ink_split:
            return ink_split
        poly = np.array(
            [[min_x, min_y], [max_x, min_y], [max_x, max_y], [min_x, max_y]],
            dtype=np.float32,
        )
        return [(words[0], poly)]

    gap_weight = 0.45
    total_units = sum(len(w) for w in words) + max(0, len(words) - 1) * gap_weight
    if total_units <= 0:
        total_units = float(len(words))

    word_polys: List[Tuple[str, object]] = []
    cursor_x = min_x
    for word in words:
        w_ratio = len(word) / total_units
        g_ratio = gap_weight / total_units
        wx1 = cursor_x
        wx2 = cursor_x + line_w * w_ratio
        poly = np.array(
            [[wx1, min_y], [wx2, min_y], [wx2, max_y], [wx1, max_y]],
            dtype=np.float32,
        )
        word_polys.append((word, poly))
        cursor_x = wx2 + line_w * g_ratio

    return word_polys


def _run_rapidocr_with_optional_deskew(engine, img_np, orig_w: int, orig_h: int):
    """Run RapidOCR on 0 deg, deskewed image, and shadow-normalized fallback; project rotated word boxes back."""
    import cv2  # type: ignore
    import numpy as np  # type: ignore

    tilt = _estimate_dominant_tilt_angle(img_np)
    candidates = [(0.0, img_np, None)]
    if abs(tilt) >= 6.0:
        rot_img, M_fwd = _rotate_image_with_matrix(img_np, tilt)
        M_inv = cv2.invertAffineTransform(M_fwd)
        candidates.insert(0, (tilt, rot_img, M_inv))

    best_lines = []
    best_boxes: List[OcrTextBox] = []
    best_score = -1.0

    vietocr_pred = _get_vietocr_predictor()

    def evaluate_candidate(angle_deg, cand_img, M_inv):
        result, _ = engine(cand_img, use_det=True, use_cls=True, use_rec=True)
        parsed_lines = []
        parsed_words = []
        total_chars = 0
        conf_sum = 0.0
        h_cand, w_cand = cand_img.shape[:2]

        for line_idx, item in enumerate(result or []):
            if not item or len(item) < 2:
                continue
            pts = item[0]
            rapid_text = str(item[1]).strip()
            score = float(item[2]) if len(item) >= 3 and item[2] is not None else 0.85
            if not pts:
                continue

            pts_arr = np.array(pts, dtype=np.float32)
            # Re-recognize line crop using VietOCR so Vietnamese diacritics are preserved accurately
            lxs = [float(p[0]) for p in pts_arr]
            lys = [float(p[1]) for p in pts_arr]
            lx1 = max(0, int(round(min(lxs))) - 3)
            ly1 = max(0, int(round(min(lys))) - 3)
            lx2 = min(w_cand, int(round(max(lxs))) + 3)
            ly2 = min(h_cand, int(round(max(lys))) + 3)
            if vietocr_pred is not None and (lx2 - lx1) >= 8 and (ly2 - ly1) >= 8:
                line_crop = cand_img[ly1:ly2, lx1:lx2]
                text = _recognize_crop_text(engine, vietocr_pred, line_crop, fallback_text=rapid_text)
            else:
                text = rapid_text

            if not text:
                continue

            word_polys = _split_line_polygon_into_word_polygons(
                engine, vietocr_pred, cand_img, pts_arr, text
            )

            line_ys = [float(p[1]) for p in pts_arr]
            line_xs = [float(p[0]) for p in pts_arr]
            sort_y = sum(line_ys) / len(line_ys)
            sort_x = sum(line_xs) / len(line_xs)
            parsed_lines.append((sort_y, sort_x, text))

            for w_idx, (word_text, w_poly) in enumerate(word_polys):
                wxs_local = [float(p[0]) for p in w_poly]
                wys_local = [float(p[1]) for p in w_poly]
                local_w = max(4.0, max(wxs_local) - min(wxs_local))
                local_h = max(4.0, max(wys_local) - min(wys_local))
                local_cx = (min(wxs_local) + max(wxs_local)) / 2.0
                local_cy = (min(wys_local) + max(wys_local)) / 2.0

                if M_inv is not None:
                    center_h = np.array([[local_cx, local_cy, 1.0]], dtype=np.float32)
                    orig_center = (center_h @ M_inv.T)[0]
                    orig_cx, orig_cy = float(orig_center[0]), float(orig_center[1])
                else:
                    orig_cx, orig_cy = local_cx, local_cy

                norm_w = _clamp01(local_w / orig_w)
                norm_h = _clamp01(local_h / orig_h)
                norm_x = _clamp01((orig_cx / orig_w) - norm_w / 2.0)
                norm_y = _clamp01((orig_cy / orig_h) - norm_h / 2.0)

                parsed_words.append(
                    (
                        sort_y,
                        sort_x + w_idx * 0.001,
                        OcrTextBox(
                            id=f"box_{line_idx}_{w_idx}",
                            text=word_text,
                            x=norm_x,
                            y=norm_y,
                            width=max(0.008, norm_w),
                            height=max(0.010, norm_h),
                            angle=round(float(angle_deg), 2),
                            confidence=_clamp01(score),
                        ),
                    )
                )

            total_chars += len(text)
            conf_sum += score * len(text)

        return (conf_sum if total_chars > 0 else 0.0), parsed_lines, parsed_words

    for angle_deg, cand_img, M_inv in candidates:
        cand_metric, parsed_lines, parsed_words = evaluate_candidate(angle_deg, cand_img, M_inv)
        if cand_metric > best_score:
            best_score = cand_metric
            parsed_lines.sort(key=lambda it: (round(it[0] / 25.0), it[1]))
            parsed_words.sort(key=lambda it: (round(it[0] / 25.0), it[1]))
            best_lines = [it[2] for it in parsed_lines]
            best_boxes = [it[2] for it in parsed_words]

        if M_inv is not None and len(parsed_lines) >= 4 and cand_metric > 25.0:
            break

    # If difficult/shaded photo yielded sparse results, retry with shadow-normalized image
    if best_score < 20.0 or len(best_boxes) < 4:
        norm_img = _build_shadow_normalized_image(img_np)
        if abs(tilt) >= 6.0:
            rot_norm, M_fwd = _rotate_image_with_matrix(norm_img, tilt)
            M_inv = cv2.invertAffineTransform(M_fwd)
            norm_metric, parsed_lines, parsed_words = evaluate_candidate(tilt, rot_norm, M_inv)
        else:
            norm_metric, parsed_lines, parsed_words = evaluate_candidate(0.0, norm_img, None)

        if norm_metric > best_score:
            parsed_lines.sort(key=lambda it: (round(it[0] / 25.0), it[1]))
            parsed_words.sort(key=lambda it: (round(it[0] / 25.0), it[1]))
            best_lines = [it[2] for it in parsed_lines]
            best_boxes = [it[2] for it in parsed_words]

    return "\n".join(best_lines).strip(), best_boxes


def _extract_with_rapidocr(image_bytes: bytes) -> Optional[Tuple[str, List[OcrTextBox]]]:
    engine = _get_rapidocr_engine()
    if engine is None:
        return None

    try:
        img_np = _preprocess_image_for_ocr(image_bytes)
        if img_np is None:
            return None
        img_h, img_w = img_np.shape[:2]

        return _run_rapidocr_with_optional_deskew(engine, img_np, img_w, img_h)
    except Exception as exc:
        logger.warning("RapidOCR extraction failed, falling back: %s", exc)
        return None


def _extract_with_tesseract(image_bytes: bytes) -> Optional[str]:
    try:
        import pytesseract  # type: ignore
        from PIL import Image  # type: ignore
    except ImportError:
        return None

    tesseract_cmd = settings.TESSERACT_CMD or "tesseract"
    if not os.path.isabs(tesseract_cmd) and shutil.which(tesseract_cmd) is None:
        return None
    if os.path.isabs(tesseract_cmd) and not os.path.exists(tesseract_cmd):
        return None

    try:
        pytesseract.pytesseract.tesseract_cmd = tesseract_cmd
        image = Image.open(io.BytesIO(image_bytes))
        text = pytesseract.image_to_string(image)
        return text.strip()
    except Exception as exc:
        logger.warning("Tesseract OCR failed, falling back: %s", exc)
        return None


async def process_ocr_extraction(
    file: UploadFile,
    source_lang: Optional[str] = None,
    target_lang: Optional[str] = None,
) -> OcrExtractResponse:
    """
    Execute the multi-tier OCR + Card Parser pipeline with graceful fallbacks.

    Order:
    1. Gemini Vision (if GEMINI_API_KEY configured)
    2. OpenAI Vision (if OPENAI_API_KEY configured)
    3. PaddleOCR (if installed)
    4. Tesseract OCR (if Pillow + pytesseract + tesseract binary available)
    5. Graceful fallback (returns empty cards with informative warnings, never crashes)
    """
    image_bytes, filename, mime_type = await validate_image_upload(file)
    warnings: List[str] = []

    # Tier 1: Vision LLMs (direct text + structured cards)
    gemini_result = await _extract_with_gemini_vision(image_bytes, mime_type, source_lang, target_lang)
    if gemini_result is not None:
        raw_text, cards = gemini_result
        if not cards and raw_text:
            cards, parse_warnings = parse_cards_from_text(raw_text)
            warnings.extend(parse_warnings)
        rapid_boxes_result = _extract_with_rapidocr(image_bytes)
        boxes = rapid_boxes_result[1] if rapid_boxes_result is not None else []
        confidence = 0.92 if cards else 0.45
        return OcrExtractResponse(
            success=True,
            raw_text=raw_text,
            cards=cards,
            boxes=boxes,
            confidence=confidence,
            engine="gemini_vision",
            warnings=warnings,
        )

    openai_result = await _extract_with_openai_vision(image_bytes, mime_type, source_lang, target_lang)
    if openai_result is not None:
        raw_text, cards = openai_result
        if not cards and raw_text:
            cards, parse_warnings = parse_cards_from_text(raw_text)
            warnings.extend(parse_warnings)
        rapid_boxes_result = _extract_with_rapidocr(image_bytes)
        boxes = rapid_boxes_result[1] if rapid_boxes_result is not None else []
        confidence = 0.90 if cards else 0.45
        return OcrExtractResponse(
            success=True,
            raw_text=raw_text,
            cards=cards,
            boxes=boxes,
            confidence=confidence,
            engine="openai_vision",
            warnings=warnings,
        )

    # Tier 2: Local OCR Engines + Deterministic Card Parser
    paddle_text = _extract_with_paddleocr(image_bytes)
    if paddle_text is not None:
        cards, parse_warnings = parse_cards_from_text(paddle_text)
        warnings.extend(parse_warnings)
        confidence = 0.80 if cards else 0.35
        return OcrExtractResponse(
            success=True,
            raw_text=paddle_text,
            cards=cards,
            boxes=[],
            confidence=confidence,
            engine="paddleocr",
            warnings=warnings,
        )

    rapid_result = _extract_with_rapidocr(image_bytes)
    if rapid_result is not None:
        rapid_text, rapid_boxes = rapid_result
        cards, parse_warnings = parse_cards_from_text(rapid_text) if rapid_text else ([], [])
        if not rapid_text:
            warnings.append("Không phát hiện văn bản rõ ràng trong ảnh.")
        elif not cards:
            warnings.extend(parse_warnings)
        confidence = 0.85 if rapid_boxes else 0.25
        return OcrExtractResponse(
            success=True,
            raw_text=rapid_text,
            cards=cards,
            boxes=rapid_boxes,
            confidence=confidence,
            engine="rapidocr",
            warnings=warnings,
        )

    tesseract_text = _extract_with_tesseract(image_bytes)
    if tesseract_text is not None:
        cards, parse_warnings = parse_cards_from_text(tesseract_text)
        warnings.extend(parse_warnings)
        confidence = 0.75 if cards else 0.30
        return OcrExtractResponse(
            success=True,
            raw_text=tesseract_text,
            cards=cards,
            confidence=confidence,
            engine="tesseract",
            warnings=warnings,
        )

    # Tier 3: Graceful Fallback when no engine/key is installed locally
    warnings.append(
        f"Đã nhận ảnh '{filename}' nhưng chưa có engine OCR (Tesseract/PaddleOCR) hoặc API key "
        "(GEMINI_API_KEY / OPENAI_API_KEY) được cấu hình trên máy. "
        "Hệ thống trả về kết quả rỗng an toàn để bạn nhập hoặc cấu hình engine sau."
    )
    return OcrExtractResponse(
        success=True,
        raw_text="",
        cards=[],
        confidence=0.0,
        engine="fallback",
        warnings=warnings,
    )

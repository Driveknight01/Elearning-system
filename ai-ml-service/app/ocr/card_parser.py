import json
import re
from typing import List, Optional, Tuple

from app.ocr.schemas import ExtractedCardItem, OcrTextBox

# Prefix patterns like "1.", "1)", "- ", "* ", "• "
_BULLET_PREFIX_RE = re.compile(r"^\s*(?:\d{1,3}[.)]|[-*•▪◦])\s+")

# Common separator patterns between term and definition (ordered by specificity)
_SEPARATOR_PATTERNS = [
    re.compile(r"^(.+?)\t+(.+)$"),  # Tab-separated
    re.compile(r"^(.+?)\s+[—–-]\s+(.+)$"),  # Dash / en-dash / em-dash with spaces
    re.compile(r"^(.+?)\s*:(?!//)\s*(.+)$"),  # Colon (with or without space after, excluding URLs)
    re.compile(r"^(.+?)\s*=\s*(.+)$"),  # Equals sign
    re.compile(r"^(.+?)\s*->\s*(.+)$"),  # Arrow
]


def clean_line(line: str) -> str:
    """Normalize horizontal spaces (preserving tabs for tab-separated pairs) and strip list prefixes."""
    cleaned = _BULLET_PREFIX_RE.sub("", line.strip())
    return re.sub(r" {2,}", " ", cleaned).strip()


def parse_cards_from_text(raw_text: str) -> Tuple[List[ExtractedCardItem], List[str]]:
    """
    Parse raw OCR text into structured flashcard term-definition pairs.

    Supports tab-separated, dash-separated, colon-separated, equals, and arrow formats.
    Returns (cards, warnings).
    """
    if not raw_text or not raw_text.strip():
        return [], ["Không trích xuất được văn bản từ ảnh."]

    cards: List[ExtractedCardItem] = []
    seen_terms = set()
    unmatched_lines: List[str] = []

    for raw_line in raw_text.splitlines():
        line = clean_line(raw_line)
        if not line or len(line) < 3:
            continue

        matched = False
        for pattern in _SEPARATOR_PATTERNS:
            match = pattern.match(line)
            if match:
                term = match.group(1).strip(" -:\t")
                definition = match.group(2).strip(" -:\t")
                if term and definition:
                    key = term.lower()
                    if key not in seen_terms:
                        seen_terms.add(key)
                        cards.append(ExtractedCardItem(term=term, definition=definition))
                    matched = True
                    break

        if not matched:
            unmatched_lines.append(line)

    warnings: List[str] = []
    if not cards:
        warnings.append(
            "Đã trích xuất văn bản nhưng không nhận diện được cặp Thuật ngữ - Định nghĩa rõ ràng. "
            "Vui lòng kiểm tra lại định dạng hoặc chỉnh sửa thủ công."
        )
    elif unmatched_lines:
        warnings.append(
            f"Có {len(unmatched_lines)} dòng không khớp định dạng cặp thẻ và đã được bỏ qua."
        )

    return cards, warnings


def parse_llm_json_payload(payload_text: str) -> Optional[Tuple[str, List[ExtractedCardItem]]]:
    """
    Parse a JSON payload returned by a Vision/Text LLM into (raw_text, cards).
    Handles optional ```json ... ``` markdown fences.
    """
    if not payload_text or not payload_text.strip():
        return None

    cleaned = payload_text.strip()
    fence_match = re.search(r"```(?:json)?\s*([\s\S]*?)\s*```", cleaned, re.IGNORECASE)
    if fence_match:
        cleaned = fence_match.group(1).strip()

    try:
        data = json.loads(cleaned)
    except json.JSONDecodeError:
        return None

    if not isinstance(data, dict):
        return None

    raw_text = str(data.get("raw_text") or "").strip()
    raw_cards = data.get("cards")
    cards: List[ExtractedCardItem] = []
    if isinstance(raw_cards, list):
        for item in raw_cards:
            if not isinstance(item, dict):
                continue
            term = str(item.get("term") or "").strip()
            definition = str(item.get("definition") or "").strip()
            if term and definition:
                cards.append(ExtractedCardItem(term=term, definition=definition))

    return raw_text, cards


def parse_cards_from_boxes(boxes: List[OcrTextBox]) -> List[ExtractedCardItem]:
    """
    Extract term-definition pairs from 2-column layouts using normalized bounding boxes.
    Uses global column split alignment (handles mixed long/short rows and wrapped definitions)
    with per-row adaptive gap fallback.
    """
    if not boxes:
        return []

    valid = [b for b in boxes if b.text and b.text.strip() and b.height > 0 and b.width > 0]
    if not valid:
        return []

    valid.sort(key=lambda b: (b.y + b.height / 2.0, b.x))

    # Group into horizontal rows by vertical center proximity
    rows: List[List[OcrTextBox]] = []
    for box in valid:
        cy = box.y + box.height / 2.0
        placed = False
        for row in rows:
            avg_cy = sum(item.y + item.height / 2.0 for item in row) / len(row)
            avg_h = sum(item.height for item in row) / len(row)
            if abs(cy - avg_cy) <= max(avg_h * 0.65, 0.015):
                row.append(box)
                placed = True
                break
        if not placed:
            rows.append([box])

    for row in rows:
        row.sort(key=lambda b: b.x)

    # First check if rows contain explicit separators
    row_lines = [" ".join(b.text.strip() for b in row if b.text.strip()) for row in rows]
    sep_cards, _ = parse_cards_from_text("\n".join(row_lines))
    if sep_cards:
        return sep_cards

    # Find candidate column gaps across all rows to detect a consistent vertical split X
    candidate_splits: List[Tuple[float, float]] = []
    for row in rows:
        if len(row) < 2:
            continue
        avg_h = sum(item.height for item in row) / len(row)
        gaps = [row[i + 1].x - (row[i].x + row[i].width) for i in range(len(row) - 1)]
        positive_gaps = sorted(g for g in gaps if g > 0)
        median_space = positive_gaps[len(positive_gaps) // 2] if positive_gaps else avg_h * 0.25
        min_col_gap = max(avg_h * 0.9, median_space * 2.1, 0.035)
        for i, gap in enumerate(gaps):
            if gap >= min_col_gap:
                split_mid = (row[i].x + row[i].width + row[i + 1].x) / 2.0
                candidate_splits.append((split_mid, gap))

    global_split_x: Optional[float] = None
    if len(candidate_splits) >= 2:
        best_cluster: List[float] = []
        for mid, _ in candidate_splits:
            cluster = [m for m, _ in candidate_splits if abs(m - mid) <= 0.08]
            if len(cluster) > len(best_cluster):
                best_cluster = cluster
        if len(best_cluster) >= 2:
            global_split_x = sum(best_cluster) / len(best_cluster)

    cards: List[ExtractedCardItem] = []
    seen_terms = set()

    for row in rows:
        left_parts: List[str] = []
        right_parts: List[str] = []

        if global_split_x is not None:
            for b in row:
                cx = b.x + b.width / 2.0
                if cx < global_split_x:
                    left_parts.append(b.text.strip())
                else:
                    right_parts.append(b.text.strip())

            # Wrapped definition continuation: row has only right-column text
            if not left_parts and right_parts and cards:
                cards[-1].definition = f"{cards[-1].definition} {' '.join(right_parts)}".strip()
                continue
        else:
            if len(row) < 2:
                continue
            avg_h = sum(item.height for item in row) / len(row)
            gaps = [row[i + 1].x - (row[i].x + row[i].width) for i in range(len(row) - 1)]
            positive_gaps = sorted(g for g in gaps if g > 0)
            median_space = positive_gaps[len(positive_gaps) // 2] if positive_gaps else avg_h * 0.25
            min_col_gap = max(avg_h * 1.1, median_space * 2.2, 0.035)
            max_gap = max(gaps)
            split_idx = gaps.index(max_gap)
            if max_gap >= min_col_gap:
                left_parts = [b.text.strip() for b in row[: split_idx + 1]]
                right_parts = [b.text.strip() for b in row[split_idx + 1 :]]

        left_text = " ".join(left_parts).strip(" -:\t")
        right_text = " ".join(right_parts).strip(" -:\t")
        if left_text and right_text:
            key = left_text.lower()
            if key not in seen_terms:
                seen_terms.add(key)
                cards.append(ExtractedCardItem(term=left_text, definition=right_text))

    return cards

import json
import re
from typing import List, Optional, Tuple

from app.ocr.schemas import ExtractedCardItem

# Prefix patterns like "1.", "1)", "- ", "* ", "• "
_BULLET_PREFIX_RE = re.compile(r"^\s*(?:\d{1,3}[.)]|[-*•▪◦])\s+")

# Common separator patterns between term and definition (ordered by specificity)
_SEPARATOR_PATTERNS = [
    re.compile(r"^(.+?)\t+(.+)$"),  # Tab-separated
    re.compile(r"^(.+?)\s+[—–-]\s+(.+)$"),  # Dash / en-dash / em-dash with spaces
    re.compile(r"^(.+?)\s*:\s+(.+)$"),  # Colon with space after
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

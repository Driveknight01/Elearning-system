/**
 * Utilities for parsing copy-pasted text (Tab, CSV, Semicolon, Dash, Colon, or custom delimiters)
 * into flashcard term-definition pairs, and normalizing prefillData from AI Chatbot / OCR.
 */

export const TERM_DELIMITERS = [
  { id: 'tab', label: 'Tab (Excel / Google Sheets)', value: '\t' },
  { id: 'comma', label: 'Dấu phẩy (CSV)', value: ',' },
  { id: 'semicolon', label: 'Dấu chấm phẩy (;)', value: ';' },
  { id: 'dash', label: 'Gạch ngang ( - )', value: ' - ' },
  { id: 'colon', label: 'Hai chấm (:)', value: ':' },
  { id: 'custom', label: 'Tùy chỉnh', value: '' },
];

export const CARD_DELIMITERS = [
  { id: 'newline', label: 'Xuống dòng', value: '\n' },
  { id: 'blank_line', label: 'Dòng trống', value: '\n\n' },
  { id: 'semicolon', label: 'Dấu chấm phẩy (;)', value: ';' },
  { id: 'custom', label: 'Tùy chỉnh', value: '' },
];

function splitCsvLine(line, delimiter = ',') {
  const result = [];
  let current = '';
  let inQuotes = false;

  for (let i = 0; i < line.length; i++) {
    const ch = line[i];
    const next = line[i + 1];

    if (ch === '"') {
      if (inQuotes && next === '"') {
        current += '"';
        i++;
      } else {
        inQuotes = !inQuotes;
      }
    } else if (!inQuotes && line.startsWith(delimiter, i)) {
      result.push(current);
      current = '';
      i += delimiter.length - 1;
    } else {
      current += ch;
    }
  }
  result.push(current);
  return result;
}

/**
 * Parse raw pasted text into `{ term, definition, hint }[]`.
 */
export function parseBulkCardsText(
  rawText,
  {
    termDelimiter = '\t',
    cardDelimiter = '\n',
    stripNumbering = false,
  } = {}
) {
  if (!rawText || !rawText.trim()) return [];

  const normalizedText = rawText.replace(/\r\n/g, '\n').replace(/\r/g, '\n');
  const effectiveCardDelim = cardDelimiter || '\n';
  const effectiveTermDelim = termDelimiter || '\t';

  const rawRecords =
    effectiveCardDelim === '\n\n'
      ? normalizedText.split(/\n\s*\n+/)
      : normalizedText.split(effectiveCardDelim);

  const cards = [];
  for (const rawRecord of rawRecords) {
    let record = rawRecord.trim();
    if (!record) continue;

    if (stripNumbering) {
      record = record.replace(/^\d+[.)]\s+/, '').replace(/^[-*•]\s+/, '');
    }

    let term = '';
    let definition = '';
    let hint = '';

    if (effectiveTermDelim === ',' || effectiveTermDelim === ';') {
      const parts = splitCsvLine(record, effectiveTermDelim);
      term = (parts[0] || '').trim();
      definition = parts.slice(1, 2).join('').trim() || parts.slice(1).join(effectiveTermDelim).trim();
      if (parts.length > 2 && effectiveTermDelim === ',' && parts[2]?.trim()) {
        // If 3+ CSV columns: 1=term, 2=definition, 3=hint
        definition = (parts[1] || '').trim();
        hint = (parts[2] || '').trim();
      }
    } else {
      const idx = record.indexOf(effectiveTermDelim);
      if (idx === -1) {
        // Fallback: if user selected ' - ' but line uses '-' or '\t'
        if (effectiveTermDelim === ' - ' && record.includes('-')) {
          const dashIdx = record.indexOf('-');
          term = record.slice(0, dashIdx).trim();
          definition = record.slice(dashIdx + 1).trim();
        } else {
          term = record;
          definition = '';
        }
      } else {
        term = record.slice(0, idx).trim();
        const rest = record.slice(idx + effectiveTermDelim.length);
        if (effectiveTermDelim === '\t' && rest.includes('\t')) {
          const tabParts = rest.split('\t');
          definition = (tabParts[0] || '').trim();
          hint = (tabParts[1] || '').trim();
        } else {
          definition = rest.trim();
        }
      }
    }

    if (term || definition) {
      cards.push({ term, definition, hint });
    }
  }

  return cards;
}

/**
 * Generate a unique client-side key for React rendering & drag-and-drop reordering.
 */
export function createDraftCard(partial = {}) {
  const clientKey =
    partial.clientKey ||
    `draft_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`;
  return {
    clientKey,
    id: partial.id ?? null,
    term: partial.term ?? '',
    definition: partial.definition ?? '',
    hint: partial.hint ?? '',
    imageUrl: partial.imageUrl ?? '',
    audioUrl: partial.audioUrl ?? '',
    conceptTag: partial.conceptTag ?? '',
  };
}

/**
 * Normalize `prefillData` coming from AI Chatbot (T4.3) or other flows into editor state.
 */
export function normalizePrefillData(prefillData) {
  if (!prefillData || typeof prefillData !== 'object') return null;

  const rawCards = Array.isArray(prefillData.cards) ? prefillData.cards : [];
  const cards = rawCards
    .filter((c) => c && (c.term || c.definition))
    .map((c) =>
      createDraftCard({
        term: String(c.term || ''),
        definition: String(c.definition || ''),
        hint: String(c.hint || ''),
        imageUrl: String(c.imageUrl || ''),
        conceptTag: String(c.conceptTag || ''),
      })
    );

  const tags = Array.isArray(prefillData.tags)
    ? prefillData.tags.map((t) => String(t).trim()).filter(Boolean)
    : [];

  return {
    title: String(prefillData.title || ''),
    description: String(prefillData.description || ''),
    categoryId: prefillData.categoryId ? String(prefillData.categoryId) : '',
    visibility: ['PUBLIC', 'LINK_ONLY', 'PRIVATE'].includes(prefillData.visibility)
      ? prefillData.visibility
      : 'PUBLIC',
    termLang: prefillData.termLang || 'en',
    defLang: prefillData.defLang || 'vi',
    tags,
    sourceType: prefillData.sourceType || 'AI_GENERATED',
    cards: cards.length > 0 ? cards : [createDraftCard(), createDraftCard()],
  };
}

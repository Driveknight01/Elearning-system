import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  Equal,
  Trash2,
  Image as ImageIcon,
  X,
  Plus,
  ArrowUp,
  ArrowDown,
  Sparkles,
} from 'lucide-react';
import InlineCardImagePicker from './InlineCardImagePicker';
import LanguagePickerPopover, { getLanguageLabel } from './LanguagePickerPopover';
import { findTypingSuggestions } from '../../data/wordSuggestions';

function autoResizeTextarea(el) {
  if (!el) return;
  el.style.height = 'auto';
  el.style.height = `${Math.max(48, el.scrollHeight)}px`;
}

export default function CardEditorRow({
  card,
  index,
  total,
  allCards = [],
  termLang,
  defLang,
  onChangeTermLang,
  onChangeDefLang,
  autoSuggestEnabled,
  isDragging,
  isDragOver,
  showValidationErrors,
  onChange,
  onApplySuggestion,
  onDelete,
  onMoveUp,
  onMoveDown,
  onInsertAfter,
  onDragStart,
  onDragOver,
  onDrop,
  onDragEnd,
}) {
  const [langMenu, setLangMenu] = useState(null); // 'term' | 'def' | null
  const [imagePickerSide, setImagePickerSide] = useState(null); // 'term' | 'definition' | null
  const [focusedField, setFocusedField] = useState(null); // 'term' | 'definition' | null

  // Insert button hover + auto-hide after click
  const [rowHovered, setRowHovered] = useState(false);
  const [insertSuppressed, setInsertSuppressed] = useState(false);
  const hideTimerRef = useRef(null);

  useEffect(() => {
    return () => {
      if (hideTimerRef.current) clearTimeout(hideTimerRef.current);
    };
  }, []);

  const termInvalid = showValidationErrors && !card.term.trim();
  const defInvalid = showValidationErrors && !card.definition.trim();

  const termLangLabel = getLanguageLabel(termLang) || 'Chọn ngôn ngữ';
  const defLangLabel = getLanguageLabel(defLang) || 'Chọn ngôn ngữ';

  const activeImageSide = card.imageSide === 'term' ? 'term' : 'definition';

  const termSuggestions = useMemo(() => {
    if (!autoSuggestEnabled || focusedField !== 'term') return [];
    return findTypingSuggestions(card.term, 'term', allCards, 5);
  }, [autoSuggestEnabled, focusedField, card.term, allCards]);

  const defSuggestions = useMemo(() => {
    if (!autoSuggestEnabled || focusedField !== 'definition') return [];
    return findTypingSuggestions(card.definition, 'definition', allCards, 5);
  }, [autoSuggestEnabled, focusedField, card.definition, allCards]);

  const handleInsertClick = (e) => {
    e.stopPropagation();
    onInsertAfter(index);
    if (hideTimerRef.current) clearTimeout(hideTimerRef.current);
    hideTimerRef.current = setTimeout(() => {
      setInsertSuppressed(true);
    }, 1500);
  };

  const renderImageSlot = (side) => {
    const hasImageOnThisSide = Boolean(card.imageUrl) && activeImageSide === side;
    const isPickerOpenHere = imagePickerSide === side;

    if (hasImageOnThisSide) {
      return (
        <div className="relative w-24 md:w-[82px] h-[66px] rounded-lg overflow-hidden border border-[#D9DDE8] dark:border-[#3D4870] bg-[#EAECEF] dark:bg-[#171836] shrink-0">
          <img
            src={card.imageUrl}
            alt={card.term || `Ảnh thẻ ${index + 1}`}
            onClick={() =>
              setImagePickerSide((prev) => (prev === side ? null : side))
            }
            className="w-full h-full object-cover cursor-pointer"
          />
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              onChange(index, 'imageUrl', '');
            }}
            title="Gỡ ảnh"
            aria-label="Gỡ ảnh thẻ"
            className="absolute top-1 right-1 w-5 h-5 rounded-full bg-black/65 hover:bg-rose-600 text-white flex items-center justify-center transition"
          >
            <X className="w-3 h-3" />
          </button>
        </div>
      );
    }

    return (
      <button
        type="button"
        onClick={() =>
          setImagePickerSide((prev) => (prev === side ? null : side))
        }
        className={`w-full md:w-[82px] h-[66px] rounded-lg flex flex-col items-center justify-center gap-1 transition shrink-0 ${
          isPickerOpenHere
            ? 'bg-[#D9DDE8]/70 dark:bg-[#2E3856] border border-[#939BB4] text-[#282E3E] dark:text-white'
            : 'border border-dashed border-[#D9DDE8] dark:border-[#3D4870] hover:border-[#4255FF] hover:bg-[#F6F7FB] dark:hover:bg-[#2E3856] text-[#282E3E] dark:text-[#E2E8F0]'
        }`}
      >
        <ImageIcon className="w-4 h-4 text-[#586380] dark:text-[#A8B1D1]" />
        <span className="text-[11px] font-bold">Hình ảnh</span>
      </button>
    );
  };

  const showTermLangBtn = focusedField === 'term' || langMenu === 'term';
  const showDefLangBtn = focusedField === 'definition' || langMenu === 'def';
  const showInsertButton = rowHovered && !insertSuppressed;

  return (
    <div
      className="group/row relative"
      onMouseEnter={() => {
        if (hideTimerRef.current) clearTimeout(hideTimerRef.current);
        setRowHovered(true);
        setInsertSuppressed(false);
      }}
      onMouseLeave={() => {
        setRowHovered(false);
      }}
    >
      <div
        draggable
        onDragStart={(e) => onDragStart(e, index)}
        onDragOver={(e) => onDragOver(e, index)}
        onDrop={(e) => onDrop(e, index)}
        onDragEnd={onDragEnd}
        className={`rounded-2xl bg-white dark:bg-[#202140] border transition-all shadow-[0_4px_16px_rgba(40,46,62,0.08)] hover:shadow-[0_6px_20px_rgba(40,46,62,0.11)] ${
          isDragging
            ? 'opacity-45 scale-[0.99] border-[#4255FF]'
            : isDragOver
              ? 'border-[#4255FF] ring-2 ring-[#4255FF]/25'
              : termInvalid || defInvalid
                ? 'border-rose-400 dark:border-rose-500'
                : 'border-[#D9DDE8] dark:border-[#2E3856]'
        }`}
      >
        {/* Top Row: Number (left) + Drag & Delete (right) */}
        <div className="px-5 pt-4 pb-2 flex items-center justify-between gap-3">
          <span className="text-[14px] font-bold text-[#282E3E] dark:text-[#E2E8F0]">
            {index + 1}
          </span>

          <div className="flex items-center gap-1">
            <div className="hidden sm:flex items-center gap-0.5 opacity-0 group-hover/row:opacity-100 transition-opacity mr-1">
              <button
                type="button"
                disabled={index === 0}
                onClick={(e) => {
                  e.stopPropagation();
                  onMoveUp(index);
                }}
                title="Di chuyển lên"
                aria-label={`Di chuyển thẻ ${index + 1} lên`}
                className="p-1 rounded-md text-[#939BB4] hover:text-[#282E3E] hover:bg-[#F6F7FB] dark:hover:bg-[#2E3856] disabled:opacity-25 transition"
              >
                <ArrowUp className="w-3.5 h-3.5" />
              </button>
              <button
                type="button"
                disabled={index === total - 1}
                onClick={(e) => {
                  e.stopPropagation();
                  onMoveDown(index);
                }}
                title="Di chuyển xuống"
                aria-label={`Di chuyển thẻ ${index + 1} xuống`}
                className="p-1 rounded-md text-[#939BB4] hover:text-[#282E3E] hover:bg-[#F6F7FB] dark:hover:bg-[#2E3856] disabled:opacity-25 transition"
              >
                <ArrowDown className="w-3.5 h-3.5" />
              </button>
            </div>

            <span
              title="Kéo thả để đổi thứ tự thẻ"
              className="p-1.5 rounded-lg text-[#586380] dark:text-[#A8B1D1] hover:text-[#282E3E] hover:bg-[#F6F7FB] dark:hover:bg-[#2E3856] cursor-grab active:cursor-grabbing transition"
            >
              <Equal className="w-4 h-4" />
            </span>

            <button
              type="button"
              disabled={total <= 1}
              onClick={(e) => {
                e.stopPropagation();
                onDelete(index);
              }}
              title={total <= 1 ? 'Bộ thẻ cần ít nhất 1 thẻ' : 'Xóa thẻ này'}
              aria-label={`Xóa thẻ ${index + 1}`}
              className="p-1.5 rounded-lg text-[#939BB4] hover:text-rose-600 hover:bg-rose-50 dark:hover:text-rose-400 dark:hover:bg-rose-900/20 disabled:opacity-30 disabled:cursor-not-allowed transition"
            >
              <Trash2 className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Main Body: Image (Term) + Term Input + Definition Input + Image (Definition) */}
        <div className="px-5 pb-5 pt-1.5 flex flex-col md:flex-row items-stretch md:items-start gap-4">
          {/* Left Image Slot (Term side) */}
          {renderImageSlot('term')}

          {/* Term Input + Label + Autocomplete */}
          <div className="flex-1 min-w-0 relative">
            <textarea
              rows={1}
              value={card.term}
              onFocus={() => setFocusedField('term')}
              onBlur={() =>
                setTimeout(() => setFocusedField((f) => (f === 'term' ? null : f)), 180)
              }
              onInput={(e) => autoResizeTextarea(e.currentTarget)}
              onChange={(e) => onChange(index, 'term', e.target.value)}
              className={`w-full px-3.5 py-3 rounded-lg bg-[#F6F7FB] dark:bg-[#171836] border-2 text-[15px] font-medium text-[#282E3E] dark:text-white outline-none transition resize-none overflow-hidden min-h-[48px] leading-snug ${
                termInvalid
                  ? 'border-rose-500'
                  : 'border-transparent focus:border-[#4255FF] focus:bg-white dark:focus:bg-[#171836]'
              }`}
            />

            {/* Typing Autocomplete Dropdown for Term */}
            {termSuggestions.length > 0 && (
              <div className="absolute left-0 right-0 top-[52px] mt-1 rounded-xl bg-white dark:bg-[#171836] border border-[#EDEFF4] dark:border-[#2E3856] shadow-xl py-1.5 z-40">
                <div className="px-3 py-1 flex items-center gap-1.5 text-[10px] font-extrabold uppercase tracking-wider text-[#4255FF] dark:text-[#8E9BFF]">
                  <Sparkles className="w-3 h-3" />
                  <span>Gợi ý từ vựng</span>
                </div>
                {termSuggestions.map((s) => (
                  <button
                    key={`${s.term}-${s.definition}`}
                    type="button"
                    onMouseDown={(e) => {
                      e.preventDefault();
                      onApplySuggestion(index, s, 'term');
                      setFocusedField(null);
                    }}
                    className="w-full text-left px-3.5 py-2 hover:bg-[#F6F7FB] dark:hover:bg-[#202140] flex items-center justify-between gap-3 transition"
                  >
                    <span className="text-sm font-bold text-[#282E3E] dark:text-white truncate">
                      {s.term}
                    </span>
                    <span className="text-xs font-medium text-[#586380] dark:text-[#A8B1D1] truncate">
                      {s.definition}
                    </span>
                  </button>
                ))}
              </div>
            )}

            <div className="mt-1.5 flex items-center justify-between gap-2 min-h-[18px]">
              <div className="flex items-center gap-2">
                <span className="text-[11px] font-bold uppercase tracking-wider text-[#586380] dark:text-[#A8B1D1]">
                  Thuật ngữ
                </span>
                {termInvalid && (
                  <span className="text-[11px] font-bold text-rose-500">
                    Bắt buộc
                  </span>
                )}
              </div>

              {showTermLangBtn && (
                <button
                  type="button"
                  onMouseDown={(e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    setLangMenu((prev) => (prev === 'term' ? null : 'term'));
                  }}
                  className="text-[11px] font-extrabold uppercase tracking-wider text-[#4255FF] dark:text-[#8E9BFF] hover:underline"
                >
                  {termLangLabel}
                </button>
              )}
            </div>

            {langMenu === 'term' && (
              <LanguagePickerPopover
                selectedCode={termLang}
                onSelect={(code) => onChangeTermLang?.(code)}
                onClose={() => setLangMenu(null)}
              />
            )}
          </div>

          {/* Definition Input + Label + Autocomplete */}
          <div className="flex-1 min-w-0 relative">
            <textarea
              rows={1}
              value={card.definition}
              onFocus={() => setFocusedField('definition')}
              onBlur={() =>
                setTimeout(() => setFocusedField((f) => (f === 'definition' ? null : f)), 180)
              }
              onInput={(e) => autoResizeTextarea(e.currentTarget)}
              onChange={(e) => onChange(index, 'definition', e.target.value)}
              className={`w-full px-3.5 py-3 rounded-lg bg-[#F6F7FB] dark:bg-[#171836] border-2 text-[15px] font-medium text-[#282E3E] dark:text-white outline-none transition resize-none overflow-hidden min-h-[48px] leading-snug ${
                defInvalid
                  ? 'border-rose-500'
                  : 'border-transparent focus:border-[#4255FF] focus:bg-white dark:focus:bg-[#171836]'
              }`}
            />

            {/* Typing Autocomplete Dropdown for Definition */}
            {defSuggestions.length > 0 && (
              <div className="absolute left-0 right-0 top-[52px] mt-1 rounded-xl bg-white dark:bg-[#171836] border border-[#EDEFF4] dark:border-[#2E3856] shadow-xl py-1.5 z-40">
                <div className="px-3 py-1 flex items-center gap-1.5 text-[10px] font-extrabold uppercase tracking-wider text-[#4255FF] dark:text-[#8E9BFF]">
                  <Sparkles className="w-3 h-3" />
                  <span>Gợi ý định nghĩa</span>
                </div>
                {defSuggestions.map((s) => (
                  <button
                    key={`${s.term}-${s.definition}`}
                    type="button"
                    onMouseDown={(e) => {
                      e.preventDefault();
                      onApplySuggestion(index, s, 'definition');
                      setFocusedField(null);
                    }}
                    className="w-full text-left px-3.5 py-2 hover:bg-[#F6F7FB] dark:hover:bg-[#202140] flex items-center justify-between gap-3 transition"
                  >
                    <span className="text-sm font-bold text-[#282E3E] dark:text-white truncate">
                      {s.definition}
                    </span>
                    <span className="text-xs font-medium text-[#586380] dark:text-[#A8B1D1] truncate">
                      {s.term}
                    </span>
                  </button>
                ))}
              </div>
            )}

            <div className="mt-1.5 flex items-center justify-between gap-2 min-h-[18px]">
              <div className="flex items-center gap-2">
                <span className="text-[11px] font-bold uppercase tracking-wider text-[#586380] dark:text-[#A8B1D1]">
                  Định nghĩa
                </span>
                {defInvalid && (
                  <span className="text-[11px] font-bold text-rose-500">
                    Bắt buộc
                  </span>
                )}
              </div>

              {showDefLangBtn && (
                <button
                  type="button"
                  onMouseDown={(e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    setLangMenu((prev) => (prev === 'def' ? null : 'def'));
                  }}
                  className="text-[11px] font-extrabold uppercase tracking-wider text-[#4255FF] dark:text-[#8E9BFF] hover:underline"
                >
                  {defLangLabel}
                </button>
              )}
            </div>

            {langMenu === 'def' && (
              <LanguagePickerPopover
                selectedCode={defLang}
                onSelect={(code) => onChangeDefLang?.(code)}
                onClose={() => setLangMenu(null)}
              />
            )}
          </div>

          {/* Right Image Slot (Definition side) */}
          {renderImageSlot('definition')}
        </div>

        {/* Inline Quizlet-style Image Picker Drawer */}
        {imagePickerSide && (
          <InlineCardImagePicker
            side={imagePickerSide}
            card={card}
            onSelectImage={(url, chosenSide) => {
              onChange(index, 'imageUrl', url);
              onChange(index, 'imageSide', chosenSide);
            }}
            onClose={() => setImagePickerSide(null)}
          />
        )}
      </div>

      {/* Circular Blue '+' Insert Card Button — auto-hides ~1.5s after click, reappears on card hover */}
      <div
        className={`absolute left-1/2 -translate-x-1/2 -bottom-[18px] z-20 transition-opacity duration-200 ${
          showInsertButton
            ? 'opacity-100 pointer-events-auto'
            : 'opacity-0 pointer-events-none'
        }`}
      >
        <button
          type="button"
          onClick={handleInsertClick}
          title="Chèn thẻ mới ngay dưới"
          aria-label="Chèn thẻ mới"
          className="w-9 h-9 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white shadow-md flex items-center justify-center transition-transform hover:scale-105"
        >
          <Plus className="w-5 h-5 stroke-[2.5]" />
        </button>
      </div>
    </div>
  );
}

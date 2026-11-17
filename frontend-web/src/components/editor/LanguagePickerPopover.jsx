import React, { useEffect, useMemo, useRef, useState } from 'react';

export const USER_LANGUAGES = [
  { code: 'en', label: 'Tiếng Anh' },
  { code: 'vi', label: 'Tiếng Việt' },
  { code: 'es', label: 'Tiếng Tây Ban Nha' },
];

export const POPULAR_LANGUAGES = [
  { code: 'ja', label: 'Tiếng Nhật' },
  { code: 'ko', label: 'Tiếng Hàn' },
  { code: 'zh', label: 'Tiếng Trung' },
  { code: 'fr', label: 'Tiếng Pháp' },
  { code: 'de', label: 'Tiếng Đức' },
  { code: 'chem', label: 'Ký hiệu hóa học' },
  { code: 'math', label: 'Ký hiệu toán học' },
  { code: 'other', label: 'Khác' },
];

export const ALL_CARD_LANGUAGES = [...USER_LANGUAGES, ...POPULAR_LANGUAGES];

export function getLanguageLabel(code) {
  return ALL_CARD_LANGUAGES.find((l) => l.code === code)?.label || '';
}

export default function LanguagePickerPopover({
  selectedCode,
  onSelect,
  onClose,
}) {
  const [search, setSearch] = useState('');
  const containerRef = useRef(null);

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        onClose?.();
      }
    };
    const handleEsc = (e) => {
      if (e.key === 'Escape') onClose?.();
    };
    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleEsc);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleEsc);
    };
  }, [onClose]);

  const q = search.trim().toLowerCase();

  const filteredUserLangs = useMemo(
    () =>
      USER_LANGUAGES.filter(
        (l) => !q || l.label.toLowerCase().includes(q) || l.code.includes(q)
      ),
    [q]
  );

  const filteredPopularLangs = useMemo(
    () =>
      POPULAR_LANGUAGES.filter(
        (l) => !q || l.label.toLowerCase().includes(q) || l.code.includes(q)
      ),
    [q]
  );

  return (
    <div
      ref={containerRef}
      onClick={(e) => e.stopPropagation()}
      onMouseDown={(e) => e.stopPropagation()}
      className="absolute right-0 mt-1.5 w-72 rounded-2xl bg-white dark:bg-[#171836] border border-[#EDEFF4] dark:border-[#2E3856] shadow-[0_12px_32px_rgba(40,46,62,0.16)] overflow-hidden z-40"
    >
      {/* Search Input */}
      <div className="p-3 pb-2">
        <input
          type="text"
          autoFocus
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Tìm ngôn ngữ"
          className="w-full h-10 px-3.5 rounded-xl bg-[#F6F7FB] dark:bg-[#202140] text-[14px] font-medium text-[#282E3E] dark:text-white placeholder:text-[#939BB4] outline-none border border-transparent focus:border-[#4255FF]"
        />
      </div>

      {/* Scrollable Language Groups */}
      <div className="max-h-72 overflow-y-auto pb-2">
        {filteredUserLangs.length > 0 && (
          <div className="pt-1 pb-2">
            <p className="px-4 py-1.5 text-[13px] font-extrabold text-[#282E3E] dark:text-white">
              Ngôn ngữ của bạn
            </p>
            {filteredUserLangs.map((lang) => (
              <button
                key={lang.code}
                type="button"
                onClick={() => {
                  onSelect?.(lang.code);
                  onClose?.();
                }}
                className={`w-full text-left px-4 py-2 text-[14px] font-bold transition ${
                  selectedCode === lang.code
                    ? 'bg-[#EDEFF4] text-[#282E3E] dark:bg-[#2E3856] dark:text-white'
                    : 'text-[#586380] dark:text-[#A8B1D1] hover:bg-[#F6F7FB] dark:hover:bg-[#202140]'
                }`}
              >
                {lang.label}
              </button>
            ))}
          </div>
        )}

        {filteredUserLangs.length > 0 && filteredPopularLangs.length > 0 && (
          <div className="mx-3 my-1 border-t border-[#EDEFF4] dark:border-[#2E3856]" />
        )}

        {filteredPopularLangs.length > 0 && (
          <div className="pt-1">
            <p className="px-4 py-1.5 text-[13px] font-extrabold text-[#282E3E] dark:text-white">
              Các ngôn ngữ phổ biến nhất
            </p>
            {filteredPopularLangs.map((lang) => (
              <button
                key={lang.code}
                type="button"
                onClick={() => {
                  onSelect?.(lang.code);
                  onClose?.();
                }}
                className={`w-full text-left px-4 py-2 text-[14px] font-bold transition ${
                  selectedCode === lang.code
                    ? 'bg-[#EDEFF4] text-[#282E3E] dark:bg-[#2E3856] dark:text-white'
                    : 'text-[#586380] dark:text-[#A8B1D1] hover:bg-[#F6F7FB] dark:hover:bg-[#202140]'
                }`}
              >
                {lang.label}
              </button>
            ))}
          </div>
        )}

        {filteredUserLangs.length === 0 && filteredPopularLangs.length === 0 && (
          <p className="px-4 py-4 text-center text-xs font-semibold text-[#939BB4]">
            Không tìm thấy ngôn ngữ phù hợp.
          </p>
        )}
      </div>
    </div>
  );
}

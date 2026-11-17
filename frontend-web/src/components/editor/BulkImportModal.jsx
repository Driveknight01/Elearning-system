import React, { useEffect, useMemo, useState } from 'react';
import { X } from 'lucide-react';
import { parseBulkCardsText } from '../../utils/bulkCardParser';

export default function BulkImportModal({ open, onClose, onImportCards }) {
  const [rawText, setRawText] = useState('');
  const [termMode, setTermMode] = useState('tab'); // 'tab' | 'comma' | 'custom'
  const [customTermDelim, setCustomTermDelim] = useState('');
  const [cardMode, setCardMode] = useState('newline'); // 'newline' | 'semicolon' | 'custom'
  const [customCardDelim, setCustomCardDelim] = useState('');
  const [replaceExisting, setReplaceExisting] = useState(false);

  useEffect(() => {
    if (!open) return;
    const handleEsc = (e) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleEsc);
    return () => window.removeEventListener('keydown', handleEsc);
  }, [open, onClose]);

  const termDelimiter = useMemo(() => {
    if (termMode === 'tab') return '\t';
    if (termMode === 'comma') return ',';
    return customTermDelim || '\t';
  }, [termMode, customTermDelim]);

  const cardDelimiter = useMemo(() => {
    if (cardMode === 'newline') return '\n';
    if (cardMode === 'semicolon') return ';';
    if (customCardDelim === '\\n\\n') return '\n\n';
    return customCardDelim || '\n';
  }, [cardMode, customCardDelim]);

  const parsedCards = useMemo(
    () =>
      parseBulkCardsText(rawText, {
        termDelimiter,
        cardDelimiter,
        stripNumbering: false,
      }),
    [rawText, termDelimiter, cardDelimiter]
  );

  if (!open) return null;

  const handleConfirm = () => {
    if (parsedCards.length === 0) return;
    onImportCards(parsedCards, { replaceExisting });
    setRawText('');
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 bg-white dark:bg-[#0A082D] flex flex-col overflow-hidden">
      {/* Main Scrollable Content */}
      <div className="flex-1 overflow-y-auto px-5 sm:px-10 pt-5 pb-24">
        <div className="max-w-[1220px] mx-auto space-y-6">
          {/* Top Header */}
          <div className="flex items-start justify-between gap-4">
            <p className="text-[15px] text-[#282E3E] dark:text-[#F6F7FB] pt-1.5">
              <span className="font-extrabold">Nhập dữ liệu.</span>{' '}
              <span className="font-normal text-[#586380] dark:text-[#A8B1D1]">
                Chép và dán dữ liệu ở đây (từ Word, Excel, Google Docs, v.v.)
              </span>
            </p>

            <button
              type="button"
              onClick={onClose}
              aria-label="Đóng"
              className="w-10 h-10 rounded-full bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#202140] dark:hover:bg-[#2E3856] text-[#586380] dark:text-[#A8B1D1] flex items-center justify-center shrink-0 transition"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Large Paste Textarea */}
          <div>
            <textarea
              autoFocus
              rows={8}
              value={rawText}
              onChange={(e) => setRawText(e.target.value)}
              placeholder={`Từ 1\tĐịnh nghĩa 1\nTừ 2\tĐịnh nghĩa 2\nTừ 3\tĐịnh nghĩa 3`}
              className="w-full min-h-[210px] rounded-md bg-white dark:bg-[#171836] border-2 border-[#2E3856] dark:border-[#4255FF] p-4 text-[15px] leading-relaxed text-[#282E3E] dark:text-white placeholder:text-[#939BB4] outline-none resize-y"
            />
          </div>

          {/* Delimiters (2-column radio layout matching Quizlet) */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-12 gap-6 items-start">
            {/* Left column: Between term & definition */}
            <div className="lg:col-span-3 space-y-2.5">
              <p className="text-[14px] font-extrabold text-[#282E3E] dark:text-white">
                Giữa thuật ngữ và định nghĩa
              </p>

              <label className="flex items-center gap-2.5 text-[14px] font-semibold text-[#282E3E] dark:text-[#E2E8F0] cursor-pointer">
                <input
                  type="radio"
                  name="termDelimiterMode"
                  checked={termMode === 'tab'}
                  onChange={() => setTermMode('tab')}
                  className="w-4 h-4 accent-[#2E3856] dark:accent-[#4255FF]"
                />
                <span>Tab</span>
              </label>

              <label className="flex items-center gap-2.5 text-[14px] font-semibold text-[#282E3E] dark:text-[#E2E8F0] cursor-pointer">
                <input
                  type="radio"
                  name="termDelimiterMode"
                  checked={termMode === 'comma'}
                  onChange={() => setTermMode('comma')}
                  className="w-4 h-4 accent-[#2E3856] dark:accent-[#4255FF]"
                />
                <span>Phẩy</span>
              </label>

              <div className="flex items-center gap-2.5">
                <input
                  type="radio"
                  name="termDelimiterMode"
                  aria-label="Tùy chỉnh giữa thuật ngữ và định nghĩa"
                  checked={termMode === 'custom'}
                  onChange={() => setTermMode('custom')}
                  className="w-4 h-4 accent-[#2E3856] dark:accent-[#4255FF] shrink-0"
                />
                <input
                  type="text"
                  value={customTermDelim}
                  onFocus={() => setTermMode('custom')}
                  onChange={(e) => {
                    setTermMode('custom');
                    setCustomTermDelim(e.target.value);
                  }}
                  placeholder="Tùy chỉnh"
                  className="w-48 h-10 px-3.5 rounded-lg bg-[#F6F7FB] dark:bg-[#202140] border border-transparent focus:border-[#4255FF] text-sm font-semibold text-[#282E3E] dark:text-white placeholder:text-[#586380] dark:placeholder:text-[#A8B1D1] outline-none transition"
                />
              </div>
            </div>

            {/* Right column: Between cards */}
            <div className="lg:col-span-4 space-y-2.5">
              <p className="text-[14px] font-extrabold text-[#282E3E] dark:text-white">
                Giữa các thẻ
              </p>

              <label className="flex items-center gap-2.5 text-[14px] font-semibold text-[#282E3E] dark:text-[#E2E8F0] cursor-pointer">
                <input
                  type="radio"
                  name="cardDelimiterMode"
                  checked={cardMode === 'newline'}
                  onChange={() => setCardMode('newline')}
                  className="w-4 h-4 accent-[#2E3856] dark:accent-[#4255FF]"
                />
                <span>Dòng mới</span>
              </label>

              <label className="flex items-center gap-2.5 text-[14px] font-semibold text-[#282E3E] dark:text-[#E2E8F0] cursor-pointer">
                <input
                  type="radio"
                  name="cardDelimiterMode"
                  checked={cardMode === 'semicolon'}
                  onChange={() => setCardMode('semicolon')}
                  className="w-4 h-4 accent-[#2E3856] dark:accent-[#4255FF]"
                />
                <span>Chấm phẩy</span>
              </label>

              <div className="flex items-center gap-2.5">
                <input
                  type="radio"
                  name="cardDelimiterMode"
                  aria-label="Tùy chỉnh giữa các thẻ"
                  checked={cardMode === 'custom'}
                  onChange={() => setCardMode('custom')}
                  className="w-4 h-4 accent-[#2E3856] dark:accent-[#4255FF] shrink-0"
                />
                <input
                  type="text"
                  value={customCardDelim}
                  onFocus={() => setCardMode('custom')}
                  onChange={(e) => {
                    setCardMode('custom');
                    setCustomCardDelim(e.target.value);
                  }}
                  placeholder="Tùy chỉnh"
                  className="w-48 h-10 px-3.5 rounded-lg bg-[#F6F7FB] dark:bg-[#202140] border border-transparent focus:border-[#4255FF] text-sm font-semibold text-[#282E3E] dark:text-white placeholder:text-[#586380] dark:placeholder:text-[#A8B1D1] outline-none transition"
                />
              </div>
            </div>

            {/* Optional replace toggle */}
            <div className="lg:col-span-5 lg:pt-7">
              <label className="inline-flex items-center gap-2 text-xs font-semibold text-[#586380] dark:text-[#A8B1D1] cursor-pointer">
                <input
                  type="checkbox"
                  checked={replaceExisting}
                  onChange={(e) => setReplaceExisting(e.target.checked)}
                  className="w-4 h-4 rounded accent-[#4255FF]"
                />
                Thay thế toàn bộ thẻ đang có trong trình soạn thảo
              </label>
            </div>
          </div>

          {/* Preview Section (matching Quizlet's card preview list) */}
          <div className="space-y-3">
            <div className="flex items-baseline gap-2.5">
              <h3 className="text-[16px] font-extrabold text-[#282E3E] dark:text-white">
                Xem trước
              </h3>
              <span className="text-[13px] font-semibold text-[#586380] dark:text-[#A8B1D1]">
                {parsedCards.length} thẻ
              </span>
            </div>

            {parsedCards.length === 0 ? (
              <div className="h-14 rounded-xl bg-gradient-to-r from-[#F6F7FB] via-[#EDEFF4]/80 to-[#F6F7FB] dark:from-[#202140] dark:via-[#2E3856]/60 dark:to-[#202140]" />
            ) : (
              <div className="rounded-xl bg-[#F6F7FB] dark:bg-[#171836] p-3 max-h-[300px] overflow-y-auto space-y-2.5">
                {parsedCards.slice(0, 100).map((item, idx) => (
                  <div
                    key={idx}
                    className="rounded-xl bg-white dark:bg-[#202140] px-4 py-3.5 flex items-start gap-4 shadow-[0_1px_3px_rgba(40,46,62,0.04)]"
                  >
                    <span className="w-6 pt-2 text-[13px] font-bold text-[#586380] dark:text-[#A8B1D1] shrink-0">
                      {idx + 1}
                    </span>

                    <div className="flex-1 grid grid-cols-1 sm:grid-cols-2 gap-4">
                      <div>
                        <div className="min-h-[38px] px-3.5 py-2 rounded-lg bg-[#F6F7FB] dark:bg-[#171836] text-[14px] font-medium text-[#586380] dark:text-[#E2E8F0] break-words">
                          {item.term || <span className="italic text-[#939BB4]">Trống</span>}
                        </div>
                        <p className="mt-1.5 text-[10px] font-bold uppercase tracking-wider text-[#939BB4]">
                          Thuật ngữ
                        </p>
                      </div>

                      <div>
                        <div className="min-h-[38px] px-3.5 py-2 rounded-lg bg-[#F6F7FB] dark:bg-[#171836] text-[14px] font-medium text-[#586380] dark:text-[#E2E8F0] break-words">
                          {item.definition || <span className="italic text-[#939BB4]">Trống</span>}
                        </div>
                        <p className="mt-1.5 text-[10px] font-bold uppercase tracking-wider text-[#939BB4]">
                          Định nghĩa
                        </p>
                      </div>
                    </div>
                  </div>
                ))}
                {parsedCards.length > 100 && (
                  <p className="text-center py-2 text-xs font-semibold text-[#586380] dark:text-[#A8B1D1]">
                    ...và {parsedCards.length - 100} thẻ khác sẽ được nhập.
                  </p>
                )}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Bottom Sticky Footer Bar */}
      <div className="sticky bottom-0 z-10 bg-white dark:bg-[#0A082D] border-t border-[#EDEFF4] dark:border-[#2E3856] px-5 sm:px-10 py-3.5">
        <div className="max-w-[1220px] mx-auto flex items-center justify-end gap-3">
          <button
            type="button"
            onClick={onClose}
            className="h-10 px-5 rounded-full bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#202140] dark:hover:bg-[#2E3856] text-sm font-bold text-[#586380] dark:text-[#A8B1D1] transition"
          >
            Hủy nhập
          </button>
          <button
            type="button"
            disabled={parsedCards.length === 0}
            onClick={handleConfirm}
            className="h-10 px-6 rounded-full bg-[#4255FF] hover:bg-[#3545E0] disabled:bg-[#F6F7FB] disabled:text-[#939BB4] dark:disabled:bg-[#202140] dark:disabled:text-[#586380] text-white text-sm font-bold transition"
          >
            Nhập
          </button>
        </div>
      </div>
    </div>
  );
}

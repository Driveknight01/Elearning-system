import React, { useEffect } from 'react';
import { RotateCcw, Volume2, X } from 'lucide-react';

/**
 * Flashcard deck options modal ("Tùy chọn").
 */
export default function DeckOptionsModal({
  open,
  autoSpeak,
  onToggleAutoSpeak,
  startWithDef,
  onToggleStartWithDef,
  trackProgress,
  onToggleTrackProgress,
  onResetDeck,
  onClose,
}) {
  useEffect(() => {
    if (!open) return undefined;
    const onEsc = (e) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onEsc);
    return () => window.removeEventListener('keydown', onEsc);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 bg-black/45 backdrop-blur-[1px] flex items-center justify-center p-4"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="w-full max-w-[460px] bg-white dark:bg-[#202140] rounded-3xl border border-[#EDEFF4] dark:border-[#2E3856] shadow-2xl overflow-hidden">
        <div className="px-6 pt-5 pb-4 flex items-center justify-between border-b border-[#EDEFF4] dark:border-[#2E3856]">
          <h2 className="text-[20px] font-extrabold text-[#282E3E] dark:text-white">
            Tùy chọn
          </h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Đóng"
            className="w-9 h-9 rounded-full bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#2E3856] text-[#586380] dark:text-[#A8B1D1] flex items-center justify-center transition"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="p-6 space-y-4">
          {/* Auto-TTS */}
          <div className="flex items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <Volume2 className="w-4.5 h-4.5 text-[#4255FF]" />
              <div>
                <p className="text-sm font-bold text-[#282E3E] dark:text-white">
                  Tự động phát âm (TTS)
                </p>
                <p className="text-xs text-[#586380] dark:text-[#A8B1D1]">
                  Đọc to văn bản khi chuyển hoặc lật thẻ
                </p>
              </div>
            </div>
            <button
              type="button"
              onClick={onToggleAutoSpeak}
              className={`w-11 h-6 rounded-full p-0.5 transition-colors ${
                autoSpeak ? 'bg-[#4255FF]' : 'bg-[#D9DDE8] dark:bg-[#2E3856]'
              }`}
            >
              <span
                className={`block w-5 h-5 rounded-full bg-white shadow transition-transform ${
                  autoSpeak ? 'translate-x-5' : 'translate-x-0'
                }`}
              />
            </button>
          </div>

          {/* Answer side */}
          <div className="flex items-center justify-between gap-4">
            <div>
              <p className="text-sm font-bold text-[#282E3E] dark:text-white">
                Mặt trước là Định nghĩa
              </p>
              <p className="text-xs text-[#586380] dark:text-[#A8B1D1]">
                Hiển thị định nghĩa trước, lật để xem thuật ngữ
              </p>
            </div>
            <button
              type="button"
              onClick={onToggleStartWithDef}
              className={`w-11 h-6 rounded-full p-0.5 transition-colors ${
                startWithDef ? 'bg-[#4255FF]' : 'bg-[#D9DDE8] dark:bg-[#2E3856]'
              }`}
            >
              <span
                className={`block w-5 h-5 rounded-full bg-white shadow transition-transform ${
                  startWithDef ? 'translate-x-5' : 'translate-x-0'
                }`}
              />
            </button>
          </div>

          {/* Track progress */}
          {typeof trackProgress === 'boolean' && onToggleTrackProgress && (
            <div className="flex items-center justify-between gap-4">
              <div>
                <p className="text-sm font-bold text-[#282E3E] dark:text-white">
                  Theo dõi tiến độ
                </p>
                <p className="text-xs text-[#586380] dark:text-[#A8B1D1]">
                  Phân loại thẻ Đang học &amp; Đã biết
                </p>
              </div>
              <button
                type="button"
                onClick={onToggleTrackProgress}
                className={`w-11 h-6 rounded-full p-0.5 transition-colors ${
                  trackProgress ? 'bg-[#4255FF]' : 'bg-[#D9DDE8] dark:bg-[#2E3856]'
                }`}
              >
                <span
                  className={`block w-5 h-5 rounded-full bg-white shadow transition-transform ${
                    trackProgress ? 'translate-x-5' : 'translate-x-0'
                  }`}
                />
              </button>
            </div>
          )}

          <div className="pt-2 border-t border-[#EDEFF4] dark:border-[#2E3856]">
            <button
              type="button"
              onClick={() => {
                onResetDeck?.();
                onClose();
              }}
              className="w-full h-10 rounded-full bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#171936] dark:hover:bg-[#2E3856] text-sm font-bold text-[#282E3E] dark:text-white inline-flex items-center justify-center gap-2 transition"
            >
              <RotateCcw className="w-4 h-4" />
              Khởi động lại thẻ ghi nhớ
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

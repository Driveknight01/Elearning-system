import React, { useEffect, useState } from 'react';
import { ArrowRight, CalendarClock, Compass, Layers, X } from 'lucide-react';

const SORT_MODES = [
  {
    key: 'browse',
    title: 'Duyệt tìm',
    description: 'Duyệt thẻ theo cách thông thường. Phù hợp để làm quen với các thuật ngữ.',
    icon: Compass,
  },
  {
    key: 'basic',
    title: 'Phân loại cơ bản',
    description:
      'Phân loại thẻ ghi nhớ theo cách truyền thống. Hiệu quả để học các thuật ngữ mới nhanh chóng.',
    icon: Layers,
  },
  {
    key: 'spaced',
    title: 'Phân loại với lặp lại ngắt quãng',
    isNew: true,
    description:
      'Phân loại theo khoa học. Tối ưu hóa cho việc học lâu dài và hiệu quả về mặt thời gian.',
    icon: CalendarClock,
  },
];

/**
 * Quizlet-style "Chọn cách phân loại thẻ" modal.
 */
export default function SortFlashcardsModal({
  open,
  currentMode = 'browse',
  onClose,
  onStart,
}) {
  const [selected, setSelected] = useState(currentMode);

  useEffect(() => {
    if (open) setSelected(currentMode);
  }, [open, currentMode]);

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
      <div className="w-full max-w-[660px] bg-white dark:bg-[#202140] rounded-3xl border border-[#EDEFF4] dark:border-[#2E3856] shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="px-7 pt-6 pb-4 flex items-center justify-between gap-4">
          <h2 className="text-[24px] font-extrabold text-[#282E3E] dark:text-white tracking-tight">
            Chọn cách phân loại thẻ
          </h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Đóng"
            className="w-9 h-9 rounded-full bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#2E3856] dark:hover:bg-[#3A466B] text-[#586380] dark:text-[#A8B1D1] flex items-center justify-center transition"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Options */}
        <div className="px-7 pb-5 space-y-3">
          {SORT_MODES.map((mode) => {
            const Icon = mode.icon;
            const active = selected === mode.key;
            return (
              <button
                key={mode.key}
                type="button"
                onClick={() => setSelected(mode.key)}
                className={`w-full p-4 rounded-2xl border-2 text-left flex items-start gap-4 transition ${
                  active
                    ? 'bg-[#EDEFFF]/70 border-[#4255FF] dark:bg-[#2E3856] dark:border-[#8E9BFF]'
                    : 'bg-[#F6F7FB] border-transparent hover:bg-[#EDEFF4] dark:bg-[#171936] dark:hover:bg-[#2E3856]/60'
                }`}
              >
                <div className="w-9 h-9 rounded-xl bg-white dark:bg-[#202140] flex items-center justify-center text-[#586380] dark:text-[#A8B1D1] shrink-0 mt-0.5">
                  <Icon className="w-4.5 h-4.5" />
                </div>

                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <span className="text-[15px] font-extrabold text-[#282E3E] dark:text-white">
                      {mode.title}
                    </span>
                    {mode.isNew && (
                      <span className="px-2 py-0.5 rounded-full text-[11px] font-extrabold bg-[#98F5FF] text-[#0D2B45]">
                        Mới
                      </span>
                    )}
                  </div>
                  <p className="mt-0.5 text-[13px] text-[#586380] dark:text-[#A8B1D1] leading-snug">
                    {mode.description}
                  </p>
                </div>
              </button>
            );
          })}
        </div>

        {/* Footer */}
        <div className="px-7 py-4 border-t border-[#EDEFF4] dark:border-[#2E3856] flex justify-end">
          <button
            type="button"
            onClick={() => {
              onStart?.(selected);
              onClose();
            }}
            className="h-11 px-6 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white text-sm font-bold inline-flex items-center gap-2 transition"
          >
            <span>Bắt đầu</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
}

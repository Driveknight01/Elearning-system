import React, { useEffect } from 'react';
import { X, Loader2 } from 'lucide-react';

export default function DeleteSetConfirmModal({
  open,
  isEditMode,
  loading = false,
  onClose,
  onConfirm,
}) {
  useEffect(() => {
    if (!open) return;
    const handleEsc = (e) => {
      if (e.key === 'Escape' && !loading) onClose();
    };
    window.addEventListener('keydown', handleEsc);
    return () => window.removeEventListener('keydown', handleEsc);
  }, [open, loading, onClose]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 bg-black/45 backdrop-blur-[1px] flex items-center justify-center p-4"
      onClick={(e) => {
        if (e.target === e.currentTarget && !loading) onClose();
      }}
    >
      <div className="w-full max-w-[660px] bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] shadow-2xl overflow-hidden">
        <div className="px-7 pt-5 pb-7 space-y-3">
          <div className="flex items-start justify-between gap-4">
            <h2 className="text-[24px] font-extrabold tracking-tight text-[#282E3E] dark:text-white pt-1">
              {isEditMode ? 'Xóa học phần này?' : 'Xóa học phần đang tạo?'}
            </h2>
            <button
              type="button"
              disabled={loading}
              onClick={onClose}
              aria-label="Đóng"
              className="w-9 h-9 rounded-full bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#2E3856] dark:hover:bg-[#3A466B] text-[#586380] dark:text-[#A8B1D1] flex items-center justify-center shrink-0 transition"
            >
              <X className="w-4 h-4" />
            </button>
          </div>

          <p className="text-[15px] leading-relaxed text-[#282E3E] dark:text-[#E2E8F0]">
            {isEditMode
              ? 'Hành động này sẽ xóa và loại bỏ học phần khỏi thư viện của bạn. Bạn không thể hoàn tác.'
              : 'Hành động này sẽ xóa toàn bộ tiêu đề, mô tả và các thẻ bạn vừa nhập. Bạn không thể hoàn tác.'}
          </p>
        </div>

        <div className="px-7 py-4 border-t border-[#EDEFF4] dark:border-[#2E3856] flex items-center justify-end gap-3">
          <button
            type="button"
            disabled={loading}
            onClick={onClose}
            className="h-10 px-6 rounded-full bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#2E3856] dark:hover:bg-[#3A466B] text-sm font-bold text-[#586380] dark:text-[#E2E8F0] transition"
          >
            Hủy
          </button>
          <button
            type="button"
            disabled={loading}
            onClick={onConfirm}
            className="h-10 px-6 rounded-full bg-[#D9383A] hover:bg-[#B92B2D] disabled:opacity-60 text-white text-sm font-bold inline-flex items-center gap-2 transition"
          >
            {loading && <Loader2 className="w-4 h-4 animate-spin" />}
            Xóa
          </button>
        </div>
      </div>
    </div>
  );
}

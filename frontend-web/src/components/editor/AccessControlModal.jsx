import React, { useEffect, useState } from 'react';
import { ChevronDown, Copy, Check, RefreshCw } from 'lucide-react';

const VISIBILITY_CHOICES = [
  {
    value: 'PUBLIC',
    label: 'Mọi người',
    description: 'Mọi người dùng ELearner có thể sử dụng học phần này',
  },
  {
    value: 'LINK_ONLY',
    label: 'Người có liên kết',
    description: 'Chỉ những người có liên kết chia sẻ mới có thể xem học phần này',
  },
  {
    value: 'PRIVATE',
    label: 'Chỉ tôi',
    description: 'Chỉ có bạn mới có thể xem và sử dụng học phần này',
  },
];

export default function AccessControlModal({
  open,
  onClose,
  visibility,
  onSaveVisibility,
  isEditMode,
  setId,
  shareToken,
  regenerateShareToken,
  setRegenerateShareToken,
}) {
  const [draftVisibility, setDraftVisibility] = useState(visibility || 'PUBLIC');
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (open) {
      setDraftVisibility(visibility || 'PUBLIC');
      setDropdownOpen(false);
    }
  }, [open, visibility]);

  useEffect(() => {
    if (!open) return;
    const handleEsc = (e) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleEsc);
    return () => window.removeEventListener('keydown', handleEsc);
  }, [open, onClose]);

  if (!open) return null;

  const activeChoice =
    VISIBILITY_CHOICES.find((c) => c.value === draftVisibility) || VISIBILITY_CHOICES[0];

  const shareUrl =
    isEditMode && shareToken
      ? `${window.location.origin}/sets/${setId}?token=${shareToken}`
      : '';

  const handleCopy = async () => {
    if (!shareUrl) return;
    try {
      await navigator.clipboard.writeText(shareUrl);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Ignore clipboard failure
    }
  };

  const handleSave = () => {
    onSaveVisibility(draftVisibility);
    onClose();
  };

  return (
    <div
      className="fixed inset-0 z-50 bg-black/45 backdrop-blur-[1px] flex items-center justify-center p-4"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="w-full max-w-[680px] bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] shadow-2xl overflow-visible">
        <div className="px-7 pt-6 pb-7 space-y-5">
          <h2 className="text-[24px] font-extrabold tracking-tight text-[#282E3E] dark:text-white">
            Quản lý quyền truy cập
          </h2>

          {/* Single column: Hiển thị với */}
          <div className="space-y-3 max-w-md">
            <p className="text-[14px] font-bold text-[#282E3E] dark:text-[#E2E8F0]">
              Hiển thị với
            </p>

            <div className="relative inline-block">
              <button
                type="button"
                onClick={() => setDropdownOpen((p) => !p)}
                className="inline-flex items-center gap-2 h-10 px-4 rounded-full bg-[#EDEFFF] hover:bg-[#DFE3FF] dark:bg-[#2E3856] dark:hover:bg-[#3A466B] text-[14px] font-bold text-[#4255FF] dark:text-[#9AA7FF] transition"
              >
                <span>{activeChoice.label}</span>
                <ChevronDown className="w-4 h-4" />
              </button>

              {dropdownOpen && (
                <div className="absolute left-0 mt-2 w-60 rounded-xl bg-white dark:bg-[#171836] border border-[#EDEFF4] dark:border-[#2E3856] shadow-xl py-1.5 z-20">
                  {VISIBILITY_CHOICES.map((choice) => (
                    <button
                      key={choice.value}
                      type="button"
                      onClick={() => {
                        setDraftVisibility(choice.value);
                        setDropdownOpen(false);
                      }}
                      className={`w-full text-left px-4 py-2.5 text-[14px] font-bold transition ${
                        draftVisibility === choice.value
                          ? 'bg-[#EDEFFF] text-[#4255FF] dark:bg-[#2E3856] dark:text-[#9AA7FF]'
                          : 'text-[#282E3E] dark:text-[#E2E8F0] hover:bg-[#F6F7FB] dark:hover:bg-[#202140]'
                      }`}
                    >
                      {choice.label}
                    </button>
                  ))}
                </div>
              )}
            </div>

            <p className="text-[14px] leading-relaxed text-[#586380] dark:text-[#A8B1D1]">
              {activeChoice.description}
            </p>
          </div>

          {isEditMode && shareUrl && draftVisibility === 'LINK_ONLY' && (
            <div className="p-3.5 rounded-xl bg-[#F6F7FB] dark:bg-[#171836] border border-[#EDEFF4] dark:border-[#2E3856] flex flex-col sm:flex-row sm:items-center justify-between gap-2.5">
              <div className="min-w-0 flex-1">
                <p className="text-[11px] font-bold uppercase tracking-wider text-[#586380] dark:text-[#A8B1D1]">
                  Liên kết chia sẻ
                </p>
                <p className="text-xs font-mono text-[#282E3E] dark:text-[#E2E8F0] truncate">
                  {shareUrl}
                </p>
              </div>
              <div className="flex items-center gap-2 shrink-0">
                <button
                  type="button"
                  onClick={handleCopy}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-white dark:bg-[#202140] border border-[#D9DDE8] dark:border-[#2E3856] text-xs font-bold text-[#282E3E] dark:text-white hover:border-[#4255FF]"
                >
                  {copied ? <Check className="w-3.5 h-3.5 text-emerald-500" /> : <Copy className="w-3.5 h-3.5" />}
                  {copied ? 'Đã chép' : 'Sao chép'}
                </button>
                <button
                  type="button"
                  onClick={() => setRegenerateShareToken((p) => !p)}
                  className={`inline-flex items-center gap-1 px-3 py-1.5 rounded-full text-xs font-bold border transition ${
                    regenerateShareToken
                      ? 'bg-amber-50 border-amber-300 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300'
                      : 'bg-white dark:bg-[#202140] border-[#D9DDE8] dark:border-[#2E3856] text-[#586380] dark:text-[#A8B1D1]'
                  }`}
                >
                  <RefreshCw className="w-3.5 h-3.5" />
                  {regenerateShareToken ? 'Sẽ đổi mã mới' : 'Đổi mã link'}
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-7 py-4 border-t border-[#EDEFF4] dark:border-[#2E3856] flex justify-end">
          <button
            type="button"
            onClick={handleSave}
            className="h-10 px-6 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white text-sm font-bold transition"
          >
            Lưu
          </button>
        </div>
      </div>
    </div>
  );
}

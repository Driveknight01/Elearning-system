import React, { useEffect, useState } from 'react';
import { Check, X } from 'lucide-react';

/**
 * Quizlet-style "Chia sẻ học phần này" modal.
 */
export default function ShareSetModal({
  open,
  title = '',
  shareUrl = '',
  onClose,
  onCopied,
}) {
  const [email, setEmail] = useState('');
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!open) return undefined;
    setCopied(false);
    const onEsc = (e) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onEsc);
    return () => window.removeEventListener('keydown', onEsc);
  }, [open, onClose]);

  if (!open) return null;

  const handleCopy = async () => {
    try {
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(shareUrl);
      } else {
        const tempInput = document.createElement('input');
        tempInput.value = shareUrl;
        document.body.appendChild(tempInput);
        tempInput.select();
        document.execCommand('copy');
        document.body.removeChild(tempInput);
      }
      setCopied(true);
      onCopied?.('Đã sao chép liên kết chia sẻ!');
      setTimeout(() => setCopied(false), 2200);
    } catch {
      onCopied?.('Không thể sao chép liên kết.');
    }
  };

  const handleSendEmail = (e) => {
    e.preventDefault();
    const to = email.trim();
    const subject = encodeURIComponent(`Học cùng mình bộ thẻ "${title}" trên ELearner`);
    const body = encodeURIComponent(
      `Mời bạn học bộ thẻ "${title}" trên ELearner:\n${shareUrl}`
    );
    window.open(`mailto:${encodeURIComponent(to)}?subject=${subject}&body=${body}`, '_blank');
  };

  const handleShareFacebook = () => {
    const fbUrl = `https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(shareUrl)}`;
    window.open(fbUrl, '_blank', 'noopener,noreferrer');
  };

  const handleShareX = () => {
    const text = encodeURIComponent(`Đang học "${title}" trên ELearner`);
    const xUrl = `https://twitter.com/intent/tweet?url=${encodeURIComponent(shareUrl)}&text=${text}`;
    window.open(xUrl, '_blank', 'noopener,noreferrer');
  };

  return (
    <div
      className="fixed inset-0 z-50 bg-black/45 backdrop-blur-[1px] flex items-center justify-center p-4"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="w-full max-w-[560px] bg-white dark:bg-[#202140] rounded-3xl border border-[#EDEFF4] dark:border-[#2E3856] shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="px-7 pt-6 pb-4 flex items-center justify-between gap-4">
          <h2 className="text-[22px] font-extrabold text-[#282E3E] dark:text-white tracking-tight">
            Chia sẻ học phần này
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

        {/* Body */}
        <div className="px-7 pb-7 space-y-3.5">
          {/* Email Share Row */}
          <form onSubmit={handleSendEmail} className="flex items-center gap-3">
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="Chia sẻ liên kết qua email"
              className="flex-1 h-11 px-4 rounded-xl bg-[#F6F7FB] dark:bg-[#171936] border border-transparent focus:border-[#4255FF] text-sm font-medium text-[#282E3E] dark:text-white placeholder-[#939BB4] outline-none transition"
            />
            <button
              type="submit"
              className="h-11 px-6 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white text-sm font-bold shrink-0 transition"
            >
              Gửi email
            </button>
          </form>

          {/* Copy Link Row */}
          <div className="flex items-center gap-3">
            <input
              type="text"
              readOnly
              value={shareUrl}
              onClick={(e) => e.target.select()}
              className="flex-1 h-11 px-4 rounded-xl bg-[#F6F7FB] dark:bg-[#171936] border border-transparent text-sm font-semibold text-[#282E3E] dark:text-[#E2E8F0] outline-none truncate"
            />
            <button
              type="button"
              onClick={handleCopy}
              className="h-11 px-6 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white text-sm font-bold inline-flex items-center gap-1.5 shrink-0 transition"
            >
              {copied ? (
                <>
                  <Check className="w-4 h-4" />
                  Đã chép
                </>
              ) : (
                'Chép liên kết'
              )}
            </button>
          </div>

          {/* Social Share Buttons */}
          <div className="pt-1 space-y-2.5">
            <button
              type="button"
              onClick={handleShareFacebook}
              className="w-full h-11 rounded-full bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#171936] dark:hover:bg-[#2E3856] text-sm font-bold text-[#586380] dark:text-[#E2E8F0] flex items-center justify-center gap-2.5 transition"
            >
              <span className="w-5 h-5 rounded-full bg-[#1877F2] text-white text-xs font-black flex items-center justify-center">
                f
              </span>
              Chia sẻ trên Facebook
            </button>

            <button
              type="button"
              onClick={handleShareX}
              className="w-full h-11 rounded-full bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#171936] dark:hover:bg-[#2E3856] text-sm font-bold text-[#586380] dark:text-[#E2E8F0] flex items-center justify-center gap-2.5 transition"
            >
              <span className="text-base font-black text-[#282E3E] dark:text-white leading-none">
                𝕏
              </span>
              Chia sẻ trên X
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

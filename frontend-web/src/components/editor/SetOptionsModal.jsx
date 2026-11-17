import React, { useState } from 'react';
import {
  X,
  Globe,
  Link2,
  Lock,
  Copy,
  Check,
  RefreshCw,
  ArrowLeftRight,
  Tag,
} from 'lucide-react';

const LANGUAGE_OPTIONS = [
  { code: 'en', label: 'Tiếng Anh (EN)' },
  { code: 'vi', label: 'Tiếng Việt (VI)' },
  { code: 'ja', label: 'Tiếng Nhật (JA)' },
  { code: 'ko', label: 'Tiếng Hàn (KO)' },
  { code: 'zh', label: 'Tiếng Trung (ZH)' },
  { code: 'fr', label: 'Tiếng Pháp (FR)' },
  { code: 'de', label: 'Tiếng Đức (DE)' },
  { code: 'es', label: 'Tiếng Tây Ban Nha (ES)' },
  { code: 'other', label: 'Khác' },
];

const VISIBILITY_OPTIONS = [
  {
    value: 'PUBLIC',
    label: 'Công khai',
    description: 'Mọi người đều có thể tìm kiếm và học',
    icon: Globe,
  },
  {
    value: 'LINK_ONLY',
    label: 'Ai có liên kết',
    description: 'Chỉ người có link hoặc mã chia sẻ mới xem được',
    icon: Link2,
  },
  {
    value: 'PRIVATE',
    label: 'Riêng tư',
    description: 'Chỉ mình bạn có thể xem và chỉnh sửa',
    icon: Lock,
  },
];

export default function SetOptionsModal({
  open,
  onClose,
  isEditMode,
  setId,
  visibility,
  setVisibility,
  shareToken,
  regenerateShareToken,
  setRegenerateShareToken,
  categoryId,
  setCategoryId,
  categories,
  termLang,
  setTermLang,
  defLang,
  setDefLang,
  tags,
  setTags,
}) {
  const [tagInput, setTagInput] = useState('');
  const [copiedLink, setCopiedLink] = useState(false);

  if (!open) return null;

  const shareUrl =
    isEditMode && shareToken
      ? `${window.location.origin}/sets/${setId}?token=${shareToken}`
      : '';

  const handleAddTag = (raw) => {
    const cleaned = raw.trim().replace(/^#/, '').slice(0, 32);
    if (!cleaned || tags.includes(cleaned) || tags.length >= 10) return;
    setTags((prev) => [...prev, cleaned]);
    setTagInput('');
  };

  const handleCopyShareLink = async () => {
    if (!shareUrl) return;
    try {
      await navigator.clipboard.writeText(shareUrl);
      setCopiedLink(true);
      setTimeout(() => setCopiedLink(false), 2000);
    } catch {
      // Ignore clipboard errors
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 bg-black/50 backdrop-blur-[2px] flex items-center justify-center p-4"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="w-full max-w-2xl max-h-[90vh] flex flex-col bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="px-6 py-4 border-b border-[#EDEFF4] dark:border-[#2E3856] flex items-center justify-between">
          <h3 className="text-lg font-extrabold text-[#282E3E] dark:text-white">
            Tùy chọn học phần
          </h3>
          <button
            type="button"
            onClick={onClose}
            aria-label="Đóng"
            className="p-2 rounded-full text-[#586380] hover:bg-[#F6F7FB] dark:text-[#A8B1D1] dark:hover:bg-[#2E3856] transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <div className="p-6 overflow-y-auto space-y-6 flex-1">
          {/* Visibility */}
          <div>
            <p className="text-xs font-extrabold uppercase tracking-wider text-[#586380] dark:text-[#A8B1D1] mb-2.5">
              Ai có thể xem học phần này?
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
              {VISIBILITY_OPTIONS.map((opt) => {
                const Icon = opt.icon;
                const selected = visibility === opt.value;
                return (
                  <button
                    key={opt.value}
                    type="button"
                    onClick={() => setVisibility(opt.value)}
                    className={`text-left p-3.5 rounded-xl border-2 transition ${
                      selected
                        ? 'border-[#4255FF] bg-[#EDEFFF]/60 dark:bg-[#2E3856]'
                        : 'border-[#EDEFF4] dark:border-[#2E3856] bg-[#F6F7FB]/70 dark:bg-[#171836] hover:border-[#D9DDE8]'
                    }`}
                  >
                    <div className="flex items-center gap-2">
                      <Icon
                        className={`w-4 h-4 shrink-0 ${
                          selected ? 'text-[#4255FF] dark:text-[#8E9BFF]' : 'text-[#586380] dark:text-[#A8B1D1]'
                        }`}
                      />
                      <span className="text-sm font-bold text-[#282E3E] dark:text-white">
                        {opt.label}
                      </span>
                    </div>
                    <p className="mt-1 text-xs leading-snug text-[#586380] dark:text-[#A8B1D1]">
                      {opt.description}
                    </p>
                  </button>
                );
              })}
            </div>

            {isEditMode && shareUrl && visibility !== 'PRIVATE' && (
              <div className="mt-3 p-3 rounded-xl bg-[#F6F7FB] dark:bg-[#171836] border border-[#EDEFF4] dark:border-[#2E3856] flex flex-col sm:flex-row sm:items-center justify-between gap-2">
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
                    onClick={handleCopyShareLink}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-white dark:bg-[#202140] border border-[#D9DDE8] dark:border-[#2E3856] text-xs font-bold text-[#282E3E] dark:text-white hover:border-[#4255FF] transition"
                  >
                    {copiedLink ? (
                      <>
                        <Check className="w-3.5 h-3.5 text-emerald-500" />
                        Đã chép
                      </>
                    ) : (
                      <>
                        <Copy className="w-3.5 h-3.5" />
                        Sao chép
                      </>
                    )}
                  </button>
                  <button
                    type="button"
                    onClick={() => setRegenerateShareToken((p) => !p)}
                    className={`inline-flex items-center gap-1 px-3 py-1.5 rounded-full text-xs font-bold border transition ${
                      regenerateShareToken
                        ? 'bg-amber-50 border-amber-300 text-amber-700 dark:bg-amber-900/30 dark:border-amber-700 dark:text-amber-300'
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

          {/* Category */}
          <div>
            <label className="block text-xs font-extrabold uppercase tracking-wider text-[#586380] dark:text-[#A8B1D1] mb-1.5">
              Danh mục môn học
            </label>
            <select
              value={categoryId}
              onChange={(e) => setCategoryId(e.target.value)}
              className="w-full h-10 px-3.5 rounded-xl bg-[#F6F7FB] dark:bg-[#171836] border border-[#EDEFF4] dark:border-[#2E3856] text-sm font-semibold text-[#282E3E] dark:text-white outline-none focus:border-[#4255FF]"
            >
              <option value="">-- Không chọn danh mục --</option>
              {categories.map((cat) => (
                <option key={cat.id} value={cat.id}>
                  {cat.name}
                </option>
              ))}
            </select>
          </div>

          {/* Languages */}
          <div>
            <label className="block text-xs font-extrabold uppercase tracking-wider text-[#586380] dark:text-[#A8B1D1] mb-1.5">
              Ngôn ngữ Thuật ngữ & Định nghĩa (TTS)
            </label>
            <div className="flex items-center gap-2">
              <select
                value={termLang}
                onChange={(e) => setTermLang(e.target.value)}
                className="flex-1 h-10 px-3 rounded-xl bg-[#F6F7FB] dark:bg-[#171836] border border-[#EDEFF4] dark:border-[#2E3856] text-sm font-semibold text-[#282E3E] dark:text-white outline-none focus:border-[#4255FF]"
              >
                {LANGUAGE_OPTIONS.map((l) => (
                  <option key={l.code} value={l.code}>
                    Thuật ngữ: {l.label}
                  </option>
                ))}
              </select>

              <button
                type="button"
                onClick={() => {
                  setTermLang(defLang);
                  setDefLang(termLang);
                }}
                title="Đảo ngôn ngữ"
                className="w-10 h-10 rounded-xl bg-[#F6F7FB] dark:bg-[#171836] border border-[#EDEFF4] dark:border-[#2E3856] text-[#586380] hover:text-[#4255FF] flex items-center justify-center shrink-0 transition"
              >
                <ArrowLeftRight className="w-4 h-4" />
              </button>

              <select
                value={defLang}
                onChange={(e) => setDefLang(e.target.value)}
                className="flex-1 h-10 px-3 rounded-xl bg-[#F6F7FB] dark:bg-[#171836] border border-[#EDEFF4] dark:border-[#2E3856] text-sm font-semibold text-[#282E3E] dark:text-white outline-none focus:border-[#4255FF]"
              >
                {LANGUAGE_OPTIONS.map((l) => (
                  <option key={l.code} value={l.code}>
                    Định nghĩa: {l.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Tags */}
          <div>
            <label className="block text-xs font-extrabold uppercase tracking-wider text-[#586380] dark:text-[#A8B1D1] mb-1.5">
              Thẻ gắn nhãn (Tags)
            </label>
            <div className="flex gap-2">
              <div className="relative flex-1">
                <Tag className="w-4 h-4 text-[#939BB4] absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
                <input
                  type="text"
                  value={tagInput}
                  onChange={(e) => setTagInput(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ',') {
                      e.preventDefault();
                      handleAddTag(tagInput);
                    }
                  }}
                  placeholder="Nhập tag rồi nhấn Enter..."
                  className="w-full h-10 pl-10 pr-3 rounded-xl bg-[#F6F7FB] dark:bg-[#171836] border border-[#EDEFF4] dark:border-[#2E3856] text-sm font-medium text-[#282E3E] dark:text-white outline-none focus:border-[#4255FF]"
                />
              </div>
              <button
                type="button"
                onClick={() => handleAddTag(tagInput)}
                className="px-4 h-10 rounded-xl bg-[#EDEFFF] dark:bg-[#2E3856] text-[#4255FF] dark:text-[#8E9BFF] text-xs font-bold hover:opacity-90 transition"
              >
                Thêm
              </button>
            </div>
            {tags.length > 0 && (
              <div className="mt-2.5 flex flex-wrap gap-1.5">
                {tags.map((tag) => (
                  <span
                    key={tag}
                    className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold bg-[#EDEFFF] dark:bg-[#2E3856] text-[#4255FF] dark:text-[#9AA7FF]"
                  >
                    #{tag}
                    <button
                      type="button"
                      onClick={() => setTags((prev) => prev.filter((t) => t !== tag))}
                      aria-label={`Xóa tag ${tag}`}
                      className="hover:text-rose-500"
                    >
                      <X className="w-3 h-3" />
                    </button>
                  </span>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-3.5 border-t border-[#EDEFF4] dark:border-[#2E3856] flex justify-end bg-[#F6F7FB]/60 dark:bg-[#171836]">
          <button
            type="button"
            onClick={onClose}
            className="px-6 py-2 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white text-sm font-bold transition"
          >
            Xong
          </button>
        </div>
      </div>
    </div>
  );
}

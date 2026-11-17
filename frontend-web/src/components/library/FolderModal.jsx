import React, { useEffect, useState } from 'react';
import { X, FolderPlus, Loader2 } from 'lucide-react';
import folderApi from '../../api/folderApi';
import { extractApiErrorMessage } from '../../utils/userHelpers';

const VISIBILITY_OPTIONS = [
  { value: 'PUBLIC', label: 'Công khai (Mọi người đều xem được)' },
  { value: 'LINK_ONLY', label: 'Chỉ người có liên kết' },
  { value: 'PRIVATE', label: 'Riêng tư (Chỉ mình tôi)' },
];

export default function FolderModal({ open, initialFolder, onClose, onSaved }) {
  const isEdit = Boolean(initialFolder?.id);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [visibility, setVisibility] = useState('PUBLIC');
  const [regenerateShareToken, setRegenerateShareToken] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!open) return;
    setName(initialFolder?.name || '');
    setDescription(initialFolder?.description || '');
    setVisibility(initialFolder?.visibility || 'PUBLIC');
    setRegenerateShareToken(false);
    setError('');
  }, [open, initialFolder]);

  if (!open) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!name.trim()) {
      setError('Tên thư mục không được để trống.');
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      const payload = {
        name: name.trim(),
        description: description.trim() || null,
        visibility,
        ...(isEdit ? { regenerateShareToken } : {}),
      };
      const res = isEdit
        ? await folderApi.updateFolder(initialFolder.id, payload)
        : await folderApi.createFolder(payload);
      onSaved?.(res?.data);
      onClose?.();
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Không thể lưu thư mục.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] shadow-2xl overflow-hidden">
        <div className="px-6 py-4 border-b border-[#EDEFF4] dark:border-[#2E3856] flex items-center justify-between">
          <div className="flex items-center gap-2">
            <FolderPlus className="w-5 h-5 text-[#4255FF]" />
            <h3 className="text-lg font-extrabold text-[#282E3E] dark:text-white">
              {isEdit ? 'Chỉnh sửa thư mục' : 'Tạo thư mục mới'}
            </h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-full text-[#586380] hover:bg-[#F6F7FB] dark:text-[#A8B1D1] dark:hover:bg-[#2E3856]"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="rounded-xl bg-red-50 dark:bg-[#431F25] px-3.5 py-2.5 text-xs font-semibold text-red-600 dark:text-red-300">
              {error}
            </div>
          )}

          <div>
            <label className="block text-xs font-bold uppercase tracking-wide text-[#586380] dark:text-[#A8B1D1] mb-1.5">
              Tên thư mục *
            </label>
            <input
              type="text"
              maxLength={150}
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Ví dụ: Từ vựng IELTS Academic"
              className="w-full h-10 px-3.5 rounded-xl bg-[#F6F7FB] dark:bg-[#0A082D] border border-[#D9DDE8] dark:border-[#2E3856] text-sm font-medium text-[#282E3E] dark:text-white outline-none focus:border-[#4255FF]"
            />
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wide text-[#586380] dark:text-[#A8B1D1] mb-1.5">
              Mô tả
            </label>
            <textarea
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Ghi chú ngắn gọn về các bộ thẻ trong thư mục..."
              className="w-full p-3.5 rounded-xl bg-[#F6F7FB] dark:bg-[#0A082D] border border-[#D9DDE8] dark:border-[#2E3856] text-sm font-medium text-[#282E3E] dark:text-white outline-none focus:border-[#4255FF] resize-none"
            />
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wide text-[#586380] dark:text-[#A8B1D1] mb-1.5">
              Quyền truy cập
            </label>
            <select
              value={visibility}
              onChange={(e) => setVisibility(e.target.value)}
              className="w-full h-10 px-3 rounded-xl bg-[#F6F7FB] dark:bg-[#0A082D] border border-[#D9DDE8] dark:border-[#2E3856] text-sm font-semibold text-[#282E3E] dark:text-white outline-none focus:border-[#4255FF]"
            >
              {VISIBILITY_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>

          {isEdit && (
            <label className="flex items-center gap-2 text-xs font-semibold text-[#586380] dark:text-[#A8B1D1] cursor-pointer">
              <input
                type="checkbox"
                checked={regenerateShareToken}
                onChange={(e) => setRegenerateShareToken(e.target.checked)}
                className="rounded border-[#D9DDE8] text-[#4255FF] focus:ring-[#4255FF]"
              />
              Tạo lại mã chia sẻ (Share Token) mới
            </label>
          )}

          <div className="pt-2 flex items-center justify-end gap-2.5">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl text-sm font-bold text-[#586380] hover:bg-[#F6F7FB] dark:text-[#A8B1D1] dark:hover:bg-[#2E3856]"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="inline-flex items-center gap-2 px-5 py-2 rounded-xl bg-[#4255FF] hover:bg-[#3545E0] text-white text-sm font-bold disabled:opacity-60 transition"
            >
              {submitting && <Loader2 className="w-4 h-4 animate-spin" />}
              {isEdit ? 'Lưu thay đổi' : 'Tạo thư mục'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

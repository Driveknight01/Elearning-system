import React, { useEffect, useState } from 'react';
import { Check, Folder, Loader2, Plus, X } from 'lucide-react';
import folderApi from '../../api/folderApi';
import { extractApiErrorMessage } from '../../utils/userHelpers';

/**
 * Quizlet-style "Lưu vào thư mục" modal (supports creating a new folder inline & adding/removing the set).
 */
export default function SaveToFolderModal({
  open,
  setId,
  shareToken,
  isSaved,
  onToggleBookmark,
  onClose,
  onSuccess,
}) {
  const [folders, setFolders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedIds, setSelectedIds] = useState([]);
  const [initialIds, setInitialIds] = useState([]);
  const [saving, setSaving] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  // Inline create folder form
  const [creatingNew, setCreatingNew] = useState(false);
  const [newFolderName, setNewFolderName] = useState('');
  const [creatingLoading, setCreatingLoading] = useState(false);

  useEffect(() => {
    if (!open) return;
    setErrorMsg('');
    setCreatingNew(false);
    setNewFolderName('');
    setLoading(true);
    folderApi
      .getMyFolders({ page: 0, size: 50 })
      .then((res) => {
        const items = res.data?.items || [];
        setFolders(items);
        const alreadyIn = items
          .filter((f) => Array.isArray(f.setIds) && f.setIds.includes(Number(setId)))
          .map((f) => f.id);
        setSelectedIds(alreadyIn);
        setInitialIds(alreadyIn);
      })
      .catch((err) => {
        setErrorMsg(extractApiErrorMessage(err, 'Không thể tải danh sách thư mục.'));
      })
      .finally(() => setLoading(false));
  }, [open, setId]);

  useEffect(() => {
    if (!open) return undefined;
    const onEsc = (e) => {
      if (e.key === 'Escape' && !saving) onClose();
    };
    window.addEventListener('keydown', onEsc);
    return () => window.removeEventListener('keydown', onEsc);
  }, [open, saving, onClose]);

  if (!open) return null;

  const toggleFolder = (folderId) => {
    setSelectedIds((prev) =>
      prev.includes(folderId) ? prev.filter((id) => id !== folderId) : [...prev, folderId]
    );
  };

  const handleCreateFolder = async (e) => {
    e.preventDefault();
    const trimmed = newFolderName.trim();
    if (!trimmed) return;
    setCreatingLoading(true);
    setErrorMsg('');
    try {
      const res = await folderApi.createFolder({
        title: trimmed,
        description: '',
        visibility: 'PRIVATE',
      });
      const created = res.data;
      if (created?.id) {
        setFolders((prev) => [created, ...prev]);
        setSelectedIds((prev) => [...prev, created.id]);
      }
      setNewFolderName('');
      setCreatingNew(false);
    } catch (err) {
      setErrorMsg(extractApiErrorMessage(err, 'Không thể tạo thư mục mới.'));
    } finally {
      setCreatingLoading(false);
    }
  };

  const handleSaveChanges = async () => {
    setSaving(true);
    setErrorMsg('');
    try {
      const toAdd = selectedIds.filter((id) => !initialIds.includes(id));
      const toRemove = initialIds.filter((id) => !selectedIds.includes(id));

      await Promise.all([
        ...toAdd.map((fid) => folderApi.addSetToFolder(fid, setId, shareToken)),
        ...toRemove.map((fid) => folderApi.removeSetFromFolder(fid, setId)),
      ]);

      if (!isSaved && onToggleBookmark) {
        await onToggleBookmark(true);
      }
      onSuccess?.('Đã cập nhật thư mục lưu học phần!');
      onClose();
    } catch (err) {
      setErrorMsg(extractApiErrorMessage(err, 'Không thể lưu vào thư mục.'));
    } finally {
      setSaving(false);
    }
  };

  const hasChanges =
    selectedIds.length !== initialIds.length ||
    selectedIds.some((id) => !initialIds.includes(id));

  return (
    <div
      className="fixed inset-0 z-50 bg-black/45 backdrop-blur-[1px] flex items-center justify-center p-4"
      onClick={(e) => {
        if (e.target === e.currentTarget && !saving) onClose();
      }}
    >
      <div className="w-full max-w-[520px] bg-white dark:bg-[#202140] rounded-3xl border border-[#EDEFF4] dark:border-[#2E3856] shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="px-7 pt-6 pb-4 flex items-center justify-between gap-4">
          <h2 className="text-[22px] font-extrabold text-[#282E3E] dark:text-white tracking-tight">
            Lưu vào thư mục
          </h2>
          <button
            type="button"
            disabled={saving}
            onClick={onClose}
            aria-label="Đóng"
            className="w-9 h-9 rounded-full bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#2E3856] dark:hover:bg-[#3A466B] text-[#586380] dark:text-[#A8B1D1] flex items-center justify-center transition"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Body */}
        <div className="px-7 pb-4 space-y-3 max-h-[340px] overflow-y-auto">
          {errorMsg && (
            <p className="text-xs font-semibold text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-950/40 px-3 py-2 rounded-xl">
              {errorMsg}
            </p>
          )}

          {/* Create new folder button / inline form */}
          {!creatingNew ? (
            <button
              type="button"
              onClick={() => setCreatingNew(true)}
              className="w-full h-12 px-4 rounded-2xl bg-[#F6F7FB] hover:bg-[#EDEFFF] dark:bg-[#171936] dark:hover:bg-[#2E3856] text-left flex items-center gap-3 text-sm font-bold text-[#282E3E] dark:text-white transition"
            >
              <Plus className="w-4.5 h-4.5 text-[#586380] dark:text-[#A8B1D1]" />
              <span>Thư mục mới</span>
            </button>
          ) : (
            <form onSubmit={handleCreateFolder} className="flex items-center gap-2">
              <input
                type="text"
                autoFocus
                maxLength={100}
                value={newFolderName}
                onChange={(e) => setNewFolderName(e.target.value)}
                placeholder="Nhập tên thư mục mới..."
                className="flex-1 h-11 px-4 rounded-2xl bg-[#F6F7FB] dark:bg-[#171936] border border-[#D9DDE8] dark:border-[#2E3856] text-sm font-medium text-[#282E3E] dark:text-white focus:outline-none focus:border-[#4255FF]"
              />
              <button
                type="submit"
                disabled={creatingLoading || !newFolderName.trim()}
                className="h-11 px-4 rounded-2xl bg-[#4255FF] hover:bg-[#3545E0] disabled:opacity-50 text-white text-xs font-bold inline-flex items-center gap-1.5 transition"
              >
                {creatingLoading && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                Tạo
              </button>
              <button
                type="button"
                onClick={() => setCreatingNew(false)}
                className="h-11 px-3 rounded-2xl bg-[#F6F7FB] dark:bg-[#2E3856] text-xs font-bold text-[#586380] dark:text-[#A8B1D1]"
              >
                Hủy
              </button>
            </form>
          )}

          {/* Folder List */}
          {loading ? (
            <div className="py-8 flex items-center justify-center text-[#586380]">
              <Loader2 className="w-5 h-5 animate-spin text-[#4255FF]" />
            </div>
          ) : folders.length === 0 ? (
            <p className="py-4 text-center text-xs font-medium text-[#939BB4]">
              Bạn chưa có thư mục nào. Hãy bấm &ldquo;+ Thư mục mới&rdquo; ở trên.
            </p>
          ) : (
            <div className="space-y-2 pt-1">
              {folders.map((folder) => {
                const checked = selectedIds.includes(folder.id);
                return (
                  <button
                    key={folder.id}
                    type="button"
                    onClick={() => toggleFolder(folder.id)}
                    className={`w-full px-4 py-3 rounded-2xl border flex items-center justify-between gap-3 text-left transition ${
                      checked
                        ? 'bg-[#EDEFFF]/70 border-[#4255FF]/40 dark:bg-[#2E3856] dark:border-[#8E9BFF]/50'
                        : 'bg-white border-[#EDEFF4] hover:bg-[#F6F7FB] dark:bg-[#171936] dark:border-[#2E3856] dark:hover:bg-[#2E3856]/60'
                    }`}
                  >
                    <div className="flex items-center gap-3 min-w-0">
                      <Folder className="w-4.5 h-4.5 text-[#586380] dark:text-[#A8B1D1] shrink-0" />
                      <div className="min-w-0">
                        <p className="text-sm font-bold text-[#282E3E] dark:text-white truncate">
                          {folder.title}
                        </p>
                        <p className="text-[11px] text-[#939BB4]">
                          {folder.setCount ?? 0} học phần
                        </p>
                      </div>
                    </div>

                    <div
                      className={`w-5 h-5 rounded-md border flex items-center justify-center shrink-0 transition ${
                        checked
                          ? 'bg-[#4255FF] border-[#4255FF] text-white'
                          : 'border-[#D9DDE8] dark:border-[#3A466B]'
                      }`}
                    >
                      {checked && <Check className="w-3.5 h-3.5 stroke-[3]" />}
                    </div>
                  </button>
                );
              })}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-7 py-4 border-t border-[#EDEFF4] dark:border-[#2E3856] flex items-center justify-between gap-3">
          <button
            type="button"
            onClick={() => onToggleBookmark?.(!isSaved)}
            className="text-xs font-bold text-[#4255FF] dark:text-[#8E9BFF] hover:underline"
          >
            {isSaved ? 'Bỏ lưu khỏi thư viện' : 'Chỉ lưu vào mục Đã lưu'}
          </button>

          <button
            type="button"
            disabled={saving || !hasChanges}
            onClick={handleSaveChanges}
            className="h-10 px-6 rounded-full bg-[#4255FF] hover:bg-[#3545E0] disabled:bg-[#F6F7FB] disabled:text-[#939BB4] dark:disabled:bg-[#2E3856] dark:disabled:text-[#7E89B0] text-white text-sm font-bold inline-flex items-center gap-2 transition"
          >
            {saving && <Loader2 className="w-4 h-4 animate-spin" />}
            Lưu
          </button>
        </div>
      </div>
    </div>
  );
}

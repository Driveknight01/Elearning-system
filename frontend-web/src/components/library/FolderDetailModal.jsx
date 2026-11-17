import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { X, FolderOpen, Plus, Trash2, Copy, Check, Loader2, Layers } from 'lucide-react';
import folderApi from '../../api/folderApi';
import { extractApiErrorMessage } from '../../utils/userHelpers';

export default function FolderDetailModal({ folderId, mySets = [], onClose, onChanged }) {
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [selectedSetId, setSelectedSetId] = useState('');
  const [busy, setBusy] = useState(false);
  const [copied, setCopied] = useState(false);
  const [error, setError] = useState('');

  const loadDetail = useCallback(async () => {
    if (!folderId) return;
    setLoading(true);
    setError('');
    try {
      const res = await folderApi.getFolderById(folderId);
      setDetail(res?.data || null);
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Không thể tải chi tiết thư mục.'));
    } finally {
      setLoading(false);
    }
  }, [folderId]);

  useEffect(() => {
    loadDetail();
  }, [loadDetail]);

  if (!folderId) return null;

  const existingSetIds = new Set((detail?.sets || []).map((s) => s.id));
  const availableSetsToAdd = mySets.filter((s) => !existingSetIds.has(s.id));

  const handleAddSet = async (e) => {
    e.preventDefault();
    if (!selectedSetId) return;
    setBusy(true);
    setError('');
    try {
      const res = await folderApi.addSetToFolder(folderId, Number(selectedSetId));
      setDetail(res?.data || null);
      setSelectedSetId('');
      onChanged?.();
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Không thể thêm bộ thẻ vào thư mục.'));
    } finally {
      setBusy(false);
    }
  };

  const handleRemoveSet = async (setId) => {
    setBusy(true);
    setError('');
    try {
      const res = await folderApi.removeSetFromFolder(folderId, setId);
      setDetail(res?.data || null);
      onChanged?.();
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Không thể xóa bộ thẻ khỏi thư mục.'));
    } finally {
      setBusy(false);
    }
  };

  const handleCopyShareToken = async () => {
    if (!detail?.shareToken) return;
    try {
      await navigator.clipboard.writeText(detail.shareToken);
      setCopied(true);
      setTimeout(() => setCopied(false), 1800);
    } catch {
      // Ignore clipboard failure
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="w-full max-w-2xl max-h-[88vh] flex flex-col bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] shadow-2xl overflow-hidden">
        <div className="px-6 py-4 border-b border-[#EDEFF4] dark:border-[#2E3856] flex items-center justify-between gap-3">
          <div className="flex items-center gap-2.5 min-w-0">
            <FolderOpen className="w-5 h-5 text-[#4255FF] shrink-0" />
            <h3 className="text-lg font-extrabold text-[#282E3E] dark:text-white truncate">
              {detail?.name || 'Chi tiết thư mục'}
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

        <div className="p-6 overflow-y-auto space-y-5">
          {error && (
            <div className="rounded-xl bg-red-50 dark:bg-[#431F25] px-3.5 py-2.5 text-xs font-semibold text-red-600 dark:text-red-300">
              {error}
            </div>
          )}

          {loading ? (
            <div className="py-12 flex items-center justify-center gap-2 text-sm text-[#586380] dark:text-[#A8B1D1]">
              <Loader2 className="w-5 h-5 animate-spin text-[#4255FF]" />
              Đang tải thư mục...
            </div>
          ) : detail ? (
            <>
              <div className="flex flex-wrap items-center justify-between gap-3 bg-[#F6F7FB] dark:bg-[#171836] rounded-xl p-3.5">
                <div>
                  <p className="text-sm text-[#586380] dark:text-[#A8B1D1]">
                    {detail.description || 'Chưa có mô tả cho thư mục này.'}
                  </p>
                  <p className="mt-1 text-xs font-bold text-[#282E3E] dark:text-white">
                    {detail.setCount} bộ thẻ • Quyền xem: {detail.visibility}
                  </p>
                </div>
                {detail.shareToken && (
                  <button
                    type="button"
                    onClick={handleCopyShareToken}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white dark:bg-[#202140] border border-[#D9DDE8] dark:border-[#2E3856] text-xs font-bold text-[#282E3E] dark:text-white hover:border-[#4255FF]"
                  >
                    {copied ? <Check className="w-3.5 h-3.5 text-emerald-500" /> : <Copy className="w-3.5 h-3.5" />}
                    {copied ? 'Đã copy token' : `Mã chia sẻ: ${detail.shareToken}`}
                  </button>
                )}
              </div>

              {detail.ownerView && (
                <form onSubmit={handleAddSet} className="flex flex-col sm:flex-row gap-2">
                  <select
                    value={selectedSetId}
                    onChange={(e) => setSelectedSetId(e.target.value)}
                    className="flex-1 h-10 px-3 rounded-xl bg-[#F6F7FB] dark:bg-[#0A082D] border border-[#D9DDE8] dark:border-[#2E3856] text-sm font-medium text-[#282E3E] dark:text-white outline-none focus:border-[#4255FF]"
                  >
                    <option value="">-- Chọn bộ thẻ của bạn để thêm vào thư mục --</option>
                    {availableSetsToAdd.map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.title} ({s.cardCount ?? 0} thuật ngữ)
                      </option>
                    ))}
                  </select>
                  <button
                    type="submit"
                    disabled={!selectedSetId || busy}
                    className="inline-flex items-center justify-center gap-1.5 h-10 px-4 rounded-xl bg-[#4255FF] hover:bg-[#3545E0] text-white text-sm font-bold disabled:opacity-50 transition"
                  >
                    <Plus className="w-4 h-4" />
                    Thêm vào thư mục
                  </button>
                </form>
              )}

              <div className="space-y-2.5">
                <h4 className="text-xs font-bold uppercase tracking-wide text-[#586380] dark:text-[#A8B1D1]">
                  Danh sách bộ thẻ ({detail.sets?.length || 0})
                </h4>
                {!detail.sets || detail.sets.length === 0 ? (
                  <p className="text-sm text-[#586380] dark:text-[#A8B1D1] py-6 text-center border border-dashed border-[#D9DDE8] dark:border-[#2E3856] rounded-xl">
                    Thư mục chưa có bộ thẻ nào.
                  </p>
                ) : (
                  detail.sets.map((s) => (
                    <div
                      key={s.id}
                      className="flex items-center justify-between gap-3 p-3.5 rounded-xl border border-[#EDEFF4] dark:border-[#2E3856] hover:border-[#4255FF] transition"
                    >
                      <Link to={`/sets/${s.id}`} className="min-w-0 flex-1">
                        <p className="text-sm font-bold text-[#282E3E] dark:text-white truncate hover:text-[#4255FF]">
                          {s.title}
                        </p>
                        <p className="text-xs text-[#586380] dark:text-[#A8B1D1] flex items-center gap-1.5 mt-0.5">
                          <Layers className="w-3.5 h-3.5" />
                          {s.cardCount ?? 0} thuật ngữ
                        </p>
                      </Link>
                      {detail.ownerView && (
                        <button
                          type="button"
                          disabled={busy}
                          onClick={() => handleRemoveSet(s.id)}
                          title="Xóa khỏi thư mục"
                          className="p-2 rounded-lg text-[#586380] hover:bg-red-50 hover:text-red-600 dark:text-[#A8B1D1] dark:hover:bg-[#431F25]"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      )}
                    </div>
                  ))
                )}
              </div>
            </>
          ) : null}
        </div>
      </div>
    </div>
  );
}

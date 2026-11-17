import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  X,
  Users,
  UserPlus,
  Copy,
  Check,
  Plus,
  Trash2,
  LogOut,
  Loader2,
  Layers,
} from 'lucide-react';
import studyGroupApi from '../../api/studyGroupApi';
import UserAvatar from '../common/UserAvatar';
import { useAuthStore } from '../../store/useAuthStore';
import { extractApiErrorMessage } from '../../utils/userHelpers';

export function CreateGroupModal({ open, onClose, onCreated }) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (open) {
      setName('');
      setDescription('');
      setError('');
    }
  }, [open]);

  if (!open) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!name.trim()) {
      setError('Tên nhóm học không được để trống.');
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      const res = await studyGroupApi.createGroup({
        name: name.trim(),
        description: description.trim() || null,
      });
      onCreated?.(res?.data);
      onClose?.();
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Không thể tạo nhóm học.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] shadow-2xl overflow-hidden">
        <div className="px-6 py-4 border-b border-[#EDEFF4] dark:border-[#2E3856] flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Users className="w-5 h-5 text-[#4255FF]" />
            <h3 className="text-lg font-extrabold text-[#282E3E] dark:text-white">
              Tạo nhóm học mới
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
              Tên nhóm học *
            </label>
            <input
              type="text"
              maxLength={150}
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Ví dụ: Nhóm ôn thi JLPT N3"
              className="w-full h-10 px-3.5 rounded-xl bg-[#F6F7FB] dark:bg-[#0A082D] border border-[#D9DDE8] dark:border-[#2E3856] text-sm font-medium text-[#282E3E] dark:text-white outline-none focus:border-[#4255FF]"
            />
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wide text-[#586380] dark:text-[#A8B1D1] mb-1.5">
              Mô tả nhóm
            </label>
            <textarea
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Mục tiêu học tập hoặc lịch ôn tập của nhóm..."
              className="w-full p-3.5 rounded-xl bg-[#F6F7FB] dark:bg-[#0A082D] border border-[#D9DDE8] dark:border-[#2E3856] text-sm font-medium text-[#282E3E] dark:text-white outline-none focus:border-[#4255FF] resize-none"
            />
          </div>

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
              Tạo nhóm & Sinh mã mời
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export function JoinGroupModal({ open, onClose, onJoined }) {
  const [inviteCode, setInviteCode] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (open) {
      setInviteCode('');
      setError('');
    }
  }, [open]);

  if (!open) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    const cleaned = inviteCode.trim().toUpperCase();
    if (!cleaned) {
      setError('Vui lòng nhập mã mời nhóm học.');
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      const res = await studyGroupApi.joinGroup(cleaned);
      onJoined?.(res?.data);
      onClose?.();
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Mã mời không hợp lệ hoặc bạn đã ở trong nhóm.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] shadow-2xl overflow-hidden">
        <div className="px-6 py-4 border-b border-[#EDEFF4] dark:border-[#2E3856] flex items-center justify-between">
          <div className="flex items-center gap-2">
            <UserPlus className="w-5 h-5 text-[#4255FF]" />
            <h3 className="text-lg font-extrabold text-[#282E3E] dark:text-white">
              Tham gia nhóm học
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
              Mã mời (Invite Code)
            </label>
            <input
              type="text"
              maxLength={15}
              value={inviteCode}
              onChange={(e) => setInviteCode(e.target.value.toUpperCase())}
              placeholder="Ví dụ: GRP7X9A2"
              className="w-full h-11 px-4 rounded-xl bg-[#F6F7FB] dark:bg-[#0A082D] border border-[#D9DDE8] dark:border-[#2E3856] text-base font-extrabold tracking-widest uppercase text-[#282E3E] dark:text-white outline-none focus:border-[#4255FF]"
            />
          </div>

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
              Tham gia nhóm
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export function GroupDetailModal({ groupId, mySets = [], onClose, onChanged }) {
  const { user } = useAuthStore();
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [selectedSetId, setSelectedSetId] = useState('');
  const [busy, setBusy] = useState(false);
  const [copied, setCopied] = useState(false);
  const [error, setError] = useState('');

  const loadDetail = useCallback(async () => {
    if (!groupId) return;
    setLoading(true);
    setError('');
    try {
      const res = await studyGroupApi.getGroupDetail(groupId);
      setDetail(res?.data || null);
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Không thể tải chi tiết nhóm học.'));
    } finally {
      setLoading(false);
    }
  }, [groupId]);

  useEffect(() => {
    loadDetail();
  }, [loadDetail]);

  if (!groupId) return null;

  const isOwner = detail?.myRole === 'OWNER' || user?.role === 'ADMIN';
  const sharedIds = new Set((detail?.sharedSets || []).map((item) => item.set?.id));
  const availableSetsToShare = mySets.filter((s) => !sharedIds.has(s.id));

  const handleCopyInvite = async () => {
    if (!detail?.inviteCode) return;
    try {
      await navigator.clipboard.writeText(detail.inviteCode);
      setCopied(true);
      setTimeout(() => setCopied(false), 1800);
    } catch {
      // Ignore clipboard failure
    }
  };

  const handleShareSet = async (e) => {
    e.preventDefault();
    if (!selectedSetId) return;
    setBusy(true);
    setError('');
    try {
      const res = await studyGroupApi.shareSetToGroup(groupId, Number(selectedSetId));
      setDetail(res?.data || null);
      setSelectedSetId('');
      onChanged?.();
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Không thể chia sẻ bộ thẻ vào nhóm.'));
    } finally {
      setBusy(false);
    }
  };

  const handleRemoveSharedSet = async (setId) => {
    setBusy(true);
    setError('');
    try {
      const res = await studyGroupApi.removeSetFromGroup(groupId, setId);
      setDetail(res?.data || null);
      onChanged?.();
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Không thể gỡ bộ thẻ khỏi nhóm.'));
    } finally {
      setBusy(false);
    }
  };

  const handleMemberAction = async (targetUserId) => {
    setBusy(true);
    setError('');
    try {
      await studyGroupApi.removeOrLeaveMember(groupId, targetUserId);
      if (targetUserId === user?.id) {
        onChanged?.();
        onClose?.();
        return;
      }
      await loadDetail();
      onChanged?.();
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Không thể cập nhật thành viên nhóm.'));
    } finally {
      setBusy(false);
    }
  };

  const handleDeleteGroup = async () => {
    setBusy(true);
    setError('');
    try {
      await studyGroupApi.deleteGroup(groupId);
      onChanged?.();
      onClose?.();
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Không thể xóa nhóm học.'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="w-full max-w-3xl max-h-[90vh] flex flex-col bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] shadow-2xl overflow-hidden">
        <div className="px-6 py-4 border-b border-[#EDEFF4] dark:border-[#2E3856] flex items-center justify-between gap-3">
          <div className="flex items-center gap-2.5 min-w-0">
            <Users className="w-5 h-5 text-[#4255FF] shrink-0" />
            <h3 className="text-lg font-extrabold text-[#282E3E] dark:text-white truncate">
              {detail?.name || 'Chi tiết nhóm học'}
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

        <div className="p-6 overflow-y-auto space-y-6">
          {error && (
            <div className="rounded-xl bg-red-50 dark:bg-[#431F25] px-3.5 py-2.5 text-xs font-semibold text-red-600 dark:text-red-300">
              {error}
            </div>
          )}

          {loading ? (
            <div className="py-12 flex items-center justify-center gap-2 text-sm text-[#586380] dark:text-[#A8B1D1]">
              <Loader2 className="w-5 h-5 animate-spin text-[#4255FF]" />
              Đang tải thông tin nhóm...
            </div>
          ) : detail ? (
            <>
              {/* Header Info & Invite Code */}
              <div className="flex flex-wrap items-center justify-between gap-3 bg-[#F6F7FB] dark:bg-[#171836] rounded-xl p-4">
                <div className="min-w-0">
                  <p className="text-sm text-[#586380] dark:text-[#A8B1D1]">
                    {detail.description || 'Không có mô tả.'}
                  </p>
                  <p className="mt-1 text-xs font-bold text-[#282E3E] dark:text-white">
                    Vai trò của bạn: {detail.myRole} • {detail.members?.length || 0} thành viên
                  </p>
                </div>

                <button
                  type="button"
                  onClick={handleCopyInvite}
                  className="inline-flex items-center gap-2 px-3.5 py-2 rounded-xl bg-white dark:bg-[#202140] border border-[#D9DDE8] dark:border-[#2E3856] text-xs font-extrabold tracking-wider text-[#4255FF] dark:text-[#9AA7FF] hover:border-[#4255FF]"
                >
                  {copied ? <Check className="w-4 h-4 text-emerald-500" /> : <Copy className="w-4 h-4" />}
                  {copied ? 'ĐÃ COPY MÃ' : `MÃ MỜI: ${detail.inviteCode}`}
                </button>
              </div>

              {/* Share Set to Group */}
              <form onSubmit={handleShareSet} className="flex flex-col sm:flex-row gap-2">
                <select
                  value={selectedSetId}
                  onChange={(e) => setSelectedSetId(e.target.value)}
                  className="flex-1 h-10 px-3 rounded-xl bg-[#F6F7FB] dark:bg-[#0A082D] border border-[#D9DDE8] dark:border-[#2E3856] text-sm font-medium text-[#282E3E] dark:text-white outline-none focus:border-[#4255FF]"
                >
                  <option value="">-- Chia sẻ bộ thẻ của bạn vào nhóm --</option>
                  {availableSetsToShare.map((s) => (
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
                  Chia sẻ vào nhóm
                </button>
              </form>

              {/* Shared Sets */}
              <div className="space-y-2.5">
                <h4 className="text-xs font-bold uppercase tracking-wide text-[#586380] dark:text-[#A8B1D1]">
                  Bộ thẻ được chia sẻ ({detail.sharedSets?.length || 0})
                </h4>
                {!detail.sharedSets || detail.sharedSets.length === 0 ? (
                  <p className="text-sm text-[#586380] dark:text-[#A8B1D1] py-5 text-center border border-dashed border-[#D9DDE8] dark:border-[#2E3856] rounded-xl">
                    Chưa có bộ thẻ nào được chia sẻ trong nhóm.
                  </p>
                ) : (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    {detail.sharedSets.map((item) => (
                      <div
                        key={item.set?.id}
                        className="p-3.5 rounded-xl border border-[#EDEFF4] dark:border-[#2E3856] flex items-center justify-between gap-2"
                      >
                        <Link to={`/sets/${item.set?.id}`} className="min-w-0 flex-1">
                          <p className="text-sm font-bold text-[#282E3E] dark:text-white truncate hover:text-[#4255FF]">
                            {item.set?.title}
                          </p>
                          <p className="text-xs text-[#586380] dark:text-[#A8B1D1] flex items-center gap-1.5 mt-0.5">
                            <Layers className="w-3.5 h-3.5" />
                            {item.set?.cardCount ?? 0} thuật ngữ • bởi @{item.sharedByUsername}
                          </p>
                        </Link>
                        {(isOwner || item.sharedByUserId === user?.id) && (
                          <button
                            type="button"
                            disabled={busy}
                            onClick={() => handleRemoveSharedSet(item.set?.id)}
                            title="Gỡ bộ thẻ khỏi nhóm"
                            className="p-1.5 rounded-lg text-[#586380] hover:bg-red-50 hover:text-red-600 dark:text-[#A8B1D1] dark:hover:bg-[#431F25]"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Members */}
              <div className="space-y-2.5">
                <h4 className="text-xs font-bold uppercase tracking-wide text-[#586380] dark:text-[#A8B1D1]">
                  Thành viên ({detail.members?.length || 0})
                </h4>
                <div className="divide-y divide-[#EDEFF4] dark:divide-[#2E3856] border border-[#EDEFF4] dark:border-[#2E3856] rounded-xl overflow-hidden">
                  {(detail.members || []).map((m) => (
                    <div key={m.userId} className="px-4 py-2.5 flex items-center justify-between gap-3">
                      <Link to={`/users/${m.username}`} className="flex items-center gap-2.5 min-w-0">
                        <UserAvatar src={m.avatarUrl} fullName={m.fullName} username={m.username} size="xs" />
                        <div className="min-w-0">
                          <p className="text-sm font-bold text-[#282E3E] dark:text-white truncate">
                            {m.fullName || m.username}
                          </p>
                          <p className="text-xs text-[#586380] dark:text-[#A8B1D1]">@{m.username}</p>
                        </div>
                      </Link>
                      <div className="flex items-center gap-2 shrink-0">
                        <span className="px-2 py-0.5 rounded-full text-[11px] font-bold bg-[#F6F7FB] dark:bg-[#171836] text-[#586380] dark:text-[#A8B1D1]">
                          {m.role}
                        </span>
                        {m.userId === user?.id && m.role !== 'OWNER' && (
                          <button
                            type="button"
                            disabled={busy}
                            onClick={() => handleMemberAction(m.userId)}
                            title="Rời nhóm"
                            className="p-1.5 rounded-lg text-[#586380] hover:bg-red-50 hover:text-red-600"
                          >
                            <LogOut className="w-4 h-4" />
                          </button>
                        )}
                        {isOwner && m.userId !== user?.id && (
                          <button
                            type="button"
                            disabled={busy}
                            onClick={() => handleMemberAction(m.userId)}
                            title="Xóa thành viên"
                            className="p-1.5 rounded-lg text-[#586380] hover:bg-red-50 hover:text-red-600"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {isOwner && (
                <div className="pt-2 flex justify-end">
                  <button
                    type="button"
                    disabled={busy}
                    onClick={handleDeleteGroup}
                    className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-red-50 hover:bg-red-100 dark:bg-[#431F25] text-red-600 dark:text-red-300 text-xs font-bold transition"
                  >
                    <Trash2 className="w-4 h-4" />
                    Giải tán nhóm học
                  </button>
                </div>
              )}
            </>
          ) : null}
        </div>
      </div>
    </div>
  );
}

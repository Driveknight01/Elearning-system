import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import {
  Layers,
  Folder,
  Users,
  Search,
  MoreHorizontal,
  Plus,
  Edit3,
  Trash2,
  Copy,
  Check,
  Loader2,
  X,
  UserPlus,
  BookmarkMinus,
  ExternalLink,
  Activity,
} from 'lucide-react';
import MainLayout from '../components/layout/MainLayout';
import FolderModal from '../components/library/FolderModal';
import FolderDetailModal from '../components/library/FolderDetailModal';
import EmptyStateBanner from '../components/library/EmptyStateBanner';
import {
  CreateGroupModal,
  JoinGroupModal,
  GroupDetailModal,
} from '../components/library/StudyGroupModals';
import flashcardApi from '../api/flashcardApi';
import folderApi from '../api/folderApi';
import studyGroupApi from '../api/studyGroupApi';
import { useAuthStore } from '../store/useAuthStore';
import { extractApiErrorMessage, formatShortDate } from '../utils/userHelpers';
import { MOCK_LIBRARY_SETS } from '../data/libraryExploreMockData';
import emptyActivityImg from '../assets/library/empty-activity.png';
import emptyGroupImg from '../assets/library/empty-group.png';
import emptyFolderImg from '../assets/library/empty-folder.png';

const PILL_TABS = [
  { id: 'sets', label: 'Học phần' },
  { id: 'activity', label: 'Hoạt động' },
  { id: 'groups', label: 'Nhóm học' },
  { id: 'folders', label: 'Thư mục' },
  { id: 'tests', label: 'Bài kiểm tra thử' },
];

export default function LibraryPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const location = useLocation();
  const navigate = useNavigate();
  const { user } = useAuthStore();

  const rawTab = searchParams.get('tab') || 'sets';
  const normalizedTab = rawTab === 'my' ? 'sets' : rawTab === 'saved' ? 'activity' : rawTab;
  const activeTab = PILL_TABS.some((t) => t.id === normalizedTab) ? normalizedTab : 'sets';

  const [setScope, setSetScope] = useState('all'); // 'all' | 'mine' | 'saved' | 'recent'
  const [searchFilter, setSearchFilter] = useState('');
  const [folderFilter, setFolderFilter] = useState('mine');

  const [mySets, setMySets] = useState([]);
  const [savedSets, setSavedSets] = useState([]);
  const [recentSets, setRecentSets] = useState([]);
  const [folders, setFolders] = useState([]);
  const [groups, setGroups] = useState([]);

  const [loading, setLoading] = useState(true);
  const [openMenuSetId, setOpenMenuSetId] = useState(null);
  const [bannerMsg, setBannerMsg] = useState({ type: '', text: '' });

  // Modals state
  const [folderModalOpen, setFolderModalOpen] = useState(false);
  const [editingFolder, setEditingFolder] = useState(null);
  const [detailFolderId, setDetailFolderId] = useState(null);

  const [createGroupOpen, setCreateGroupOpen] = useState(false);
  const [joinGroupOpen, setJoinGroupOpen] = useState(false);
  const [detailGroupId, setDetailGroupId] = useState(null);

  const [copiedCode, setCopiedCode] = useState('');

  const showMessage = (type, text) => {
    setBannerMsg({ type, text });
    if (type === 'success') {
      setTimeout(() => setBannerMsg({ type: '', text: '' }), 3000);
    }
  };

  const setActiveTab = (tabId) => {
    const next = new URLSearchParams(searchParams);
    if (tabId === 'sets') {
      next.delete('tab');
    } else {
      next.set('tab', tabId);
    }
    setSearchParams(next);
  };

  const loadAllLibraryData = useCallback(async () => {
    setLoading(true);
    try {
      const [myRes, savedRes, recentRes, folderRes, groupRes] = await Promise.all([
        flashcardApi.getMySets({ page: 0, size: 50 }).catch(() => null),
        flashcardApi.getSavedSets({ page: 0, size: 50 }).catch(() => null),
        flashcardApi.getRecentSets({ page: 0, size: 30 }).catch(() => null),
        folderApi.getMyFolders({ page: 0, size: 50 }).catch(() => null),
        studyGroupApi.getMyGroups().catch(() => null),
      ]);
      setMySets(myRes?.data?.content || []);
      setSavedSets(savedRes?.data?.content || []);
      setRecentSets(recentRes?.data?.content || []);
      setFolders(folderRes?.data?.content || []);
      setGroups(groupRes?.data || []);
    } catch (err) {
      showMessage('error', extractApiErrorMessage(err, 'Không thể tải dữ liệu thư viện.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadAllLibraryData();
  }, [loadAllLibraryData]);

  useEffect(() => {
    if (location.state?.toast) {
      showMessage('success', location.state.toast);
      navigate(`${location.pathname}${location.search}`, { replace: true, state: null });
    }
  }, [location.state, location.pathname, location.search, navigate]);

  useEffect(() => {
    const closeMenu = () => setOpenMenuSetId(null);
    document.addEventListener('click', closeMenu);
    return () => document.removeEventListener('click', closeMenu);
  }, []);

  // Combine sets for Quizlet "Học phần" list view
  const combinedSetRows = useMemo(() => {
    const savedSetIds = new Set(savedSets.map((item) => item.setId));
    const rows = [];
    const seen = new Set();

    const pushRow = (set, meta = {}) => {
      if (!set || seen.has(set.id)) return;
      seen.add(set.id);
      const isMine = set.owner?.id === user?.id;
      rows.push({
        set,
        isMine,
        isSaved: savedSetIds.has(set.id),
        lastAccessedAt: meta.lastAccessedAt || set.updatedAt || set.createdAt,
        lastStudyMode: meta.lastStudyMode || null,
      });
    };

    if (setScope === 'all' || setScope === 'recent') {
      recentSets.forEach((item) =>
        pushRow(item.set, { lastAccessedAt: item.lastAccessedAt, lastStudyMode: item.lastStudyMode })
      );
    }
    if (setScope === 'all' || setScope === 'mine') {
      mySets.forEach((s) => pushRow(s));
    }
    if (setScope === 'all' || setScope === 'saved') {
      savedSets.forEach((item) => pushRow(item.set));
    }

    // Fallback to mock sets when DB has no sets yet so UI matches Quizlet preview
    if (rows.length === 0 && mySets.length === 0 && savedSets.length === 0 && recentSets.length === 0) {
      MOCK_LIBRARY_SETS.forEach((m) => {
        if (setScope === 'mine' && !m.isMineMock) return;
        rows.push({
          set: m,
          isMine: Boolean(m.isMineMock),
          isSaved: false,
          isMock: true,
        });
      });
    }

    const q = searchFilter.trim().toLowerCase();
    if (!q) return rows;
    return rows.filter(
      ({ set }) =>
        set.title?.toLowerCase().includes(q) ||
        set.owner?.username?.toLowerCase().includes(q)
    );
  }, [mySets, savedSets, recentSets, setScope, searchFilter, user?.id]);

  const handleDeleteSet = async (set) => {
    if (!window.confirm(`Bạn có chắc muốn xóa học phần "${set.title}"?`)) return;
    try {
      await flashcardApi.deleteSet(set.id);
      showMessage('success', `Đã xóa "${set.title}".`);
      await loadAllLibraryData();
    } catch (err) {
      showMessage('error', extractApiErrorMessage(err, 'Không thể xóa học phần.'));
    }
  };

  const handleCloneSet = async (set) => {
    try {
      await flashcardApi.cloneSet(set.id);
      showMessage('success', `Đã nhân bản "${set.title}" vào thư viện của bạn.`);
      await loadAllLibraryData();
    } catch (err) {
      showMessage('error', extractApiErrorMessage(err, 'Không thể nhân bản học phần.'));
    }
  };

  const handleUnsaveSet = async (set) => {
    try {
      await flashcardApi.unsaveSet(set.id);
      showMessage('success', `Đã bỏ lưu "${set.title}".`);
      await loadAllLibraryData();
    } catch (err) {
      showMessage('error', extractApiErrorMessage(err, 'Không thể bỏ lưu học phần.'));
    }
  };

  const handleDeleteFolder = async (folder) => {
    if (!window.confirm(`Bạn có chắc muốn xóa thư mục "${folder.name}"?`)) return;
    try {
      await folderApi.deleteFolder(folder.id);
      showMessage('success', `Đã xóa thư mục "${folder.name}".`);
      await loadAllLibraryData();
    } catch (err) {
      showMessage('error', extractApiErrorMessage(err, 'Không thể xóa thư mục.'));
    }
  };

  const handleCopyInviteCode = async (code, e) => {
    e.stopPropagation();
    try {
      await navigator.clipboard.writeText(code);
      setCopiedCode(code);
      setTimeout(() => setCopiedCode(''), 1800);
    } catch {
      // Ignore clipboard failure
    }
  };

  return (
    <MainLayout>
      <div className="max-w-5xl mx-auto px-4 sm:px-8 py-6 pb-24 space-y-6">
        {/* Quizlet-style Page Title */}
        <h1 className="text-2xl sm:text-[28px] font-extrabold text-[#282E3E] dark:text-white tracking-tight">
          Thư viện của bạn
        </h1>

        {/* Quizlet-style Pill Tabs */}
        <div className="flex flex-wrap items-center gap-2">
          {PILL_TABS.map((tab) => {
            const active = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                onClick={() => setActiveTab(tab.id)}
                className={`px-4 py-2 rounded-full text-[13px] font-bold transition ${
                  active
                    ? 'bg-white dark:bg-[#202140] text-[#282E3E] dark:text-white border-2 border-[#282E3E] dark:border-white shadow-xs'
                    : 'bg-[#F6F7FB] dark:bg-[#202140] text-[#586380] dark:text-[#A8B1D1] border-2 border-transparent hover:bg-[#EDEFF4] dark:hover:bg-[#2E3856]'
                }`}
              >
                {tab.label}
              </button>
            );
          })}
        </div>

        {bannerMsg.text && (
          <div
            className={`rounded-xl px-4 py-3 text-sm font-medium flex items-center justify-between ${
              bannerMsg.type === 'error'
                ? 'bg-red-50 dark:bg-[#431F25] text-red-700 dark:text-red-300 border border-red-200 dark:border-red-900/50'
                : 'bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-900/50'
            }`}
          >
            <span>{bannerMsg.text}</span>
            <button type="button" onClick={() => setBannerMsg({ type: '', text: '' })} className="p-1">
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

        {loading ? (
          <div className="py-16 flex items-center justify-center gap-2 text-sm font-semibold text-[#586380] dark:text-[#A8B1D1]">
            <Loader2 className="w-5 h-5 animate-spin text-[#4255FF]" />
            Đang tải thư viện...
          </div>
        ) : (
          <>
            {/* TAB 1: HỌC PHẦN (Quizlet list view) */}
            {activeTab === 'sets' && (
              <div className="space-y-5">
                {/* Quizlet Create Banner (gray card, gray text) */}
                <div className="rounded-2xl bg-[#F6F7FB] dark:bg-[#202140] px-5 py-4 flex items-center justify-between gap-4">
                  <div className="flex items-center gap-3.5">
                    <div className="w-9 h-9 rounded-lg bg-[#E0F2FE] dark:bg-[#1E3A5F] text-[#0284C7] dark:text-[#38BDF8] flex items-center justify-center shrink-0">
                      <Layers className="w-5 h-5" />
                    </div>
                    <span className="text-[15px] font-bold text-[#586380] dark:text-[#A8B1D1]">
                      Tạo bộ thẻ ghi nhớ
                    </span>
                  </div>
                  <Link
                    to="/sets/create"
                    className="px-5 py-2 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white text-sm font-bold transition shrink-0"
                  >
                    Tạo
                  </Link>
                </div>

                {/* Filter Dropdown (gray text) + Search Input */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <select
                    value={setScope}
                    onChange={(e) => setSetScope(e.target.value)}
                    aria-label="Lọc danh sách học phần"
                    className="w-fit h-9 px-2 rounded-lg bg-transparent text-sm font-semibold text-[#586380] dark:text-[#A8B1D1] outline-none cursor-pointer"
                  >
                    <option value="all" className="dark:bg-[#202140]">Tất cả</option>
                    <option value="mine" className="dark:bg-[#202140]">Do bạn tạo</option>
                    <option value="saved" className="dark:bg-[#202140]">Đã lưu</option>
                    <option value="recent" className="dark:bg-[#202140]">Học gần đây</option>
                  </select>

                  <div className="relative w-full sm:w-80">
                    <input
                      type="text"
                      value={searchFilter}
                      onChange={(e) => setSearchFilter(e.target.value)}
                      placeholder="Tìm kiếm thẻ ghi nhớ"
                      className="w-full h-10 pl-4 pr-10 rounded-xl bg-[#F6F7FB] dark:bg-[#202140] border border-transparent focus:border-[#4255FF] text-sm font-medium text-[#282E3E] dark:text-white placeholder:text-[#586380] dark:placeholder:text-[#A8B1D1] outline-none transition"
                    />
                    <Search className="w-4 h-4 text-[#586380] dark:text-[#A8B1D1] absolute right-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
                  </div>
                </div>

                {/* Quizlet Vertical List (shifted down slightly for breathing room) */}
                {combinedSetRows.length === 0 ? (
                  <p className="pt-8 pb-6 text-center text-sm font-medium text-[#586380] dark:text-[#A8B1D1]">
                    Không tìm thấy học phần nào khớp với &quot;{searchFilter}&quot;.
                  </p>
                ) : (
                  <div className="pt-4 space-y-1">
                    {combinedSetRows.map(({ set, isMine, isSaved, lastAccessedAt }) => (
                      <div
                        key={set.id}
                        className="px-3 py-3.5 rounded-xl flex items-center justify-between gap-4 hover:bg-[#F6F7FB] dark:hover:bg-[#202140] transition"
                      >
                        <Link to={`/sets/${set.id}`} className="flex items-center gap-4 min-w-0 flex-1">
                          <div className="w-10 h-10 rounded-xl bg-[#E0F2FE] dark:bg-[#1E3A5F] text-[#0284C7] dark:text-[#38BDF8] flex items-center justify-center shrink-0">
                            <Layers className="w-5 h-5" />
                          </div>
                          <div className="min-w-0">
                            <p className="text-[15px] font-bold text-[#282E3E] dark:text-white truncate">
                              {set.title}
                            </p>
                            <p className="mt-0.5 text-[13px] font-semibold text-[#586380] dark:text-[#A8B1D1] truncate">
                              {set.mockSubtitle || (
                                <>
                                  {set.cardCount ?? 0} thuật ngữ
                                  {'  •  '}
                                  Tác giả: {isMine ? 'bạn' : set.owner?.username || 'ẩn danh'}
                                  {lastAccessedAt ? `  •  Hoạt động gần đây trên ${formatShortDate(lastAccessedAt)}` : ''}
                                </>
                              )}
                            </p>
                          </div>
                        </Link>

                        <div className="relative shrink-0" onClick={(e) => e.stopPropagation()}>
                          <button
                            type="button"
                            onClick={() =>
                              setOpenMenuSetId((prev) => (prev === set.id ? null : set.id))
                            }
                            aria-label="Tùy chọn học phần"
                            className="p-2 rounded-full text-[#586380] hover:bg-[#EDEFF4] dark:text-[#A8B1D1] dark:hover:bg-[#2E3856]"
                          >
                            <MoreHorizontal className="w-5 h-5" />
                          </button>

                          {openMenuSetId === set.id && (
                            <div className="absolute right-0 mt-1 w-48 bg-white dark:bg-[#0A082D] rounded-xl border border-[#EDEFF4] dark:border-[#2E3856] shadow-xl py-1.5 z-30">
                              <Link
                                to={`/sets/${set.id}`}
                                className="flex items-center gap-2.5 px-3.5 py-2 text-xs font-bold text-[#282E3E] dark:text-white hover:bg-[#F6F7FB] dark:hover:bg-[#202140]"
                              >
                                <ExternalLink className="w-3.5 h-3.5" />
                                Mở học phần
                              </Link>
                              {isMine && (
                                <Link
                                  to={`/sets/${set.id}/edit`}
                                  className="flex items-center gap-2.5 px-3.5 py-2 text-xs font-bold text-[#282E3E] dark:text-white hover:bg-[#F6F7FB] dark:hover:bg-[#202140]"
                                >
                                  <Edit3 className="w-3.5 h-3.5" />
                                  Chỉnh sửa
                                </Link>
                              )}
                              <button
                                type="button"
                                onClick={() => {
                                  setOpenMenuSetId(null);
                                  handleCloneSet(set);
                                }}
                                className="w-full flex items-center gap-2.5 px-3.5 py-2 text-xs font-bold text-[#282E3E] dark:text-white hover:bg-[#F6F7FB] dark:hover:bg-[#202140]"
                              >
                                <Copy className="w-3.5 h-3.5" />
                                Nhân bản
                              </button>
                              {isSaved && (
                                <button
                                  type="button"
                                  onClick={() => {
                                    setOpenMenuSetId(null);
                                    handleUnsaveSet(set);
                                  }}
                                  className="w-full flex items-center gap-2.5 px-3.5 py-2 text-xs font-bold text-[#586380] dark:text-[#A8B1D1] hover:bg-[#F6F7FB] dark:hover:bg-[#202140]"
                                >
                                  <BookmarkMinus className="w-3.5 h-3.5" />
                                  Bỏ lưu
                                </button>
                              )}
                              {isMine && (
                                <button
                                  type="button"
                                  onClick={() => {
                                    setOpenMenuSetId(null);
                                    handleDeleteSet(set);
                                  }}
                                  className="w-full flex items-center gap-2.5 px-3.5 py-2 text-xs font-bold text-red-600 hover:bg-red-50 dark:hover:bg-[#431F25]"
                                >
                                  <Trash2 className="w-3.5 h-3.5" />
                                  Xóa học phần
                                </button>
                              )}
                            </div>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* TAB 2: HOẠT ĐỘNG (Quizlet Activity Banner + Empty State / Recents) */}
            {activeTab === 'activity' && (
              <div className="space-y-6">
                <div className="rounded-2xl bg-[#F6F7FB] dark:bg-[#202140] px-5 py-4 flex items-center justify-between gap-4">
                  <div className="flex items-center gap-3.5">
                    <div className="w-9 h-9 rounded-lg bg-white dark:bg-[#2E3856] text-[#586380] dark:text-[#A8B1D1] flex items-center justify-center shrink-0">
                      <Activity className="w-5 h-5" />
                    </div>
                    <span className="text-[15px] font-bold text-[#586380] dark:text-[#A8B1D1]">
                      Học với bạn bè để xem tiến độ học tập được chia sẻ
                    </span>
                  </div>
                  <Link
                    to="/explore"
                    className="px-5 py-2 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white text-sm font-bold transition shrink-0"
                  >
                    Bắt đầu hoạt động
                  </Link>
                </div>

                {recentSets.length === 0 ? (
                  <EmptyStateBanner
                    variant="activity"
                    imageSrc={emptyActivityImg}
                    title="Học với bạn bè để xem tiến độ học được chia sẻ"
                    actionLabel="Bắt đầu một hoạt động"
                    actionTo="/explore"
                  />
                ) : (
                  <div className="divide-y divide-[#EDEFF4] dark:divide-[#2E3856] bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856]">
                    {recentSets.map((item) => (
                      <Link
                        key={item.setId}
                        to={`/sets/${item.setId}`}
                        className="px-5 py-4 flex items-center justify-between gap-4 hover:bg-[#F6F7FB]/70 dark:hover:bg-[#2E3856]/40 transition first:rounded-t-2xl last:rounded-b-2xl"
                      >
                        <div className="flex items-center gap-4 min-w-0">
                          <div className="w-10 h-10 rounded-xl bg-[#EDEFFF] dark:bg-[#2E3856] text-[#4255FF] flex items-center justify-center shrink-0">
                            <Layers className="w-5 h-5" />
                          </div>
                          <div className="min-w-0">
                            <p className="text-[15px] font-extrabold text-[#282E3E] dark:text-white truncate">
                              {item.set?.title}
                            </p>
                            <p className="text-[13px] font-semibold text-[#586380] dark:text-[#A8B1D1]">
                              Chế độ: {item.lastStudyMode || 'FLASHCARD'}
                              {item.lastAccessedAt ? ` • ${formatShortDate(item.lastAccessedAt)}` : ''}
                            </p>
                          </div>
                        </div>
                        <span className="text-xs font-bold text-[#4255FF] shrink-0">Tiếp tục →</span>
                      </Link>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* TAB 3: NHÓM HỌC (Quizlet Class/Group Empty State + List) */}
            {activeTab === 'groups' && (
              <div className="space-y-5">
                {groups.length > 0 && (
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <p className="text-sm font-bold text-[#586380] dark:text-[#A8B1D1]">
                      {groups.length} nhóm học bạn đang tham gia
                    </p>
                    <div className="flex items-center gap-2.5">
                      <button
                        type="button"
                        onClick={() => setJoinGroupOpen(true)}
                        className="inline-flex items-center gap-1.5 px-4 py-2 rounded-full bg-[#F6F7FB] dark:bg-[#202140] border border-[#D9DDE8] dark:border-[#2E3856] text-sm font-bold text-[#282E3E] dark:text-white hover:border-[#4255FF]"
                      >
                        <UserPlus className="w-4 h-4 text-[#4255FF]" />
                        Nhập mã mời
                      </button>
                      <button
                        type="button"
                        onClick={() => setCreateGroupOpen(true)}
                        className="inline-flex items-center gap-1.5 px-5 py-2 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white text-sm font-bold transition"
                      >
                        <Plus className="w-4 h-4" />
                        Tạo nhóm học
                      </button>
                    </div>
                  </div>
                )}

                {groups.length === 0 ? (
                  <EmptyStateBanner
                    variant="group"
                    imageSrc={emptyGroupImg}
                    title="Bạn chưa tạo hoặc tham gia nhóm học nào"
                    description="Tạo một nhóm học để sắp xếp học phần của mình và chia sẻ chúng với bạn bè cùng mã mời."
                    actionLabel="Tạo nhóm học"
                    onAction={() => setCreateGroupOpen(true)}
                    secondaryLabel="Tham gia bằng mã mời"
                    onSecondaryAction={() => setJoinGroupOpen(true)}
                  />
                ) : (
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                    {groups.map((g) => (
                      <div
                        key={g.id}
                        onClick={() => setDetailGroupId(g.id)}
                        className="cursor-pointer bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] hover:border-[#4255FF] p-5 flex flex-col justify-between transition shadow-xs hover:shadow-md"
                      >
                        <div>
                          <div className="flex items-center justify-between gap-2 mb-2">
                            <span className="inline-flex items-center gap-1 text-xs font-bold text-[#4255FF] dark:text-[#9AA7FF]">
                              <Users className="w-4 h-4" />
                              {g.memberCount} thành viên • {g.setCount} học phần
                            </span>
                            <span className="text-[11px] font-bold px-2 py-0.5 rounded-full bg-[#EDEFFF] dark:bg-[#2E3856] text-[#4255FF] dark:text-[#9AA7FF]">
                              {g.myRole}
                            </span>
                          </div>
                          <h3 className="text-base font-extrabold text-[#282E3E] dark:text-white line-clamp-1">
                            {g.name}
                          </h3>
                          {g.description && (
                            <p className="mt-1 text-xs text-[#586380] dark:text-[#A8B1D1] line-clamp-2">
                              {g.description}
                            </p>
                          )}
                        </div>

                        <div className="mt-4 pt-3 border-t border-[#EDEFF4] dark:border-[#2E3856] flex items-center justify-between gap-2">
                          <span className="text-xs font-mono font-bold text-[#586380] dark:text-[#A8B1D1]">
                            Mã: {g.inviteCode}
                          </span>
                          <button
                            type="button"
                            onClick={(e) => handleCopyInviteCode(g.inviteCode, e)}
                            className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-[#F6F7FB] dark:bg-[#171836] text-xs font-bold text-[#282E3E] dark:text-white hover:text-[#4255FF]"
                          >
                            {copiedCode === g.inviteCode ? (
                              <>
                                <Check className="w-3.5 h-3.5 text-emerald-500" />
                                Đã copy
                              </>
                            ) : (
                              <>
                                <Copy className="w-3.5 h-3.5" />
                                Copy mã
                              </>
                            )}
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* TAB 4: THƯ MỤC (Quizlet Banner + Empty State + List) */}
            {activeTab === 'folders' && (
              <div className="space-y-5">
                <div className="rounded-2xl bg-[#F6F7FB] dark:bg-[#202140] px-5 py-4 flex items-center justify-between gap-4">
                  <div className="flex items-center gap-3.5">
                    <div className="w-9 h-9 rounded-lg bg-white dark:bg-[#2E3856] text-[#586380] dark:text-[#A8B1D1] flex items-center justify-center shrink-0">
                      <Folder className="w-5 h-5" />
                    </div>
                    <span className="text-[15px] font-bold text-[#586380] dark:text-[#A8B1D1]">
                      Tạo một thư mục để sắp xếp các học phần
                    </span>
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      setEditingFolder(null);
                      setFolderModalOpen(true);
                    }}
                    className="px-5 py-2 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white text-sm font-bold transition shrink-0"
                  >
                    Tạo
                  </button>
                </div>

                <div>
                  <select
                    value={folderFilter}
                    onChange={(e) => setFolderFilter(e.target.value)}
                    aria-label="Lọc thư mục"
                    className="h-9 px-2 rounded-lg bg-transparent text-sm font-semibold text-[#586380] dark:text-[#A8B1D1] outline-none cursor-pointer"
                  >
                    <option value="mine" className="dark:bg-[#202140]">Do bạn tạo</option>
                    <option value="all" className="dark:bg-[#202140]">Tất cả thư mục</option>
                  </select>
                </div>

                {folders.length === 0 ? (
                  <EmptyStateBanner
                    variant="folder"
                    imageSrc={emptyFolderImg}
                    title="Bạn chưa tạo thư mục nào"
                    description="Tạo một thư mục để sắp xếp các học phần"
                    actionLabel="Tạo thư mục"
                    onAction={() => {
                      setEditingFolder(null);
                      setFolderModalOpen(true);
                    }}
                  />
                ) : (
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                    {folders.map((folder) => (
                      <div
                        key={folder.id}
                        onClick={() => setDetailFolderId(folder.id)}
                        className="cursor-pointer bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] hover:border-[#4255FF] p-5 flex flex-col justify-between transition shadow-xs hover:shadow-md"
                      >
                        <div>
                          <div className="flex items-center justify-between gap-2 mb-2">
                            <span className="inline-flex items-center gap-1.5 text-xs font-bold text-[#4255FF] dark:text-[#9AA7FF]">
                              <Folder className="w-4 h-4" />
                              {folder.setCount} học phần
                            </span>
                            <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full bg-[#F6F7FB] dark:bg-[#171836] text-[#586380] dark:text-[#A8B1D1]">
                              {folder.visibility}
                            </span>
                          </div>
                          <h3 className="text-base font-extrabold text-[#282E3E] dark:text-white line-clamp-1">
                            {folder.name}
                          </h3>
                          {folder.description && (
                            <p className="mt-1 text-xs text-[#586380] dark:text-[#A8B1D1] line-clamp-2">
                              {folder.description}
                            </p>
                          )}
                        </div>

                        <div
                          className="mt-4 pt-3 border-t border-[#EDEFF4] dark:border-[#2E3856] flex items-center justify-end gap-1.5"
                          onClick={(e) => e.stopPropagation()}
                        >
                          <button
                            type="button"
                            onClick={() => {
                              setEditingFolder(folder);
                              setFolderModalOpen(true);
                            }}
                            title="Sửa thư mục"
                            className="p-1.5 rounded-lg text-[#586380] hover:bg-[#F6F7FB] hover:text-[#4255FF] dark:text-[#A8B1D1] dark:hover:bg-[#2E3856]"
                          >
                            <Edit3 className="w-4 h-4" />
                          </button>
                          <button
                            type="button"
                            onClick={() => handleDeleteFolder(folder)}
                            title="Xóa thư mục"
                            className="p-1.5 rounded-lg text-[#586380] hover:bg-red-50 hover:text-red-600 dark:text-[#A8B1D1] dark:hover:bg-[#431F25]"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* TAB 5: BÀI KIỂM TRA THỬ */}
            {activeTab === 'tests' && (
              <EmptyStateBanner
                variant="test"
                imageSrc={emptyGroupImg}
                title="Tìm và làm các bài kiểm tra thử dựa trên những gì bạn đang học"
                actionLabel="Tìm kiếm bài kiểm tra thử"
                actionTo="/explore?mode=test"
              />
            )}
          </>
        )}
      </div>

      {/* Folder Modals */}
      <FolderModal
        open={folderModalOpen}
        initialFolder={editingFolder}
        onClose={() => setFolderModalOpen(false)}
        onSaved={() => {
          showMessage('success', editingFolder ? 'Đã cập nhật thư mục.' : 'Đã tạo thư mục mới.');
          loadAllLibraryData();
        }}
      />

      <FolderDetailModal
        folderId={detailFolderId}
        mySets={mySets}
        onClose={() => setDetailFolderId(null)}
        onChanged={loadAllLibraryData}
      />

      {/* Study Group Modals */}
      <CreateGroupModal
        open={createGroupOpen}
        onClose={() => setCreateGroupOpen(false)}
        onCreated={(created) => {
          showMessage('success', `Đã tạo nhóm "${created?.name || ''}" (Mã mời: ${created?.inviteCode || ''}).`);
          loadAllLibraryData();
        }}
      />

      <JoinGroupModal
        open={joinGroupOpen}
        onClose={() => setJoinGroupOpen(false)}
        onJoined={(joined) => {
          showMessage('success', `Đã tham gia nhóm "${joined?.name || ''}".`);
          loadAllLibraryData();
        }}
      />

      <GroupDetailModal
        groupId={detailGroupId}
        mySets={mySets}
        onClose={() => setDetailGroupId(null)}
        onChanged={loadAllLibraryData}
      />
    </MainLayout>
  );
}

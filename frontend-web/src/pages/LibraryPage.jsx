import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import {
  Layers,
  Bookmark,
  Folder,
  Users,
  Plus,
  Search,
  UserPlus,
  Edit3,
  Trash2,
  Copy,
  Check,
  Clock,
  ChevronLeft,
  ChevronRight,
  Loader2,
  X,
} from 'lucide-react';
import MainLayout from '../components/layout/MainLayout';
import FlashcardSetCard from '../components/library/FlashcardSetCard';
import FolderModal from '../components/library/FolderModal';
import FolderDetailModal from '../components/library/FolderDetailModal';
import {
  CreateGroupModal,
  JoinGroupModal,
  GroupDetailModal,
} from '../components/library/StudyGroupModals';
import flashcardApi from '../api/flashcardApi';
import folderApi from '../api/folderApi';
import studyGroupApi from '../api/studyGroupApi';
import { extractApiErrorMessage, formatShortDate } from '../utils/userHelpers';

const TABS = [
  { id: 'my', label: 'Bộ thẻ của tôi', icon: Layers },
  { id: 'saved', label: 'Đã lưu & Gần đây', icon: Bookmark },
  { id: 'folders', label: 'Thư mục', icon: Folder },
  { id: 'groups', label: 'Nhóm học', icon: Users },
];

export default function LibraryPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const rawTab = searchParams.get('tab') || 'my';
  const activeTab = TABS.some((t) => t.id === rawTab) ? rawTab : 'my';

  const [mySetsPage, setMySetsPage] = useState(0);
  const [mySetsData, setMySetsData] = useState(null);
  const [myFilter, setMyFilter] = useState('');

  const [savedSets, setSavedSets] = useState([]);
  const [recentSets, setRecentSets] = useState([]);

  const [folders, setFolders] = useState([]);
  const [groups, setGroups] = useState([]);

  const [loading, setLoading] = useState(true);
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
    if (tabId === 'my') {
      next.delete('tab');
    } else {
      next.set('tab', tabId);
    }
    setSearchParams(next);
  };

  const loadMySets = useCallback(async (page = mySetsPage) => {
    const res = await flashcardApi.getMySets({ page, size: 12 });
    setMySetsData(res?.data || null);
  }, [mySetsPage]);

  const loadSavedAndRecents = useCallback(async () => {
    const [savedRes, recentRes] = await Promise.all([
      flashcardApi.getSavedSets({ page: 0, size: 30 }),
      flashcardApi.getRecentSets({ page: 0, size: 20 }),
    ]);
    setSavedSets(savedRes?.data?.content || []);
    setRecentSets(recentRes?.data?.content || []);
  }, []);

  const loadFolders = useCallback(async () => {
    const res = await folderApi.getMyFolders({ page: 0, size: 50 });
    setFolders(res?.data?.content || []);
  }, []);

  const loadGroups = useCallback(async () => {
    const res = await studyGroupApi.getMyGroups();
    setGroups(res?.data || []);
  }, []);

  const loadActiveTabData = useCallback(async () => {
    setLoading(true);
    try {
      if (activeTab === 'my') {
        await loadMySets(mySetsPage);
      } else if (activeTab === 'saved') {
        await loadSavedAndRecents();
      } else if (activeTab === 'folders') {
        await Promise.all([loadFolders(), loadMySets(0)]);
      } else if (activeTab === 'groups') {
        await Promise.all([loadGroups(), loadMySets(0)]);
      }
    } catch (err) {
      showMessage('error', extractApiErrorMessage(err, 'Không thể tải dữ liệu thư viện.'));
    } finally {
      setLoading(false);
    }
  }, [activeTab, mySetsPage, loadMySets, loadSavedAndRecents, loadFolders, loadGroups]);

  useEffect(() => {
    loadActiveTabData();
  }, [loadActiveTabData]);

  const filteredMySets = useMemo(() => {
    const list = mySetsData?.content || [];
    const q = myFilter.trim().toLowerCase();
    if (!q) return list;
    return list.filter(
      (s) =>
        s.title?.toLowerCase().includes(q) ||
        s.description?.toLowerCase().includes(q) ||
        (s.tags || []).some((t) => t.toLowerCase().includes(q))
    );
  }, [mySetsData, myFilter]);

  const savedSetIdSet = useMemo(() => new Set(savedSets.map((item) => item.setId)), [savedSets]);

  const handleDeleteSet = async (set) => {
    if (!window.confirm(`Bạn có chắc muốn xóa bộ thẻ "${set.title}"?`)) return;
    try {
      await flashcardApi.deleteSet(set.id);
      showMessage('success', `Đã xóa bộ thẻ "${set.title}".`);
      await loadMySets(mySetsPage);
    } catch (err) {
      showMessage('error', extractApiErrorMessage(err, 'Không thể xóa bộ thẻ.'));
    }
  };

  const handleCloneSet = async (set) => {
    try {
      await flashcardApi.cloneSet(set.id);
      showMessage('success', `Đã nhân bản "${set.title}" vào bộ thẻ của bạn.`);
      await loadMySets(mySetsPage);
    } catch (err) {
      showMessage('error', extractApiErrorMessage(err, 'Không thể nhân bản bộ thẻ.'));
    }
  };

  const handleUnsaveSet = async (set) => {
    try {
      await flashcardApi.unsaveSet(set.id);
      setSavedSets((prev) => prev.filter((item) => item.setId !== set.id));
      showMessage('success', `Đã bỏ lưu "${set.title}".`);
    } catch (err) {
      showMessage('error', extractApiErrorMessage(err, 'Không thể bỏ lưu bộ thẻ.'));
    }
  };

  const handleDeleteFolder = async (folder) => {
    if (!window.confirm(`Bạn có chắc muốn xóa thư mục "${folder.name}"?`)) return;
    try {
      await folderApi.deleteFolder(folder.id);
      showMessage('success', `Đã xóa thư mục "${folder.name}".`);
      await loadFolders();
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
      <div className="max-w-6xl mx-auto px-4 sm:px-8 py-6 pb-20 space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-[#282E3E] dark:text-white tracking-tight">
              Thư viện của bạn
            </h1>
            <p className="mt-1 text-sm text-[#586380] dark:text-[#A8B1D1]">
              Quản lý bộ thẻ flashcard, danh sách đã lưu, thư mục học tập và nhóm học cùng bạn bè.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2.5 shrink-0">
            <Link
              to="/sets/create"
              className="inline-flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-[#4255FF] hover:bg-[#3545E0] text-white text-sm font-bold transition shadow-sm"
            >
              <Plus className="w-4 h-4" />
              Tạo bộ thẻ
            </Link>
            <button
              type="button"
              onClick={() => {
                setEditingFolder(null);
                setFolderModalOpen(true);
              }}
              className="inline-flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-[#F6F7FB] dark:bg-[#202140] border border-[#D9DDE8] dark:border-[#2E3856] hover:border-[#4255FF] text-sm font-bold text-[#282E3E] dark:text-white transition"
            >
              <Folder className="w-4 h-4 text-[#4255FF]" />
              Thư mục mới
            </button>
          </div>
        </div>

        {/* Quizlet-style Tabs */}
        <div className="border-b border-[#EDEFF4] dark:border-[#2E3856] flex items-center gap-1 overflow-x-auto">
          {TABS.map((tab) => {
            const Icon = tab.icon;
            const active = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                onClick={() => setActiveTab(tab.id)}
                className={`inline-flex items-center gap-2 px-4 py-3 text-sm font-bold border-b-2 whitespace-nowrap transition-colors ${
                  active
                    ? 'border-[#4255FF] text-[#4255FF] dark:text-white'
                    : 'border-transparent text-[#586380] hover:text-[#282E3E] dark:text-[#A8B1D1] dark:hover:text-white'
                }`}
              >
                <Icon className="w-4 h-4" />
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
            Đang tải dữ liệu...
          </div>
        ) : (
          <>
            {/* Tab 1: My Sets */}
            {activeTab === 'my' && (
              <div className="space-y-5">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div className="relative w-full sm:max-w-xs">
                    <Search className="w-4 h-4 text-[#586380] dark:text-[#A8B1D1] absolute left-3.5 top-1/2 -translate-y-1/2" />
                    <input
                      type="text"
                      value={myFilter}
                      onChange={(e) => setMyFilter(e.target.value)}
                      placeholder="Lọc nhanh bộ thẻ của bạn..."
                      className="w-full h-10 pl-9 pr-3 rounded-xl bg-[#F6F7FB] dark:bg-[#202140] border border-[#EDEFF4] dark:border-[#2E3856] text-sm font-medium text-[#282E3E] dark:text-white outline-none focus:border-[#4255FF]"
                    />
                  </div>
                  <p className="text-xs font-bold text-[#586380] dark:text-[#A8B1D1]">
                    Tổng cộng: {mySetsData?.totalElements ?? 0} bộ thẻ
                  </p>
                </div>

                {filteredMySets.length === 0 ? (
                  <div className="rounded-2xl border border-dashed border-[#D9DDE8] dark:border-[#2E3856] p-12 text-center space-y-3">
                    <p className="text-base font-bold text-[#282E3E] dark:text-white">
                      {myFilter ? 'Không có bộ thẻ nào khớp từ khóa lọc' : 'Bạn chưa tạo bộ thẻ nào'}
                    </p>
                    <p className="text-sm text-[#586380] dark:text-[#A8B1D1]">
                      Bắt đầu tạo bộ thẻ đầu tiên hoặc khám phá bộ thẻ từ cộng đồng.
                    </p>
                    <div className="pt-1 flex items-center justify-center gap-3">
                      <Link
                        to="/sets/create"
                        className="px-5 py-2.5 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white text-sm font-bold transition"
                      >
                        Tạo bộ thẻ ngay
                      </Link>
                      <Link
                        to="/explore"
                        className="px-5 py-2.5 rounded-full border border-[#D9DDE8] dark:border-[#2E3856] text-sm font-bold text-[#282E3E] dark:text-white hover:bg-[#F6F7FB] dark:hover:bg-[#202140] transition"
                      >
                        Khám phá cộng đồng
                      </Link>
                    </div>
                  </div>
                ) : (
                  <>
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                      {filteredMySets.map((set) => (
                        <FlashcardSetCard
                          key={set.id}
                          set={set}
                          showVisibility
                          showEdit
                          onClone={handleCloneSet}
                          onDelete={handleDeleteSet}
                        />
                      ))}
                    </div>

                    {mySetsData && mySetsData.totalPages > 1 && (
                      <div className="pt-2 flex items-center justify-center gap-3">
                        <button
                          type="button"
                          disabled={mySetsData.first}
                          onClick={() => setMySetsPage((p) => Math.max(0, p - 1))}
                          className="inline-flex items-center gap-1 px-3.5 py-2 rounded-xl border border-[#D9DDE8] dark:border-[#2E3856] text-sm font-bold text-[#282E3E] dark:text-white disabled:opacity-40"
                        >
                          <ChevronLeft className="w-4 h-4" />
                          Trước
                        </button>
                        <span className="text-sm font-semibold text-[#586380] dark:text-[#A8B1D1]">
                          Trang {mySetsData.page + 1} / {mySetsData.totalPages}
                        </span>
                        <button
                          type="button"
                          disabled={mySetsData.last}
                          onClick={() => setMySetsPage((p) => p + 1)}
                          className="inline-flex items-center gap-1 px-3.5 py-2 rounded-xl border border-[#D9DDE8] dark:border-[#2E3856] text-sm font-bold text-[#282E3E] dark:text-white disabled:opacity-40"
                        >
                          Sau
                          <ChevronRight className="w-4 h-4" />
                        </button>
                      </div>
                    )}
                  </>
                )}
              </div>
            )}

            {/* Tab 2: Saved & Recents */}
            {activeTab === 'saved' && (
              <div className="space-y-10">
                <section className="space-y-4">
                  <h2 className="text-lg font-extrabold text-[#282E3E] dark:text-white flex items-center gap-2">
                    <Clock className="w-5 h-5 text-[#4255FF]" />
                    Học gần đây ({recentSets.length})
                  </h2>
                  {recentSets.length === 0 ? (
                    <div className="rounded-2xl border border-dashed border-[#D9DDE8] dark:border-[#2E3856] p-8 text-center text-sm text-[#586380] dark:text-[#A8B1D1]">
                      Bạn chưa có lịch sử học gần đây nào.
                    </div>
                  ) : (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                      {recentSets.map((item) => (
                        <FlashcardSetCard
                          key={`recent-${item.setId}`}
                          set={item.set}
                          isSaved={savedSetIdSet.has(item.setId)}
                          extraMeta={
                            item.lastStudyMode && (
                              <span className="inline-flex items-center px-2 py-0.5 rounded-md text-[11px] font-semibold bg-[#EDEFFF] text-[#4255FF] dark:bg-[#2E3856] dark:text-[#9AA7FF]">
                                {item.lastStudyMode}
                                {item.lastAccessedAt ? ` • ${formatShortDate(item.lastAccessedAt)}` : ''}
                              </span>
                            )
                          }
                        />
                      ))}
                    </div>
                  )}
                </section>

                <section className="space-y-4">
                  <h2 className="text-lg font-extrabold text-[#282E3E] dark:text-white flex items-center gap-2">
                    <Bookmark className="w-5 h-5 text-[#4255FF]" />
                    Bộ thẻ đã lưu ({savedSets.length})
                  </h2>
                  {savedSets.length === 0 ? (
                    <div className="rounded-2xl border border-dashed border-[#D9DDE8] dark:border-[#2E3856] p-8 text-center space-y-2">
                      <p className="text-sm text-[#586380] dark:text-[#A8B1D1]">
                        Chưa có bộ thẻ nào được lưu.
                      </p>
                      <Link
                        to="/explore"
                        className="inline-block text-sm font-bold text-[#4255FF] hover:underline"
                      >
                        Khám phá bộ thẻ công khai →
                      </Link>
                    </div>
                  ) : (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                      {savedSets.map((item) => (
                        <FlashcardSetCard
                          key={`saved-${item.setId}`}
                          set={item.set}
                          isSaved
                          onToggleSave={handleUnsaveSet}
                        />
                      ))}
                    </div>
                  )}
                </section>
              </div>
            )}

            {/* Tab 3: Folders */}
            {activeTab === 'folders' && (
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <p className="text-sm font-bold text-[#586380] dark:text-[#A8B1D1]">
                    {folders.length} thư mục của bạn
                  </p>
                  <button
                    type="button"
                    onClick={() => {
                      setEditingFolder(null);
                      setFolderModalOpen(true);
                    }}
                    className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-[#4255FF] hover:bg-[#3545E0] text-white text-sm font-bold transition"
                  >
                    <Plus className="w-4 h-4" />
                    Tạo thư mục
                  </button>
                </div>

                {folders.length === 0 ? (
                  <div className="rounded-2xl border border-dashed border-[#D9DDE8] dark:border-[#2E3856] p-12 text-center space-y-2">
                    <p className="text-base font-bold text-[#282E3E] dark:text-white">
                      Chưa có thư mục nào
                    </p>
                    <p className="text-sm text-[#586380] dark:text-[#A8B1D1]">
                      Gom các bộ thẻ cùng môn học vào thư mục để quản lý và chia sẻ dễ dàng hơn.
                    </p>
                  </div>
                ) : (
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                    {folders.map((folder) => (
                      <div
                        key={folder.id}
                        onClick={() => setDetailFolderId(folder.id)}
                        className="cursor-pointer bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] hover:border-[#4255FF] p-5 flex flex-col justify-between transition shadow-sm hover:shadow-md"
                      >
                        <div>
                          <div className="flex items-center justify-between gap-2 mb-2">
                            <span className="inline-flex items-center gap-1.5 text-xs font-bold text-[#4255FF] dark:text-[#9AA7FF]">
                              <Folder className="w-4 h-4" />
                              {folder.setCount} bộ thẻ
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

            {/* Tab 4: Study Groups */}
            {activeTab === 'groups' && (
              <div className="space-y-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <p className="text-sm font-bold text-[#586380] dark:text-[#A8B1D1]">
                    {groups.length} nhóm học bạn đang tham gia
                  </p>
                  <div className="flex items-center gap-2.5">
                    <button
                      type="button"
                      onClick={() => setJoinGroupOpen(true)}
                      className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-[#F6F7FB] dark:bg-[#202140] border border-[#D9DDE8] dark:border-[#2E3856] hover:border-[#4255FF] text-sm font-bold text-[#282E3E] dark:text-white transition"
                    >
                      <UserPlus className="w-4 h-4 text-[#4255FF]" />
                      Nhập mã mời
                    </button>
                    <button
                      type="button"
                      onClick={() => setCreateGroupOpen(true)}
                      className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-[#4255FF] hover:bg-[#3545E0] text-white text-sm font-bold transition"
                    >
                      <Plus className="w-4 h-4" />
                      Tạo nhóm học
                    </button>
                  </div>
                </div>

                {groups.length === 0 ? (
                  <div className="rounded-2xl border border-dashed border-[#D9DDE8] dark:border-[#2E3856] p-12 text-center space-y-2">
                    <p className="text-base font-bold text-[#282E3E] dark:text-white">
                      Bạn chưa tham gia nhóm học nào
                    </p>
                    <p className="text-sm text-[#586380] dark:text-[#A8B1D1]">
                      Tạo nhóm mới để nhận mã mời hoặc nhập mã mời từ bạn bè để cùng chia sẻ bộ thẻ.
                    </p>
                  </div>
                ) : (
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                    {groups.map((g) => (
                      <div
                        key={g.id}
                        onClick={() => setDetailGroupId(g.id)}
                        className="cursor-pointer bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] hover:border-[#4255FF] p-5 flex flex-col justify-between transition shadow-sm hover:shadow-md"
                      >
                        <div>
                          <div className="flex items-center justify-between gap-2 mb-2">
                            <span className="inline-flex items-center gap-1 text-xs font-bold text-[#4255FF] dark:text-[#9AA7FF]">
                              <Users className="w-4 h-4" />
                              {g.memberCount} thành viên • {g.setCount} bộ thẻ
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
                            className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-[#F6F7FB] dark:bg-[#171836] text-xs font-bold text-[#282E3E] dark:text-white hover:text-[#4255FF]"
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
          loadFolders();
        }}
      />

      <FolderDetailModal
        folderId={detailFolderId}
        mySets={mySetsData?.content || []}
        onClose={() => setDetailFolderId(null)}
        onChanged={loadFolders}
      />

      {/* Study Group Modals */}
      <CreateGroupModal
        open={createGroupOpen}
        onClose={() => setCreateGroupOpen(false)}
        onCreated={(created) => {
          showMessage('success', `Đã tạo nhóm "${created?.name || ''}" (Mã mời: ${created?.inviteCode || ''}).`);
          loadGroups();
        }}
      />

      <JoinGroupModal
        open={joinGroupOpen}
        onClose={() => setJoinGroupOpen(false)}
        onJoined={(joined) => {
          showMessage('success', `Đã tham gia nhóm "${joined?.name || ''}".`);
          loadGroups();
        }}
      />

      <GroupDetailModal
        groupId={detailGroupId}
        mySets={mySetsData?.content || []}
        onClose={() => setDetailGroupId(null)}
        onChanged={loadGroups}
      />
    </MainLayout>
  );
}

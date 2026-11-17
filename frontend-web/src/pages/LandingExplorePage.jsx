import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import {
  SlidersHorizontal,
  X,
  ChevronLeft,
  ChevronRight,
  Loader2,
  Users,
  Layers,
  BadgeCheck,
} from 'lucide-react';
import MainLayout from '../components/layout/MainLayout';
import FlashcardSetCard from '../components/library/FlashcardSetCard';
import SetPreviewModal from '../components/library/SetPreviewModal';
import UserAvatar from '../components/common/UserAvatar';
import flashcardApi from '../api/flashcardApi';
import { useAuthStore } from '../store/useAuthStore';
import { extractApiErrorMessage } from '../utils/userHelpers';
import {
  MOCK_CATEGORIES,
  MOCK_EXPLORE_SETS,
  MOCK_GROUPS_PREVIEW,
  MOCK_QUESTIONS_PREVIEW,
  MOCK_STUDY_GUIDES,
  MOCK_USERS_PREVIEW,
} from '../data/libraryExploreMockData';

const EXPLORE_TABS = [
  { id: 'all', label: 'Tất cả kết quả' },
  { id: 'sets', label: 'Học phần' },
  { id: 'tests', label: 'Bài kiểm tra thử' },
  { id: 'guides', label: 'Hướng dẫn học' },
  { id: 'questions', label: 'Câu hỏi' },
  { id: 'users', label: 'Người dùng' },
  { id: 'classes', label: 'Lớp học' },
];

const SORT_OPTIONS = [
  { value: 'newest', label: 'Mới nhất' },
  { value: 'popular', label: 'Phổ biến nhất' },
  { value: 'rating', label: 'Đánh giá cao' },
];

const QUIZLET_CARD_CLASS =
  "relative overflow-hidden bg-white dark:bg-[#202140] rounded-2xl border-2 border-[#EDEFF4] dark:border-[#2E3856] p-5 transition-all shadow-[0_1px_4px_rgba(40,46,62,0.04)] after:content-[''] after:absolute after:inset-x-0 after:bottom-0 after:h-[4px] after:bg-[#A8B1FF] after:opacity-0 hover:after:opacity-100 after:transition-opacity";

export default function LandingExplorePage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { isAuthenticated } = useAuthStore();

  const urlQuery = searchParams.get('q') || '';
  const urlCategoryId = searchParams.get('categoryId') || '';
  const urlSort = searchParams.get('sort') || 'newest';
  const urlTab = searchParams.get('tab') || (searchParams.get('mode') === 'test' ? 'tests' : 'all');
  const urlPage = Math.max(0, Number(searchParams.get('page') || 0) || 0);

  const activeTab = EXPLORE_TABS.some((t) => t.id === urlTab) ? urlTab : 'all';

  const [exploreData, setExploreData] = useState({
    featuredSets: [],
    popularSets: [],
    recentSets: [],
    categories: MOCK_CATEGORIES,
  });
  const [searchResult, setSearchResult] = useState(null);
  const [loading, setLoading] = useState(true);

  const [savedIds, setSavedIds] = useState(() => new Set());
  const [savingId, setSavingId] = useState(null);
  const [previewSet, setPreviewSet] = useState(null);
  const [errorMsg, setErrorMsg] = useState('');

  const isFiltering = useMemo(
    () => Boolean(urlQuery.trim() || urlCategoryId || urlSort !== 'newest' || urlPage > 0),
    [urlQuery, urlCategoryId, urlSort, urlPage]
  );

  useEffect(() => {
    let active = true;
    const loadData = async () => {
      setLoading(true);
      setErrorMsg('');
      try {
        if (isFiltering) {
          const [searchRes, expRes] = await Promise.all([
            flashcardApi.search({
              keyword: urlQuery,
              categoryId: urlCategoryId || undefined,
              sort: urlSort,
              page: urlPage,
              size: 12,
            }),
            flashcardApi.explore().catch(() => null),
          ]);
          if (!active) return;
          const apiPage = searchRes?.data;
          const cats = expRes?.data?.categories;
          if (cats?.length) {
            setExploreData((prev) => ({ ...prev, categories: cats }));
          }
          if (apiPage && apiPage.content?.length > 0) {
            setSearchResult(apiPage);
          } else {
            const q = urlQuery.trim().toLowerCase();
            const filtered = MOCK_EXPLORE_SETS.filter((s) => {
              const matchesQ =
                !q ||
                s.title.toLowerCase().includes(q) ||
                s.description.toLowerCase().includes(q);
              const matchesCat =
                !urlCategoryId || String(s.category?.id) === String(urlCategoryId);
              return matchesQ && matchesCat;
            });
            setSearchResult({
              content: filtered,
              page: 0,
              totalPages: 1,
              totalElements: filtered.length,
              first: true,
              last: true,
            });
          }
        } else {
          const expRes = await flashcardApi.explore();
          if (!active) return;
          const d = expRes?.data;
          const hasRealSets =
            (d?.featuredSets?.length || 0) +
              (d?.popularSets?.length || 0) +
              (d?.recentSets?.length || 0) >
            0;
          if (hasRealSets) {
            setExploreData({
              featuredSets: d.featuredSets || [],
              popularSets: d.popularSets || [],
              recentSets: d.recentSets || [],
              categories: d.categories?.length ? d.categories : MOCK_CATEGORIES,
            });
          } else {
            setExploreData({
              featuredSets: MOCK_EXPLORE_SETS.slice(0, 3),
              popularSets: MOCK_EXPLORE_SETS,
              recentSets: [...MOCK_EXPLORE_SETS].reverse(),
              categories: d?.categories?.length ? d.categories : MOCK_CATEGORIES,
            });
          }
          setSearchResult(null);
        }
      } catch {
        if (!active) return;
        const q = urlQuery.trim().toLowerCase();
        const filtered = MOCK_EXPLORE_SETS.filter((s) => {
          const matchesQ =
            !q ||
            s.title.toLowerCase().includes(q) ||
            s.description.toLowerCase().includes(q);
          const matchesCat =
            !urlCategoryId || String(s.category?.id) === String(urlCategoryId);
          return matchesQ && matchesCat;
        });
        setSearchResult(
          isFiltering
            ? {
                content: filtered,
                page: 0,
                totalPages: 1,
                totalElements: filtered.length,
                first: true,
                last: true,
              }
            : null
        );
        setExploreData({
          featuredSets: MOCK_EXPLORE_SETS.slice(0, 3),
          popularSets: MOCK_EXPLORE_SETS,
          recentSets: [...MOCK_EXPLORE_SETS].reverse(),
          categories: MOCK_CATEGORIES,
        });
      } finally {
        if (active) setLoading(false);
      }
    };

    loadData();
    return () => {
      active = false;
    };
  }, [isFiltering, urlQuery, urlCategoryId, urlSort, urlPage]);

  useEffect(() => {
    if (!isAuthenticated) {
      setSavedIds(new Set());
      return;
    }
    let active = true;
    flashcardApi
      .getSavedSets({ page: 0, size: 100 })
      .then((res) => {
        if (!active) return;
        const items = res?.data?.content || [];
        setSavedIds(new Set(items.map((item) => item.setId)));
      })
      .catch(() => {});
    return () => {
      active = false;
    };
  }, [isAuthenticated]);

  const updateParams = useCallback(
    (updates) => {
      const next = new URLSearchParams(searchParams);
      Object.entries(updates).forEach(([key, val]) => {
        if (val === undefined || val === null || val === '' || (key === 'page' && Number(val) === 0)) {
          next.delete(key);
        } else {
          next.set(key, String(val));
        }
      });
      setSearchParams(next);
    },
    [searchParams, setSearchParams]
  );

  const handleToggleSave = async (set) => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    const currentlySaved = savedIds.has(set.id);
    setSavingId(set.id);
    try {
      if (currentlySaved) {
        await flashcardApi.unsaveSet(set.id);
        setSavedIds((prev) => {
          const next = new Set(prev);
          next.delete(set.id);
          return next;
        });
      } else {
        await flashcardApi.saveSet(set.id);
        setSavedIds((prev) => {
          const next = new Set(prev);
          next.add(set.id);
          return next;
        });
      }
    } catch (err) {
      setErrorMsg(extractApiErrorMessage(err, 'Không thể cập nhật trạng thái lưu học phần.'));
    } finally {
      setSavingId(null);
    }
  };

  const displayedSets = useMemo(() => {
    if (isFiltering) return searchResult?.content || [];
    const merged = [
      ...exploreData.featuredSets,
      ...exploreData.popularSets,
      ...exploreData.recentSets,
    ];
    const seen = new Set();
    return merged.filter((s) => {
      if (seen.has(s.id)) return false;
      seen.add(s.id);
      return true;
    });
  }, [isFiltering, searchResult, exploreData]);

  const filteredUsers = useMemo(() => {
    const q = urlQuery.trim().toLowerCase();
    if (!q) return MOCK_USERS_PREVIEW;
    const matched = MOCK_USERS_PREVIEW.filter((u) => u.username.toLowerCase().includes(q));
    return matched.length > 0 ? matched : MOCK_USERS_PREVIEW;
  }, [urlQuery]);

  const filteredClasses = useMemo(() => {
    const q = urlQuery.trim().toLowerCase();
    if (!q) return MOCK_GROUPS_PREVIEW;
    const matched = MOCK_GROUPS_PREVIEW.filter((g) => g.name.toLowerCase().includes(q));
    return matched.length > 0 ? matched : MOCK_GROUPS_PREVIEW;
  }, [urlQuery]);

  return (
    <MainLayout>
      <div className="max-w-[1360px] mx-auto px-4 sm:px-8 pt-4 pb-24 space-y-4">
        {/* Compact Quizlet-style Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <h1 className="text-[18px] sm:text-[19px] font-bold text-[#282E3E] dark:text-white tracking-tight">
            {urlQuery.trim() ? `Kết quả cho "${urlQuery.trim()}"` : 'Khám phá học phần'}
          </h1>

          {!isAuthenticated && (
            <div className="flex items-center gap-2 shrink-0">
              <Link
                to="/login"
                className="px-4 py-1.5 rounded-full text-xs font-bold text-[#282E3E] dark:text-white bg-[#F6F7FB] dark:bg-[#202140] hover:bg-[#EDEFF4] transition"
              >
                Đăng nhập
              </Link>
              <Link
                to="/register"
                className="px-4 py-1.5 rounded-full text-xs font-bold text-white bg-[#4255FF] hover:bg-[#3545E0] transition"
              >
                Đăng ký
              </Link>
            </div>
          )}
        </div>

        {/* Quizlet Underline Tab Bar (shifted up closer to heading) */}
        <div className="border-b border-[#EDEFF4] dark:border-[#2E3856] flex items-center gap-6 overflow-x-auto">
          {EXPLORE_TABS.map((tab) => {
            const active = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                onClick={() => updateParams({ tab: tab.id === 'all' ? '' : tab.id, page: 0 })}
                className={`pb-2.5 text-[13.5px] font-bold whitespace-nowrap border-b-[2.5px] transition-colors ${
                  active
                    ? 'border-[#4255FF] text-[#282E3E] dark:text-white'
                    : 'border-transparent text-[#586380] hover:text-[#282E3E] dark:text-[#A8B1D1] dark:hover:text-white'
                }`}
              >
                {tab.label}
              </button>
            );
          })}
        </div>

        {/* Category Pills + Sort Filter Row (only when filtering in sets/tests) */}
        {(activeTab === 'sets' || activeTab === 'tests') && (
          <div className="flex flex-wrap items-center justify-between gap-3 pt-1">
            <div className="flex flex-wrap items-center gap-2">
              <button
                type="button"
                onClick={() => updateParams({ categoryId: '', page: 0 })}
                className={`px-3.5 py-1.5 rounded-full text-xs font-bold transition ${
                  !urlCategoryId
                    ? 'bg-[#282E3E] text-white dark:bg-white dark:text-[#0A082D]'
                    : 'bg-[#F6F7FB] dark:bg-[#202140] text-[#586380] dark:text-[#A8B1D1] hover:bg-[#EDEFF4]'
                }`}
              >
                Tất cả
              </button>
              {exploreData.categories.map((cat) => {
                const active = String(cat.id) === String(urlCategoryId);
                return (
                  <button
                    key={cat.id}
                    type="button"
                    onClick={() => updateParams({ categoryId: active ? '' : cat.id, page: 0 })}
                    className={`px-3.5 py-1.5 rounded-full text-xs font-bold transition ${
                      active
                        ? 'bg-[#282E3E] text-white dark:bg-white dark:text-[#0A082D]'
                        : 'bg-[#F6F7FB] dark:bg-[#202140] text-[#586380] dark:text-[#A8B1D1] hover:bg-[#EDEFF4]'
                    }`}
                  >
                    {cat.name}
                  </button>
                );
              })}
            </div>

            <div className="relative">
              <SlidersHorizontal className="w-3.5 h-3.5 text-[#586380] dark:text-[#A8B1D1] absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
              <select
                value={urlSort}
                onChange={(e) => updateParams({ sort: e.target.value, page: 0 })}
                aria-label="Sắp xếp học phần"
                className="h-9 pl-8 pr-3 rounded-full bg-[#F6F7FB] dark:bg-[#202140] border border-[#EDEFF4] dark:border-[#2E3856] text-xs font-bold text-[#282E3E] dark:text-white outline-none focus:border-[#4255FF]"
              >
                {SORT_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>
          </div>
        )}

        {errorMsg && (
          <div className="rounded-xl bg-red-50 dark:bg-[#431F25] px-4 py-3 text-sm font-medium text-red-700 dark:text-red-300 flex items-center justify-between">
            <span>{errorMsg}</span>
            <button type="button" onClick={() => setErrorMsg('')}>
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

        {loading ? (
          <div className="py-16 flex items-center justify-center gap-2 text-sm font-semibold text-[#586380] dark:text-[#A8B1D1]">
            <Loader2 className="w-5 h-5 animate-spin text-[#4255FF]" />
            Đang tải học phần...
          </div>
        ) : (
          <div className="space-y-9 pt-1">
            {/* 1. Section: Học phần */}
            {(activeTab === 'all' || activeTab === 'sets') && (
              <section className="space-y-3.5">
                <div className="flex items-center justify-between">
                  <h2 className="text-[16px] font-bold text-[#282E3E] dark:text-white">
                    Học phần
                  </h2>
                  {activeTab === 'all' && displayedSets.length > 0 && (
                    <button
                      type="button"
                      onClick={() => updateParams({ tab: 'sets' })}
                      className="text-[13.5px] font-bold text-[#4255FF] hover:underline"
                    >
                      Xem tất cả
                    </button>
                  )}
                </div>

                {displayedSets.length === 0 ? (
                  <div className="rounded-2xl border border-dashed border-[#D9DDE8] dark:border-[#2E3856] p-10 text-center text-sm text-[#586380] dark:text-[#A8B1D1]">
                    Không tìm thấy học phần nào phù hợp.
                  </div>
                ) : (
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                    {(activeTab === 'all' ? displayedSets.slice(0, 6) : displayedSets).map((set) => (
                      <FlashcardSetCard
                        key={set.id}
                        set={set}
                        isSaved={savedIds.has(set.id)}
                        saving={savingId === set.id}
                        onToggleSave={handleToggleSave}
                        onPreview={setPreviewSet}
                      />
                    ))}
                  </div>
                )}

                {activeTab === 'sets' && searchResult && searchResult.totalPages > 1 && (
                  <div className="pt-3 flex items-center justify-center gap-3">
                    <button
                      type="button"
                      disabled={searchResult.first}
                      onClick={() => updateParams({ page: urlPage - 1 })}
                      className="inline-flex items-center gap-1 px-3.5 py-2 rounded-full border border-[#D9DDE8] dark:border-[#2E3856] text-xs font-bold disabled:opacity-40"
                    >
                      <ChevronLeft className="w-4 h-4" />
                      Trước
                    </button>
                    <span className="text-xs font-bold text-[#586380] dark:text-[#A8B1D1]">
                      Trang {searchResult.page + 1} / {searchResult.totalPages}
                    </span>
                    <button
                      type="button"
                      disabled={searchResult.last}
                      onClick={() => updateParams({ page: urlPage + 1 })}
                      className="inline-flex items-center gap-1 px-3.5 py-2 rounded-full border border-[#D9DDE8] dark:border-[#2E3856] text-xs font-bold disabled:opacity-40"
                    >
                      Sau
                      <ChevronRight className="w-4 h-4" />
                    </button>
                  </div>
                )}
              </section>
            )}

            {/* 2. Section: Hướng dẫn học */}
            {(activeTab === 'all' || activeTab === 'guides') && (
              <section className="space-y-3.5">
                <div className="flex items-center justify-between">
                  <h2 className="text-[16px] font-bold text-[#282E3E] dark:text-white">
                    Hướng dẫn học
                  </h2>
                  {activeTab === 'all' && (
                    <button
                      type="button"
                      onClick={() => updateParams({ tab: 'guides' })}
                      className="text-[13.5px] font-bold text-[#4255FF] hover:underline"
                    >
                      Xem tất cả
                    </button>
                  )}
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                  {MOCK_STUDY_GUIDES.map((guide) => (
                    <Link
                      key={guide.id}
                      to={`/sets/${guide.setId}`}
                      className={`${QUIZLET_CARD_CLASS} flex flex-col justify-between min-h-[160px]`}
                    >
                      <div>
                        <h3 className="text-[15px] font-bold text-[#282E3E] dark:text-white line-clamp-1">
                          {guide.title}
                        </h3>
                        <div className="mt-2">
                          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-[#E0F2FE] text-[#0369A1] dark:bg-sky-950/50 dark:text-sky-300">
                            Hướng dẫn học
                          </span>
                        </div>
                      </div>
                      <div className="mt-5 flex items-center gap-2 min-w-0">
                        <UserAvatar username={guide.author} fullName={guide.author} size="xs" />
                        <span className="text-[13px] font-semibold text-[#282E3E] dark:text-[#E2E8F0] truncate">
                          {guide.author}
                        </span>
                      </div>
                    </Link>
                  ))}
                </div>
              </section>
            )}

            {/* 3. Section: Bài kiểm tra thử */}
            {(activeTab === 'all' || activeTab === 'tests') && (
              <section className="space-y-3.5">
                <div className="flex items-center justify-between">
                  <h2 className="text-[16px] font-bold text-[#282E3E] dark:text-white">
                    Bài kiểm tra thử
                  </h2>
                  {activeTab === 'all' && (
                    <button
                      type="button"
                      onClick={() => updateParams({ tab: 'tests' })}
                      className="text-[13.5px] font-bold text-[#4255FF] hover:underline"
                    >
                      Xem tất cả
                    </button>
                  )}
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                  {displayedSets.slice(0, activeTab === 'all' ? 3 : 6).map((set) => (
                    <Link
                      key={`test-${set.id}`}
                      to={`/sets/${set.id}`}
                      className={`${QUIZLET_CARD_CLASS} flex flex-col justify-between min-h-[160px]`}
                    >
                      <div>
                        <h3 className="text-[15px] font-bold text-[#282E3E] dark:text-white truncate">
                          {set.title}
                        </h3>
                        <div className="mt-2">
                          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-[#F3E8FF] text-[#6B21A8] dark:bg-purple-950/50 dark:text-purple-300">
                            Bài kiểm tra thử
                          </span>
                        </div>
                      </div>
                      <div className="mt-5 flex items-center gap-2 min-w-0">
                        <UserAvatar
                          src={set.owner?.avatarUrl}
                          fullName={set.owner?.fullName}
                          username={set.owner?.username}
                          size="xs"
                        />
                        <span className="text-[13px] font-semibold text-[#282E3E] dark:text-[#E2E8F0] truncate">
                          {set.owner?.username || 'Ẩn danh'}
                        </span>
                        {set.owner?.verifiedTeacher && (
                          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-bold bg-[#EDEFFF]/80 text-[#586380] dark:bg-[#2E3856] dark:text-[#A8B1D1] shrink-0">
                            Giáo viên
                          </span>
                        )}
                      </div>
                    </Link>
                  ))}
                </div>
              </section>
            )}

            {/* 4. Section: Câu hỏi */}
            {(activeTab === 'all' || activeTab === 'questions') && (
              <section className="space-y-3.5">
                <div className="flex items-center justify-between">
                  <h2 className="text-[16px] font-bold text-[#282E3E] dark:text-white">
                    Câu hỏi
                  </h2>
                  {activeTab === 'all' && (
                    <button
                      type="button"
                      onClick={() => updateParams({ tab: 'questions' })}
                      className="text-[13.5px] font-bold text-[#4255FF] hover:underline"
                    >
                      Xem tất cả
                    </button>
                  )}
                </div>

                <div className={`grid gap-4 ${activeTab === 'questions' ? 'grid-cols-1' : 'grid-cols-1 md:grid-cols-2'}`}>
                  {(activeTab === 'all' ? MOCK_QUESTIONS_PREVIEW.slice(0, 2) : MOCK_QUESTIONS_PREVIEW).map((q) => (
                    <Link
                      key={q.id}
                      to={`/sets/${q.setId}`}
                      className={`${QUIZLET_CARD_CLASS} flex flex-col justify-between min-h-[144px]`}
                    >
                      <div>
                        <p className="text-[12px] font-bold text-[#586380] dark:text-[#A8B1D1]">
                          {q.subject}
                        </p>
                        <p className="mt-1.5 text-[13px] leading-snug font-medium text-[#282E3E] dark:text-white line-clamp-2">
                          {q.question}
                        </p>
                      </div>
                      <div className="mt-4">
                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold bg-[#EDEFFF] text-[#282E3E] dark:bg-[#2E3856] dark:text-[#E2E8F0]">
                          <BadgeCheck className="w-3.5 h-3.5 text-[#4255FF]" />
                          Câu trả lời đã xác minh
                        </span>
                      </div>
                    </Link>
                  ))}
                </div>
              </section>
            )}

            {/* 5. Section: Người dùng */}
            {(activeTab === 'all' || activeTab === 'users') && (
              <section className="space-y-3.5">
                <div className="flex items-center justify-between">
                  <h2 className="text-[16px] font-bold text-[#282E3E] dark:text-white">
                    Người dùng
                  </h2>
                  {activeTab === 'all' && (
                    <button
                      type="button"
                      onClick={() => updateParams({ tab: 'users' })}
                      className="text-[13.5px] font-bold text-[#4255FF] hover:underline"
                    >
                      Xem tất cả
                    </button>
                  )}
                </div>

                <div className={`grid gap-4 ${activeTab === 'users' ? 'grid-cols-1 sm:grid-cols-2 lg:grid-cols-3' : 'grid-cols-1 md:grid-cols-2'}`}>
                  {filteredUsers.slice(0, activeTab === 'all' ? 2 : 6).map((u) => (
                    <Link
                      key={u.id}
                      to={`/users/${u.username}`}
                      className={`${QUIZLET_CARD_CLASS} flex flex-col gap-3.5`}
                    >
                      <UserAvatar
                        src={u.avatarUrl}
                        fullName={u.fullName}
                        username={u.username}
                        size="md"
                        className="!w-14 !h-14 !text-lg"
                      />
                      <div>
                        <p className="text-[15px] font-bold text-[#282E3E] dark:text-white truncate">
                          {u.username}
                        </p>
                        <div className="mt-2.5 flex flex-wrap items-center gap-2">
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[12px] font-bold bg-[#EDEFFF] text-[#282E3E] dark:bg-[#2E3856] dark:text-[#E2E8F0]">
                            <Layers className="w-3 h-3 text-[#586380] dark:text-[#A8B1D1]" />
                            {u.setCount} học phần
                          </span>
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[12px] font-bold bg-[#EDEFFF] text-[#282E3E] dark:bg-[#2E3856] dark:text-[#E2E8F0]">
                            <Users className="w-3 h-3 text-[#586380] dark:text-[#A8B1D1]" />
                            {u.classCount} lớp học
                          </span>
                        </div>
                      </div>
                    </Link>
                  ))}
                </div>
              </section>
            )}

            {/* 6. Section: Lớp học */}
            {(activeTab === 'all' || activeTab === 'classes') && (
              <section className="space-y-3.5">
                <div className="flex items-center justify-between">
                  <h2 className="text-[16px] font-bold text-[#282E3E] dark:text-white">
                    Lớp học
                  </h2>
                  {activeTab === 'all' && (
                    <button
                      type="button"
                      onClick={() => updateParams({ tab: 'classes' })}
                      className="text-[13.5px] font-bold text-[#4255FF] hover:underline"
                    >
                      Xem tất cả
                    </button>
                  )}
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {filteredClasses.map((g) => (
                    <Link
                      key={g.id}
                      to={isAuthenticated ? '/library?tab=groups' : '/login'}
                      className={`${QUIZLET_CARD_CLASS} flex flex-col justify-between min-h-[154px] gap-6`}
                    >
                      <div className="flex items-start justify-between gap-3">
                        <h3 className="text-[15px] font-extrabold text-[#282E3E] dark:text-white uppercase tracking-tight">
                          {g.name}
                        </h3>
                        <Users className="w-7 h-7 text-[#38BDF8] shrink-0" />
                      </div>
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[12px] font-bold bg-[#EDEFFF] text-[#282E3E] dark:bg-[#2E3856] dark:text-[#E2E8F0]">
                          <Layers className="w-3 h-3 text-[#586380] dark:text-[#A8B1D1]" />
                          {g.setCount} học phần
                        </span>
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[12px] font-bold bg-[#EDEFFF] text-[#282E3E] dark:bg-[#2E3856] dark:text-[#E2E8F0]">
                          <Users className="w-3 h-3 text-[#586380] dark:text-[#A8B1D1]" />
                          {g.memberCount} thành viên
                        </span>
                      </div>
                    </Link>
                  ))}
                </div>
              </section>
            )}
          </div>
        )}
      </div>

      <SetPreviewModal set={previewSet} onClose={() => setPreviewSet(null)} />
    </MainLayout>
  );
}

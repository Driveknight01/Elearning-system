import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import {
  Search,
  Sparkles,
  TrendingUp,
  Clock,
  SlidersHorizontal,
  X,
  ChevronLeft,
  ChevronRight,
  Compass,
  Loader2,
  BookOpen,
} from 'lucide-react';
import MainLayout from '../components/layout/MainLayout';
import FlashcardSetCard from '../components/library/FlashcardSetCard';
import flashcardApi from '../api/flashcardApi';
import { useAuthStore } from '../store/useAuthStore';
import { extractApiErrorMessage } from '../utils/userHelpers';

const SORT_OPTIONS = [
  { value: 'newest', label: 'Mới nhất' },
  { value: 'popular', label: 'Phổ biến nhất' },
  { value: 'rating', label: 'Đánh giá cao' },
];

const MODE_BANNERS = {
  test: 'Chọn một bộ thẻ công khai bên dưới để bắt đầu làm Bài thi thử.',
  flashcards: 'Chọn bộ thẻ để luyện tập với Thẻ ghi nhớ thông minh FSRS.',
  games: 'Chọn bộ thẻ để thử sức với các Minigames Ghép thẻ, Blast, Đá quý & Khối hộp.',
};

export default function LandingExplorePage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { isAuthenticated } = useAuthStore();

  const urlQuery = searchParams.get('q') || '';
  const urlCategoryId = searchParams.get('categoryId') || '';
  const urlSort = searchParams.get('sort') || 'newest';
  const urlMode = searchParams.get('mode') || '';
  const urlPage = Math.max(0, Number(searchParams.get('page') || 0) || 0);

  const [keywordInput, setKeywordInput] = useState(urlQuery);
  const [exploreData, setExploreData] = useState({
    featuredSets: [],
    popularSets: [],
    recentSets: [],
    categories: [],
  });
  const [exploreLoading, setExploreLoading] = useState(true);

  const [searchResult, setSearchResult] = useState(null);
  const [searchLoading, setSearchLoading] = useState(false);

  const [savedIds, setSavedIds] = useState(() => new Set());
  const [savingId, setSavingId] = useState(null);
  const [errorMsg, setErrorMsg] = useState('');

  useEffect(() => {
    setKeywordInput(urlQuery);
  }, [urlQuery]);

  const isFiltering = useMemo(
    () => Boolean(urlQuery.trim() || urlCategoryId || urlSort !== 'newest' || urlPage > 0),
    [urlQuery, urlCategoryId, urlSort, urlPage]
  );

  // Load initial explore sections & categories
  useEffect(() => {
    let active = true;
    const loadExplore = async () => {
      setExploreLoading(true);
      try {
        const res = await flashcardApi.explore();
        if (active && res?.data) {
          setExploreData({
            featuredSets: res.data.featuredSets || [],
            popularSets: res.data.popularSets || [],
            recentSets: res.data.recentSets || [],
            categories: res.data.categories || [],
          });
        }
      } catch (err) {
        if (active) setErrorMsg(extractApiErrorMessage(err, 'Không thể tải dữ liệu khám phá.'));
      } finally {
        if (active) setExploreLoading(false);
      }
    };
    loadExplore();
    return () => {
      active = false;
    };
  }, []);

  // Load user's saved set IDs so bookmark state stays synced
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
      .catch(() => {
        // Non-blocking
      });
    return () => {
      active = false;
    };
  }, [isAuthenticated]);

  // Execute search when filters are active
  useEffect(() => {
    if (!isFiltering) {
      setSearchResult(null);
      return;
    }
    let active = true;
    const runSearch = async () => {
      setSearchLoading(true);
      setErrorMsg('');
      try {
        const res = await flashcardApi.search({
          keyword: urlQuery,
          categoryId: urlCategoryId || undefined,
          sort: urlSort,
          page: urlPage,
          size: 12,
        });
        if (active) {
          setSearchResult(res?.data || null);
        }
      } catch (err) {
        if (active) setErrorMsg(extractApiErrorMessage(err, 'Không thể tìm kiếm bộ thẻ.'));
      } finally {
        if (active) setSearchLoading(false);
      }
    };
    runSearch();
    return () => {
      active = false;
    };
  }, [isFiltering, urlQuery, urlCategoryId, urlSort, urlPage]);

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

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    const trimmed = keywordInput.trim();
    if (trimmed.startsWith('@') && trimmed.length > 1) {
      navigate(`/users/${encodeURIComponent(trimmed.slice(1))}`);
      return;
    }
    updateParams({ q: trimmed, page: 0 });
  };

  const handleResetFilters = () => {
    setKeywordInput('');
    setSearchParams(urlMode ? { mode: urlMode } : {});
  };

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
      setErrorMsg(extractApiErrorMessage(err, 'Không thể cập nhật trạng thái lưu bộ thẻ.'));
    } finally {
      setSavingId(null);
    }
  };

  const renderSection = (title, icon, sets, emptyText) => {
    const Icon = icon;
    return (
      <section className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-[19px] font-extrabold text-[#282E3E] dark:text-white flex items-center gap-2">
            <Icon className="w-5 h-5 text-[#4255FF]" />
            {title}
          </h2>
          {sets.length > 0 && (
            <button
              type="button"
              onClick={() => updateParams({ sort: title.includes('Phổ biến') ? 'popular' : 'newest', page: 0 })}
              className="text-[13px] font-bold text-[#4255FF] hover:underline"
            >
              Xem tất cả
            </button>
          )}
        </div>

        {exploreLoading ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {[1, 2, 3].map((n) => (
              <div
                key={n}
                className="h-44 rounded-2xl bg-[#F6F7FB] dark:bg-[#202140] animate-pulse border border-[#EDEFF4] dark:border-[#2E3856]"
              />
            ))}
          </div>
        ) : sets.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-[#D9DDE8] dark:border-[#2E3856] p-6 text-center text-sm text-[#586380] dark:text-[#A8B1D1]">
            {emptyText}
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {sets.map((set) => (
              <FlashcardSetCard
                key={set.id}
                set={set}
                isSaved={savedIds.has(set.id)}
                saving={savingId === set.id}
                onToggleSave={handleToggleSave}
              />
            ))}
          </div>
        )}
      </section>
    );
  };

  return (
    <MainLayout>
      <div className="max-w-6xl mx-auto px-4 sm:px-8 py-6 pb-20 space-y-8">
        {/* Hero / Search Header */}
        <section className="rounded-3xl bg-gradient-to-br from-[#F6F7FB] via-white to-[#EDEFFF] dark:from-[#171836] dark:via-[#202140] dark:to-[#1B224B] border border-[#EDEFF4] dark:border-[#2E3856] p-6 sm:p-8 shadow-sm">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div>
              <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#EDEFFF] dark:bg-[#2E3856] text-[#4255FF] dark:text-[#9AA7FF] text-xs font-bold mb-2.5">
                <Compass className="w-3.5 h-3.5" />
                Khám phá cộng đồng SmartFlash
              </div>
              <h1 className="text-2xl sm:text-3xl font-extrabold text-[#282E3E] dark:text-white tracking-tight">
                Tìm kiếm & Khám phá Bộ thẻ Công khai
              </h1>
              <p className="mt-1.5 text-sm text-[#586380] dark:text-[#A8B1D1] max-w-2xl">
                {MODE_BANNERS[urlMode] ||
                  'Học từ hàng ngàn bộ thẻ flashcard được chia sẻ bởi cộng đồng học viên và giáo viên.'}
              </p>
            </div>

            {!isAuthenticated && (
              <div className="flex items-center gap-2.5 shrink-0">
                <Link
                  to="/login"
                  className="px-4 py-2.5 rounded-full text-sm font-bold text-[#282E3E] dark:text-white bg-white dark:bg-[#2E3856] border border-[#D9DDE8] dark:border-[#3D4870] hover:bg-[#F6F7FB] transition"
                >
                  Đăng nhập
                </Link>
                <Link
                  to="/register"
                  className="px-5 py-2.5 rounded-full text-sm font-bold text-white bg-[#4255FF] hover:bg-[#3545E0] transition shadow-sm"
                >
                  Tạo tài khoản miễn phí
                </Link>
              </div>
            )}
          </div>

          {/* Search & Sort Controls */}
          <form onSubmit={handleSearchSubmit} className="mt-6 flex flex-col sm:flex-row gap-3">
            <div className="relative flex-1">
              <Search className="w-4 h-4 text-[#586380] dark:text-[#A8B1D1] absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                type="text"
                value={keywordInput}
                onChange={(e) => setKeywordInput(e.target.value)}
                placeholder="Nhập từ khóa môn học, từ vựng, chủ đề... (hoặc @username)"
                className="w-full h-11 pl-10 pr-9 rounded-xl bg-white dark:bg-[#0A082D] border border-[#D9DDE8] dark:border-[#2E3856] focus:border-[#4255FF] text-sm font-medium text-[#282E3E] dark:text-white placeholder:text-[#586380] dark:placeholder:text-[#A8B1D1] outline-none transition"
              />
              {keywordInput && (
                <button
                  type="button"
                  onClick={() => {
                    setKeywordInput('');
                    if (urlQuery) updateParams({ q: '', page: 0 });
                  }}
                  aria-label="Xóa từ khóa"
                  className="absolute right-2.5 top-1/2 -translate-y-1/2 p-1 rounded-full text-[#586380] hover:text-[#282E3E] dark:text-[#A8B1D1]"
                >
                  <X className="w-4 h-4" />
                </button>
              )}
            </div>

            <div className="flex items-center gap-2">
              <div className="relative">
                <SlidersHorizontal className="w-4 h-4 text-[#586380] dark:text-[#A8B1D1] absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                <select
                  value={urlSort}
                  onChange={(e) => updateParams({ sort: e.target.value, page: 0 })}
                  aria-label="Sắp xếp kết quả"
                  className="h-11 pl-9 pr-4 rounded-xl bg-white dark:bg-[#0A082D] border border-[#D9DDE8] dark:border-[#2E3856] text-sm font-semibold text-[#282E3E] dark:text-white outline-none focus:border-[#4255FF]"
                >
                  {SORT_OPTIONS.map((opt) => (
                    <option key={opt.value} value={opt.value}>
                      {opt.label}
                    </option>
                  ))}
                </select>
              </div>

              <button
                type="submit"
                className="h-11 px-5 rounded-xl bg-[#4255FF] hover:bg-[#3545E0] text-white text-sm font-bold transition shrink-0"
              >
                Tìm kiếm
              </button>
            </div>
          </form>

          {/* Category Filter Chips */}
          {exploreData.categories.length > 0 && (
            <div className="mt-4 flex flex-wrap items-center gap-2">
              <button
                type="button"
                onClick={() => updateParams({ categoryId: '', page: 0 })}
                className={`px-3.5 py-1.5 rounded-full text-xs font-bold transition ${
                  !urlCategoryId
                    ? 'bg-[#4255FF] text-white'
                    : 'bg-white dark:bg-[#0A082D] text-[#586380] dark:text-[#A8B1D1] border border-[#EDEFF4] dark:border-[#2E3856] hover:border-[#4255FF]'
                }`}
              >
                Tất cả danh mục
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
                        ? 'bg-[#4255FF] text-white'
                        : 'bg-white dark:bg-[#0A082D] text-[#586380] dark:text-[#A8B1D1] border border-[#EDEFF4] dark:border-[#2E3856] hover:border-[#4255FF]'
                    }`}
                  >
                    {cat.name}
                  </button>
                );
              })}
            </div>
          )}
        </section>

        {errorMsg && (
          <div className="rounded-xl bg-red-50 dark:bg-[#431F25] border border-red-200 dark:border-red-900/50 px-4 py-3 text-sm font-medium text-red-700 dark:text-red-300 flex items-center justify-between">
            <span>{errorMsg}</span>
            <button type="button" onClick={() => setErrorMsg('')} className="p-1">
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

        {/* Active Search / Filter Results */}
        {isFiltering ? (
          <section className="space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-2">
                <BookOpen className="w-5 h-5 text-[#4255FF]" />
                <h2 className="text-[19px] font-extrabold text-[#282E3E] dark:text-white">
                  Kết quả tìm kiếm
                  {searchResult ? ` (${searchResult.totalElements})` : ''}
                </h2>
              </div>

              <button
                type="button"
                onClick={handleResetFilters}
                className="inline-flex items-center gap-1.5 text-xs font-bold text-[#586380] hover:text-[#282E3E] dark:text-[#A8B1D1] dark:hover:text-white px-3 py-1.5 rounded-full bg-[#F6F7FB] dark:bg-[#202140]"
              >
                <X className="w-3.5 h-3.5" />
                Xóa bộ lọc
              </button>
            </div>

            {searchLoading ? (
              <div className="py-16 flex items-center justify-center gap-2 text-sm font-semibold text-[#586380] dark:text-[#A8B1D1]">
                <Loader2 className="w-5 h-5 animate-spin text-[#4255FF]" />
                Đang tìm kiếm bộ thẻ...
              </div>
            ) : !searchResult || searchResult.content.length === 0 ? (
              <div className="rounded-2xl border border-dashed border-[#D9DDE8] dark:border-[#2E3856] p-12 text-center">
                <p className="text-base font-bold text-[#282E3E] dark:text-white">
                  Không tìm thấy bộ thẻ nào phù hợp
                </p>
                <p className="mt-1 text-sm text-[#586380] dark:text-[#A8B1D1]">
                  Thử đổi từ khóa ngắn hơn hoặc bỏ chọn bộ lọc danh mục.
                </p>
                <button
                  type="button"
                  onClick={handleResetFilters}
                  className="mt-4 px-5 py-2 rounded-full bg-[#4255FF] text-white text-sm font-bold hover:bg-[#3545E0] transition"
                >
                  Quay lại trang Khám phá
                </button>
              </div>
            ) : (
              <>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                  {searchResult.content.map((set) => (
                    <FlashcardSetCard
                      key={set.id}
                      set={set}
                      isSaved={savedIds.has(set.id)}
                      saving={savingId === set.id}
                      onToggleSave={handleToggleSave}
                    />
                  ))}
                </div>

                {/* Pagination */}
                {searchResult.totalPages > 1 && (
                  <div className="pt-4 flex items-center justify-center gap-3">
                    <button
                      type="button"
                      disabled={searchResult.first}
                      onClick={() => updateParams({ page: urlPage - 1 })}
                      className="inline-flex items-center gap-1 px-3.5 py-2 rounded-xl border border-[#D9DDE8] dark:border-[#2E3856] text-sm font-bold text-[#282E3E] dark:text-white disabled:opacity-40 hover:bg-[#F6F7FB] dark:hover:bg-[#202140] transition"
                    >
                      <ChevronLeft className="w-4 h-4" />
                      Trước
                    </button>
                    <span className="text-sm font-semibold text-[#586380] dark:text-[#A8B1D1]">
                      Trang {searchResult.page + 1} / {searchResult.totalPages}
                    </span>
                    <button
                      type="button"
                      disabled={searchResult.last}
                      onClick={() => updateParams({ page: urlPage + 1 })}
                      className="inline-flex items-center gap-1 px-3.5 py-2 rounded-xl border border-[#D9DDE8] dark:border-[#2E3856] text-sm font-bold text-[#282E3E] dark:text-white disabled:opacity-40 hover:bg-[#F6F7FB] dark:hover:bg-[#202140] transition"
                    >
                      Sau
                      <ChevronRight className="w-4 h-4" />
                    </button>
                  </div>
                )}
              </>
            )}
          </section>
        ) : (
          <div className="space-y-10">
            {exploreData.featuredSets.length > 0 &&
              renderSection(
                'Bộ thẻ nổi bật',
                Sparkles,
                exploreData.featuredSets,
                'Chưa có bộ thẻ nổi bật.'
              )}

            {renderSection(
              'Phổ biến nhất',
              TrendingUp,
              exploreData.popularSets,
              'Chưa có bộ thẻ công khai nào. Hãy tạo bộ thẻ đầu tiên của bạn!'
            )}

            {renderSection(
              'Mới nhất',
              Clock,
              exploreData.recentSets,
              'Chưa có bộ thẻ mới nào.'
            )}
          </div>
        )}
      </div>
    </MainLayout>
  );
}

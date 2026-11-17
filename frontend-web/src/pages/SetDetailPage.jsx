import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Link,
  useLocation,
  useNavigate,
  useParams,
  useSearchParams,
} from 'react-router-dom';
import {
  AlertCircle,
  ArrowLeft,
  ArrowLeftRight,
  BadgeCheck,
  Bookmark,
  CalendarPlus,
  ChevronDown,
  Code2,
  Copy,
  Download,
  Edit3,
  Eye,
  EyeOff,
  GitMerge,
  Globe,
  Link2,
  Loader2,
  Lock,
  MoreHorizontal,
  Plus,
  Printer,
  Share,
  Sparkles,
  Star,
  Trash2,
  Volume2,
} from 'lucide-react';
import MainLayout from '../components/layout/MainLayout';
import UserAvatar from '../components/common/UserAvatar';
import CardPreviewDeck from '../components/sets/CardPreviewDeck';
import {
  FunGamesSection,
  SocialAndStudyModeBar,
} from '../components/sets/StudyModeSelector';
import SampleQuestionWidget from '../components/sets/SampleQuestionWidget';
import SaveToFolderModal from '../components/sets/SaveToFolderModal';
import ShareSetModal from '../components/sets/ShareSetModal';
import DeleteSetConfirmModal from '../components/editor/DeleteSetConfirmModal';
import flashcardApi from '../api/flashcardApi';
import { useAuthStore } from '../store/useAuthStore';
import { useTextToSpeech } from '../hooks/useTextToSpeech';
import { extractApiErrorMessage, formatShortDate } from '../utils/userHelpers';

const VISIBILITY_BADGE = {
  PUBLIC: {
    label: 'Công khai',
    icon: Globe,
  },
  LINK_ONLY: {
    label: 'Người có liên kết',
    icon: Link2,
  },
  PRIVATE: {
    label: 'Riêng tư',
    icon: Lock,
  },
};

function formatGoogleCalendarUtc(date) {
  return date.toISOString().replace(/[-:]/g, '').replace(/\.\d{3}Z$/, 'Z');
}

export default function SetDetailPage() {
  const { id, shareToken } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { user, accessToken } = useAuthStore();
  const isAuthenticated = Boolean(user && accessToken);
  const queryToken = searchParams.get('token') || undefined;

  const tts = useTextToSpeech();
  const studyModesRef = useRef(null);
  const termsSectionRef = useRef(null);
  const moreMenuRef = useRef(null);
  const sortMenuRef = useRef(null);

  const [setDetail, setSetDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState('');

  const [isSaved, setIsSaved] = useState(false);
  const [cloning, setCloning] = useState(false);
  const [moreMenuOpen, setMoreMenuOpen] = useState(false);

  // Modals
  const [saveFolderModalOpen, setSaveFolderModalOpen] = useState(false);
  const [shareModalOpen, setShareModalOpen] = useState(false);
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const [starredCardIds, setStarredCardIds] = useState([]);
  const [togglingStarId, setTogglingStarId] = useState(null);

  const [cardFilterTab, setCardFilterTab] = useState('all'); // 'all' | 'starred'
  const [sortMenuOpen, setSortMenuOpen] = useState(false);
  const [hideMode, setHideMode] = useState(null); // null | 'definition' | 'term'
  const [hideTarget, setHideTarget] = useState('definition'); // 'definition' | 'term'
  const [showTermsFloatingBar, setShowTermsFloatingBar] = useState(false);

  // Ratings state
  const [myRating, setMyRating] = useState(0);
  const [hoverRating, setHoverRating] = useState(0);
  const [reviewComment, setReviewComment] = useState('');
  const [submittingRating, setSubmittingRating] = useState(false);
  const [ratingsList, setRatingsList] = useState([]);

  // Toast feedback
  const [toast, setToast] = useState(null);

  const showToast = useCallback((message, type = 'info') => {
    setToast({ message, type });
  }, []);

  useEffect(() => {
    if (!toast) return undefined;
    const timer = setTimeout(() => setToast(null), 3200);
    return () => clearTimeout(timer);
  }, [toast]);

  // Show floating bottom bar only when scrolled down to the Terms section
  useEffect(() => {
    const checkScroll = () => {
      if (!termsSectionRef.current) {
        setShowTermsFloatingBar(false);
        return;
      }
      const rect = termsSectionRef.current.getBoundingClientRect();
      setShowTermsFloatingBar(rect.top < window.innerHeight - 100);
    };
    checkScroll();
    window.addEventListener('scroll', checkScroll, { passive: true });
    window.addEventListener('resize', checkScroll);
    return () => {
      window.removeEventListener('scroll', checkScroll);
      window.removeEventListener('resize', checkScroll);
    };
  }, [setDetail?.id]);

  // Close "..." and sort dropdowns on outside click
  useEffect(() => {
    const onDocClick = (e) => {
      if (moreMenuRef.current && !moreMenuRef.current.contains(e.target)) {
        setMoreMenuOpen(false);
      }
      if (sortMenuRef.current && !sortMenuRef.current.contains(e.target)) {
        setSortMenuOpen(false);
      }
    };
    document.addEventListener('mousedown', onDocClick);
    return () => document.removeEventListener('mousedown', onDocClick);
  }, []);

  const effectiveToken = useMemo(
    () => queryToken || setDetail?.shareToken || undefined,
    [queryToken, setDetail?.shareToken]
  );

  const shareUrl = useMemo(() => {
    if (!setDetail) return '';
    const base = `${window.location.origin}/sets/${setDetail.id}`;
    return setDetail.visibility === 'LINK_ONLY' && setDetail.shareToken
      ? `${base}?token=${encodeURIComponent(setDetail.shareToken)}`
      : base;
  }, [setDetail]);

  const loadSetDetail = useCallback(async () => {
    setLoading(true);
    setErrorMsg('');
    try {
      const res = shareToken
        ? await flashcardApi.getSetByShareToken(shareToken)
        : await flashcardApi.getSetById(id, queryToken);
      const data = res.data;
      setSetDetail(data);

      const resolvedId = data?.id;
      const tokenForSubCalls = queryToken || data?.shareToken || undefined;

      if (resolvedId) {
        flashcardApi
          .getSetRatings(resolvedId, { page: 0, size: 10, token: tokenForSubCalls })
          .then((r) => setRatingsList(r.data?.items || []))
          .catch(() => {});
      }

      if (resolvedId && isAuthenticated) {
        Promise.allSettled([
          flashcardApi.getInteraction(resolvedId, tokenForSubCalls),
          flashcardApi.getStarredCards(resolvedId, tokenForSubCalls),
        ]).then(([interRes, starRes]) => {
          if (interRes.status === 'fulfilled' && interRes.value?.data) {
            const inter = interRes.value.data;
            setIsSaved(Boolean(inter.saved));
            if (inter.rating) setMyRating(Number(inter.rating));
            if (inter.reviewComment) setReviewComment(inter.reviewComment);
          }
          if (starRes.status === 'fulfilled' && starRes.value?.data) {
            setStarredCardIds(starRes.value.data.starredCardIds || []);
          }
        });
      } else {
        setIsSaved(false);
        setStarredCardIds([]);
      }
    } catch (err) {
      setErrorMsg(
        extractApiErrorMessage(err, 'Không thể tải học phần này hoặc bạn không có quyền xem.')
      );
      setSetDetail(null);
    } finally {
      setLoading(false);
    }
  }, [id, shareToken, queryToken, isAuthenticated]);

  useEffect(() => {
    loadSetDetail();
  }, [loadSetDetail]);

  const requireAuthOrRedirect = useCallback(
    (actionLabel) => {
      if (isAuthenticated) return true;
      showToast(`Vui lòng đăng nhập để ${actionLabel}.`, 'warning');
      navigate('/login', { state: { from: location } });
      return false;
    },
    [isAuthenticated, location, navigate, showToast]
  );

  const handleOpenSaveModal = () => {
    if (!setDetail || !requireAuthOrRedirect('lưu vào thư mục')) return;
    setSaveFolderModalOpen(true);
  };

  const handleToggleBookmarkOnly = async (shouldSave) => {
    if (!setDetail || !requireAuthOrRedirect('lưu học phần')) return;
    try {
      if (shouldSave) {
        await flashcardApi.saveSet(setDetail.id, effectiveToken);
        setIsSaved(true);
        showToast('Đã lưu học phần vào thư viện!', 'success');
      } else {
        await flashcardApi.unsaveSet(setDetail.id, effectiveToken);
        setIsSaved(false);
        showToast('Đã bỏ lưu học phần.', 'info');
      }
    } catch (err) {
      showToast(extractApiErrorMessage(err, 'Không thể cập nhật trạng thái lưu.'), 'error');
    }
  };

  const handleAddToGoogleCalendar = () => {
    if (!setDetail) return;
    const start = new Date();
    start.setDate(start.getDate() + 1);
    start.setHours(9, 0, 0, 0);
    const end = new Date(start.getTime() + 30 * 60 * 1000);

    const params = new URLSearchParams({
      action: 'TEMPLATE',
      text: `Ôn tập: ${setDetail.title}`,
      dates: `${formatGoogleCalendarUtc(start)}/${formatGoogleCalendarUtc(end)}`,
      details: `Lịch ôn tập bộ thẻ "${setDetail.title}" (${cards.length} thuật ngữ) trên ELearner:\n${shareUrl}`,
    });

    window.open(
      `https://calendar.google.com/calendar/render?${params.toString()}`,
      '_blank',
      'noopener,noreferrer'
    );
  };

  const handleCloneSet = async () => {
    if (!setDetail || !requireAuthOrRedirect('tạo bản sao học phần')) return;
    setCloning(true);
    setMoreMenuOpen(false);
    try {
      const res = await flashcardApi.cloneSet(setDetail.id, null, effectiveToken);
      const cloned = res.data;
      showToast('Đã tạo một bản sao học phần!', 'success');
      if (cloned?.id) {
        navigate(`/sets/${cloned.id}/edit`);
      }
    } catch (err) {
      showToast(extractApiErrorMessage(err, 'Không thể tạo bản sao học phần.'), 'error');
    } finally {
      setCloning(false);
    }
  };

  const handleExportCards = () => {
    if (!setDetail) return;
    setMoreMenuOpen(false);
    const lines = cards.map((c) => `${c.term || ''}\t${c.definition || ''}`).join('\n');
    const blob = new Blob([lines], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${(setDetail.title || 'flashcards').replace(/[^a-zA-Z0-9-_]+/g, '_')}.txt`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    showToast('Đã xuất danh sách thẻ (.txt)!', 'success');
  };

  const handleCopyEmbed = async () => {
    if (!setDetail) return;
    setMoreMenuOpen(false);
    const embedCode = `<iframe src="${shareUrl}" width="100%" height="500" style="border:0;"></iframe>`;
    try {
      await navigator.clipboard?.writeText(embedCode);
      showToast('Đã sao chép mã nhúng HTML!', 'success');
    } catch {
      showToast('Không thể sao chép mã nhúng.', 'error');
    }
  };

  const handleToggleStarAllVisible = async () => {
    if (!setDetail || filteredListCards.length === 0 || !requireAuthOrRedirect('gắn sao thẻ')) {
      return;
    }
    const allStarred = filteredListCards.every((c) => starredCardIds.includes(c.id));
    try {
      if (allStarred) {
        await Promise.all(
          filteredListCards.map((c) => flashcardApi.unstarCard(setDetail.id, c.id))
        );
        const visibleIds = new Set(filteredListCards.map((c) => c.id));
        setStarredCardIds((prev) => prev.filter((cid) => !visibleIds.has(cid)));
        showToast('Đã bỏ gắn sao tất cả thẻ hiển thị.', 'info');
      } else {
        const toStar = filteredListCards.filter((c) => !starredCardIds.includes(c.id));
        await Promise.all(
          toStar.map((c) => flashcardApi.starCard(setDetail.id, c.id, effectiveToken))
        );
        setStarredCardIds((prev) => [
          ...prev,
          ...toStar.map((c) => c.id).filter((cid) => !prev.includes(cid)),
        ]);
        showToast(`Đã gắn sao ${toStar.length} thẻ!`, 'success');
      }
    } catch (err) {
      showToast(extractApiErrorMessage(err, 'Không thể cập nhật gắn sao hàng loạt.'), 'error');
    }
  };

  const handleToggleStarCard = async (card) => {
    if (!setDetail || !card || !requireAuthOrRedirect('gắn sao thẻ khó')) return;
    const currentlyStarred = starredCardIds.includes(card.id);
    setTogglingStarId(card.id);
    try {
      if (currentlyStarred) {
        await flashcardApi.unstarCard(setDetail.id, card.id);
        setStarredCardIds((prev) => prev.filter((cid) => cid !== card.id));
      } else {
        await flashcardApi.starCard(setDetail.id, card.id, effectiveToken);
        setStarredCardIds((prev) => (prev.includes(card.id) ? prev : [...prev, card.id]));
      }
    } catch (err) {
      showToast(extractApiErrorMessage(err, 'Không thể cập nhật gắn sao thẻ.'), 'error');
    } finally {
      setTogglingStarId(null);
    }
  };

  const handleSubmitRating = async (e) => {
    e.preventDefault();
    if (!setDetail || !requireAuthOrRedirect('đánh giá học phần')) return;
    if (myRating < 1 || myRating > 5) {
      showToast('Vui lòng chọn từ 1 đến 5 sao.', 'warning');
      return;
    }
    setSubmittingRating(true);
    try {
      await flashcardApi.rateSet(
        setDetail.id,
        { rating: myRating, reviewComment: reviewComment.trim() || null },
        effectiveToken
      );
      showToast('Cảm ơn bạn đã đánh giá học phần!', 'success');
      const [detailRes, ratingsRes] = await Promise.all([
        shareToken
          ? flashcardApi.getSetByShareToken(shareToken)
          : flashcardApi.getSetById(setDetail.id, effectiveToken),
        flashcardApi.getSetRatings(setDetail.id, { page: 0, size: 10, token: effectiveToken }),
      ]);
      if (detailRes.data) setSetDetail(detailRes.data);
      if (ratingsRes.data) setRatingsList(ratingsRes.data.items || []);
    } catch (err) {
      showToast(extractApiErrorMessage(err, 'Không thể gửi đánh giá.'), 'error');
    } finally {
      setSubmittingRating(false);
    }
  };

  const handleConfirmDelete = async () => {
    if (!setDetail) return;
    setDeleting(true);
    try {
      await flashcardApi.deleteSet(setDetail.id);
      setDeleteModalOpen(false);
      navigate('/library');
    } catch (err) {
      showToast(extractApiErrorMessage(err, 'Không thể xóa học phần.'), 'error');
      setDeleting(false);
    }
  };

  const handleSelectStudyMode = (mode) => {
    showToast(
      `Chế độ "${mode.label}" đang được phát triển ở ${mode.phaseNote}. Hãy trải nghiệm thẻ lật xem trước hoặc câu hỏi mẫu bên dưới nhé!`,
      'info'
    );
  };

  const cards = useMemo(() => setDetail?.cards || [], [setDetail?.cards]);

  const deckCards = useMemo(() => {
    if (cardFilterTab === 'starred') {
      const starred = cards.filter((c) => starredCardIds.includes(c.id));
      return starred.length > 0 ? starred : cards;
    }
    return cards;
  }, [cards, cardFilterTab, starredCardIds]);

  const filteredListCards = useMemo(() => {
    return cardFilterTab === 'starred'
      ? cards.filter((c) => starredCardIds.includes(c.id))
      : cards;
  }, [cards, cardFilterTab, starredCardIds]);

  const starredCount = useMemo(
    () => cards.filter((c) => starredCardIds.includes(c.id)).length,
    [cards, starredCardIds]
  );

  const visMeta = VISIBILITY_BADGE[setDetail?.visibility] || VISIBILITY_BADGE.PUBLIC;
  const VisIcon = visMeta.icon;

  return (
    <MainLayout hideSidebar stickyHeader={false}>
      {/* Page container: hidden sidebar, slightly wider on both sides */}
      <div className="w-full px-4 sm:px-8 lg:pl-[21%] lg:pr-[3%] py-6 sm:py-7 pb-24">
        <div className="w-full max-w-[790px]">
          {loading ? (
            <div className="py-24 flex flex-col items-center justify-center gap-3 text-[#586380] dark:text-[#A8B1D1]">
              <Loader2 className="w-8 h-8 animate-spin text-[#4255FF]" />
              <p className="text-sm font-semibold">Đang tải chi tiết học phần...</p>
            </div>
          ) : errorMsg || !setDetail ? (
            <div className="bg-white dark:bg-[#202140] rounded-3xl border border-[#EDEFF4] dark:border-[#2E3856] p-8 sm:p-12 text-center max-w-lg mx-auto my-10">
              <div className="w-12 h-12 rounded-2xl bg-red-50 dark:bg-red-950/40 text-red-500 flex items-center justify-center mx-auto mb-4">
                <AlertCircle className="w-6 h-6" />
              </div>
              <h2 className="text-xl font-extrabold text-[#282E3E] dark:text-white">
                Không thể mở học phần
              </h2>
              <p className="mt-2 text-sm text-[#586380] dark:text-[#A8B1D1]">
                {errorMsg || 'Học phần không tồn tại hoặc đã bị xóa.'}
              </p>
              <div className="mt-6 flex items-center justify-center gap-3">
                <Link
                  to="/explore"
                  className="inline-flex items-center gap-2 h-10 px-5 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white text-sm font-bold transition"
                >
                  <ArrowLeft className="w-4 h-4" />
                  Khám phá bộ thẻ
                </Link>
              </div>
            </div>
          ) : (
            <div className="space-y-8">
              {/* 1. Quizlet-style Header (Matches Images 1 & 2) */}
              <div className="space-y-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <h1 className="text-2xl sm:text-[32px] font-extrabold text-[#282E3E] dark:text-white tracking-tight leading-tight break-words">
                    {setDetail.title}
                  </h1>

                  {/* Right Action Cluster: Bookmark -> Thêm vào lịch -> Chia sẻ -> Xem thêm (...) */}
                  <div className="flex items-center gap-2 shrink-0">
                    {/* Save / Folder Modal */}
                    <button
                      type="button"
                      onClick={handleOpenSaveModal}
                      title="Lưu vào thư mục"
                      aria-label="Lưu vào thư mục"
                      className={`w-10 h-10 rounded-full flex items-center justify-center transition ${
                        isSaved
                          ? 'bg-[#EDEFFF] text-[#4255FF] dark:bg-[#2E3856] dark:text-[#9AA7FF]'
                          : 'bg-[#F6F7FB] hover:bg-[#EDEFF4] text-[#282E3E] dark:bg-[#202140] dark:hover:bg-[#2E3856] dark:text-[#E2E8F0]'
                      }`}
                    >
                      <Bookmark className={`w-4.5 h-4.5 ${isSaved ? 'fill-current' : ''}`} />
                    </button>

                    {/* Add to Google Calendar */}
                    <button
                      type="button"
                      onClick={handleAddToGoogleCalendar}
                      title="Thêm lịch ôn tập vào Google Calendar"
                      className="h-10 px-4 rounded-full bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#202140] dark:hover:bg-[#2E3856] text-[13px] font-bold text-[#282E3E] dark:text-[#E2E8F0] inline-flex items-center gap-2 transition"
                    >
                      <CalendarPlus className="w-4 h-4 text-[#586380] dark:text-[#A8B1D1]" />
                      <span>Thêm vào lịch</span>
                    </button>

                    {/* Share Modal */}
                    <button
                      type="button"
                      onClick={() => setShareModalOpen(true)}
                      title="Chia sẻ học phần"
                      aria-label="Chia sẻ học phần"
                      className="w-10 h-10 rounded-full bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#202140] dark:hover:bg-[#2E3856] text-[#282E3E] dark:text-[#E2E8F0] flex items-center justify-center transition"
                    >
                      <Share className="w-4.5 h-4.5" />
                    </button>

                    {/* More (...) Dropdown */}
                    <div className="relative" ref={moreMenuRef}>
                      <button
                        type="button"
                        onClick={() => setMoreMenuOpen((prev) => !prev)}
                        title="Xem thêm"
                        aria-label="Xem thêm tùy chọn"
                        className="w-10 h-10 rounded-full bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#202140] dark:hover:bg-[#2E3856] text-[#282E3E] dark:text-[#E2E8F0] flex items-center justify-center transition"
                      >
                        <MoreHorizontal className="w-4.5 h-4.5" />
                      </button>

                      {moreMenuOpen && (
                        <div className="absolute right-0 mt-2 w-56 rounded-2xl bg-white dark:bg-[#202140] border border-[#EDEFF4] dark:border-[#2E3856] shadow-[0_12px_36px_rgba(40,46,62,0.14)] py-2 z-40">
                          {setDetail.ownerView && (
                            <Link
                              to={`/sets/${setDetail.id}/edit`}
                              onClick={() => setMoreMenuOpen(false)}
                              className="w-full px-4 py-2.5 text-left text-sm font-semibold text-[#586380] dark:text-[#E2E8F0] hover:bg-[#F6F7FB] dark:hover:bg-[#2E3856] flex items-center gap-3"
                            >
                              <Edit3 className="w-4 h-4" />
                              <span>Sửa</span>
                            </Link>
                          )}

                          <button
                            type="button"
                            disabled={cloning}
                            onClick={handleCloneSet}
                            className="w-full px-4 py-2.5 text-left text-sm font-semibold text-[#586380] dark:text-[#E2E8F0] hover:bg-[#F6F7FB] dark:hover:bg-[#2E3856] flex items-center gap-3 disabled:opacity-60"
                          >
                            {cloning ? (
                              <Loader2 className="w-4 h-4 animate-spin" />
                            ) : (
                              <Copy className="w-4 h-4" />
                            )}
                            <span>Tạo một bản sao</span>
                          </button>

                          <button
                            type="button"
                            onClick={() => {
                              setMoreMenuOpen(false);
                              window.print();
                            }}
                            className="w-full px-4 py-2.5 text-left text-sm font-semibold text-[#586380] dark:text-[#E2E8F0] hover:bg-[#F6F7FB] dark:hover:bg-[#2E3856] flex items-center gap-3"
                          >
                            <Printer className="w-4 h-4" />
                            <span>In</span>
                          </button>

                          <button
                            type="button"
                            onClick={() => {
                              setMoreMenuOpen(false);
                              handleSelectStudyMode({
                                key: 'match',
                                label: 'Ghép thẻ',
                                phaseNote: 'Phase 6 (T6.5)',
                              });
                            }}
                            className="w-full px-4 py-2.5 text-left text-sm font-semibold text-[#586380] dark:text-[#E2E8F0] hover:bg-[#F6F7FB] dark:hover:bg-[#2E3856] flex items-center gap-3"
                          >
                            <GitMerge className="w-4 h-4" />
                            <span>Ghép</span>
                          </button>

                          <button
                            type="button"
                            onClick={handleExportCards}
                            className="w-full px-4 py-2.5 text-left text-sm font-semibold text-[#586380] dark:text-[#E2E8F0] hover:bg-[#F6F7FB] dark:hover:bg-[#2E3856] flex items-center gap-3"
                          >
                            <Download className="w-4 h-4" />
                            <span>Xuất</span>
                          </button>

                          <button
                            type="button"
                            onClick={handleCopyEmbed}
                            className="w-full px-4 py-2.5 text-left text-sm font-semibold text-[#586380] dark:text-[#E2E8F0] hover:bg-[#F6F7FB] dark:hover:bg-[#2E3856] flex items-center gap-3"
                          >
                            <Code2 className="w-4 h-4" />
                            <span>Nhúng</span>
                          </button>

                          {setDetail.ownerView && (
                            <button
                              type="button"
                              onClick={() => {
                                setMoreMenuOpen(false);
                                setDeleteModalOpen(true);
                              }}
                              className="w-full px-4 py-2.5 text-left text-sm font-semibold text-[#D9383A] hover:bg-red-50 dark:hover:bg-red-950/30 flex items-center gap-3"
                            >
                              <Trash2 className="w-4 h-4" />
                              <span>Xóa</span>
                            </button>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                </div>

                {/* Visibility Pill & Metadata (Matches SetEditorPage CRUD style, no numeric codes) */}
                <div className="flex flex-wrap items-center gap-2.5">
                  <span className="inline-flex items-center gap-1.5 h-8 px-3.5 rounded-full bg-[#F6F7FB] dark:bg-[#202140] text-[13px] font-bold text-[#282E3E] dark:text-[#E2E8F0]">
                    <VisIcon className="w-3.5 h-3.5 text-[#586380] dark:text-[#A8B1D1]" />
                    <span>{visMeta.label}</span>
                  </span>
                  {setDetail.category?.name && (
                    <span className="inline-flex items-center h-8 px-3.5 rounded-full text-[13px] font-bold bg-[#EDEFFF] text-[#4255FF] dark:bg-[#202140] dark:text-[#9AA7FF]">
                      {setDetail.category.name}
                    </span>
                  )}
                  {setDetail.featured && (
                    <span className="inline-flex items-center gap-1 h-8 px-3 rounded-full text-[13px] font-bold bg-amber-50 text-amber-700 dark:bg-amber-950/40 dark:text-amber-300">
                      <Sparkles className="w-3.5 h-3.5" />
                      Nổi bật
                    </span>
                  )}
                  {setDetail.description && (
                    <span className="text-[13px] text-[#586380] dark:text-[#A8B1D1]">
                      {setDetail.description}
                    </span>
                  )}
                </div>

                {/* 2. Social & Primary Study Mode Bar (Matches Images 1 & 2) */}
                <div ref={studyModesRef} className="pt-1">
                  <SocialAndStudyModeBar
                    cardCount={cards.length}
                    onSelectMode={handleSelectStudyMode}
                  />
                </div>
              </div>

              {/* 3. 3D Continuous Circular Flip Card Preview Deck */}
              <section>
                <CardPreviewDeck
                  setId={setDetail.id}
                  setTitle={setDetail.title}
                  isOwner={Boolean(setDetail.ownerView)}
                  cards={deckCards}
                  termLang={setDetail.termLang}
                  defLang={setDetail.defLang}
                  starredCardIds={starredCardIds}
                  togglingStarId={togglingStarId}
                  onToggleStar={handleToggleStarCard}
                  onSelectLearnMode={() =>
                    handleSelectStudyMode({
                      key: 'learn',
                      label: 'Học',
                      phaseNote: 'Phase 6 (T6.4)',
                    })
                  }
                  tts={tts}
                />
              </section>

              <hr className="border-[#EDEFF4] dark:border-[#2E3856]" />

              {/* 4. Fun Games Section with Quizlet Artworks */}
              <FunGamesSection cardCount={cards.length} onSelectMode={handleSelectStudyMode} />

              {/* 5. Author Strip */}
              {setDetail.owner && (
                <div className="flex flex-wrap items-center justify-between gap-4 py-2">
                  <Link
                    to={setDetail.owner.username ? `/users/${setDetail.owner.username}` : '#'}
                    className="flex items-center gap-3 hover:opacity-85 transition"
                  >
                    <UserAvatar
                      src={setDetail.owner.avatarUrl}
                      fullName={setDetail.owner.fullName}
                      username={setDetail.owner.username}
                      size="md"
                    />
                    <div>
                      <p className="text-[11px] font-semibold text-[#586380] dark:text-[#A8B1D1]">
                        Tạo bởi
                      </p>
                      <div className="flex items-center gap-1.5">
                        <span className="text-sm font-extrabold text-[#282E3E] dark:text-white">
                          {setDetail.owner.username || setDetail.owner.fullName}
                        </span>
                        {setDetail.owner.verifiedTeacher && (
                          <BadgeCheck className="w-4 h-4 text-emerald-500" />
                        )}
                        {setDetail.owner.role === 'TEACHER' && (
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-[#EDEFFF] text-[#4255FF] dark:bg-[#2E3856] dark:text-[#8E9BFF]">
                            Giáo viên
                          </span>
                        )}
                      </div>
                      <p className="text-[11px] text-[#939BB4] dark:text-[#7E89B0]">
                        Đã tạo {formatShortDate(setDetail.createdAt)} • {setDetail.viewCount || 0}{' '}
                        lượt xem
                      </p>
                    </div>
                  </Link>
                </div>
              )}

              {/* 6. Sample Question Widget */}
              <SampleQuestionWidget
                cards={cards}
                onOpenLearnMode={() =>
                  handleSelectStudyMode({
                    key: 'learn',
                    label: 'Học',
                    phaseNote: 'Phase 6 (T6.4)',
                  })
                }
              />

              {/* 7. Terms List (Matches Image 2) */}
              <section ref={termsSectionRef} className="space-y-4 pt-2">
                <div className="flex items-center justify-between gap-3">
                  <h2 className="text-[20px] font-extrabold text-[#282E3E] dark:text-white">
                    Thuật ngữ trong học phần này ({cards.length})
                  </h2>

                  <div className="relative" ref={sortMenuRef}>
                    <button
                      type="button"
                      onClick={() => setSortMenuOpen((p) => !p)}
                      className="inline-flex items-center gap-1.5 text-[15px] font-bold text-[#586380] hover:text-[#282E3E] dark:text-[#A8B1D1] dark:hover:text-white transition"
                    >
                      <span>
                        {cardFilterTab === 'starred'
                          ? `Thẻ gắn sao (${starredCount})`
                          : 'Thông số của bạn'}
                      </span>
                      <ChevronDown className="w-4 h-4" />
                    </button>

                    {sortMenuOpen && (
                      <div className="absolute right-0 mt-2 w-48 rounded-xl bg-white dark:bg-[#202140] border border-[#EDEFF4] dark:border-[#2E3856] shadow-lg py-1.5 z-30">
                        <button
                          type="button"
                          onClick={() => {
                            setCardFilterTab('all');
                            setSortMenuOpen(false);
                          }}
                          className={`w-full px-4 py-2 text-left text-xs font-bold hover:bg-[#F6F7FB] dark:hover:bg-[#2E3856] ${
                            cardFilterTab === 'all'
                              ? 'text-[#4255FF] dark:text-[#8E9BFF]'
                              : 'text-[#282E3E] dark:text-white'
                          }`}
                        >
                          Thông số của bạn ({cards.length})
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            setCardFilterTab('starred');
                            setSortMenuOpen(false);
                          }}
                          className={`w-full px-4 py-2 text-left text-xs font-bold hover:bg-[#F6F7FB] dark:hover:bg-[#2E3856] flex items-center gap-1.5 ${
                            cardFilterTab === 'starred'
                              ? 'text-[#4255FF] dark:text-[#8E9BFF]'
                              : 'text-[#282E3E] dark:text-white'
                          }`}
                        >
                          <Star className="w-3.5 h-3.5 fill-[#FFCD1F] text-[#FFCD1F]" />
                          Thẻ gắn sao ({starredCount})
                        </button>
                      </div>
                    )}
                  </div>
                </div>

                {filteredListCards.length === 0 ? (
                  <div className="bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] p-8 text-center">
                    <p className="text-sm font-semibold text-[#586380] dark:text-[#A8B1D1]">
                      Bạn chưa gắn sao thẻ nào trong học phần này.
                    </p>
                  </div>
                ) : (
                  <div className="bg-[#F3F4F9] dark:bg-[#171936] rounded-2xl p-4 sm:p-5 space-y-3">
                    {/* Sub-header inside container (Image 2) */}
                    <div className="flex flex-wrap items-start justify-between gap-3 px-1 pb-1">
                      <div>
                        <h3 className="text-[17px] font-extrabold text-[#EA580C]">
                          {cardFilterTab === 'starred'
                            ? `Đã gắn sao (${filteredListCards.length})`
                            : `Đang học (${filteredListCards.length})`}
                        </h3>
                        <p className="mt-0.5 text-sm font-medium text-[#586380] dark:text-[#A8B1D1]">
                          Bạn đã bắt đầu học những thuật ngữ này. Tiếp tục phát huy nhé!
                        </p>
                      </div>

                      <button
                        type="button"
                        onClick={handleToggleStarAllVisible}
                        className="inline-flex items-center gap-1.5 text-sm font-bold text-[#586380] hover:text-[#282E3E] dark:text-[#A8B1D1] dark:hover:text-white transition"
                      >
                        <Star
                          className={`w-4 h-4 ${
                            filteredListCards.length > 0 &&
                            filteredListCards.every((c) => starredCardIds.includes(c.id))
                              ? 'fill-[#FFCD1F] text-[#FFCD1F]'
                              : ''
                          }`}
                        />
                        <span>Chọn {filteredListCards.length}</span>
                      </button>
                    </div>

                    {filteredListCards.map((card) => {
                      const isStarred = starredCardIds.includes(card.id);
                      const termKey = `list-${card.id}-term`;
                      const defKey = `list-${card.id}-def`;
                      return (
                        <div
                          key={card.id}
                          className="bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] p-4 sm:px-5 sm:py-4 space-y-2.5 shadow-[0_1px_4px_rgba(40,46,62,0.03)]"
                        >
                          {/* Top-right action icons */}
                          <div className="flex items-center justify-end gap-1">
                            <button
                              type="button"
                              disabled={togglingStarId === card.id}
                              title={isStarred ? 'Bỏ gắn sao' : 'Gắn sao thẻ khó'}
                              onClick={() => handleToggleStarCard(card)}
                              className={`w-8 h-8 rounded-full flex items-center justify-center transition ${
                                isStarred
                                  ? 'text-[#FFCD1F] bg-amber-50 dark:bg-amber-900/30'
                                  : 'text-[#586380] hover:bg-[#F6F7FB] dark:text-[#A8B1D1] dark:hover:bg-[#2E3856]'
                              }`}
                            >
                              <Star
                                className={`w-4 h-4 ${
                                  isStarred ? 'fill-[#FFCD1F] text-[#FFCD1F]' : ''
                                }`}
                              />
                            </button>

                            {tts.isSupported && (
                              <button
                                type="button"
                                title="Phát âm thuật ngữ"
                                onClick={() =>
                                  tts.speak(
                                    hideMode === 'definition'
                                      ? card.term
                                      : hideMode === 'term'
                                        ? card.definition
                                        : `${card.term}. ${card.definition}`,
                                    hideMode === 'term' ? setDetail.defLang : setDetail.termLang,
                                    termKey
                                  )
                                }
                                onContextMenu={(e) => {
                                  e.preventDefault();
                                  tts.speak(card.definition, setDetail.defLang, defKey);
                                }}
                                className={`w-8 h-8 rounded-full flex items-center justify-center transition ${
                                  tts.speakingKey === termKey || tts.speakingKey === defKey
                                    ? 'bg-[#4255FF] text-white'
                                    : 'text-[#586380] hover:bg-[#F6F7FB] dark:text-[#A8B1D1] dark:hover:bg-[#2E3856]'
                                }`}
                              >
                                <Volume2 className="w-4 h-4" />
                              </button>
                            )}

                            {setDetail.ownerView && (
                              <Link
                                to={`/sets/${setDetail.id}/edit`}
                                title="Chỉnh sửa thẻ"
                                className="w-8 h-8 rounded-full flex items-center justify-center text-[#586380] hover:bg-[#F6F7FB] dark:text-[#A8B1D1] dark:hover:bg-[#2E3856] transition"
                              >
                                <Edit3 className="w-4 h-4" />
                              </Link>
                            )}
                          </div>

                          {/* Term & Definition Columns (wider spacing + +1 text size step) */}
                          <div className="flex flex-col sm:flex-row sm:items-center gap-6 sm:gap-7 pb-1">
                            <div className="sm:w-[34%] sm:pr-6 sm:border-r border-[#EDEFF4] dark:border-[#2E3856] min-w-0">
                              {hideMode === 'term' ? (
                                <span className="inline-flex items-center px-3 py-1 rounded-lg bg-[#F6F7FB] dark:bg-[#2E3856] text-sm font-semibold text-[#939BB4] select-none">
                                  Thuật ngữ đang ẩn
                                </span>
                              ) : (
                                <p className="text-base font-medium text-[#282E3E] dark:text-white break-words">
                                  {card.term}
                                </p>
                              )}
                              {card.hint && (
                                <p className="mt-1 text-sm text-[#586380] dark:text-[#A8B1D1]">
                                  Gợi ý: {card.hint}
                                </p>
                              )}
                            </div>

                            <div className="flex-1 flex items-center justify-between gap-3 min-w-0">
                              {hideMode === 'definition' ? (
                                <span className="inline-flex items-center px-3 py-1 rounded-lg bg-[#F6F7FB] dark:bg-[#2E3856] text-sm font-semibold text-[#939BB4] select-none">
                                  Định nghĩa đang ẩn
                                </span>
                              ) : (
                                <p className="text-base font-normal text-[#282E3E] dark:text-[#E2E8F0] break-words">
                                  {card.definition}
                                </p>
                              )}

                              {card.imageUrl && hideMode !== 'definition' && (
                                <img
                                  src={card.imageUrl}
                                  alt={card.term}
                                  className="w-14 h-14 object-cover rounded-xl border border-[#EDEFF4] dark:border-[#2E3856] shrink-0"
                                />
                              )}
                            </div>
                          </div>
                        </div>
                      );
                    })}

                  </div>
                )}

                {/* Bottom centered "Thêm hoặc xóa thuật ngữ" pill button (matches reference image) */}
                {setDetail.ownerView && (
                  <div className="flex justify-center pt-3">
                    <Link
                      to={`/sets/${setDetail.id}/edit`}
                      className="h-11 px-6 rounded-full bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#202140] dark:hover:bg-[#2E3856] text-sm font-bold text-[#282E3E] dark:text-white inline-flex items-center gap-2 transition"
                    >
                      <span>Thêm hoặc xóa thuật ngữ</span>
                      <Edit3 className="w-4 h-4 text-[#586380] dark:text-[#A8B1D1]" />
                    </Link>
                  </div>
                )}
              </section>

              {/* 8. Rating & Reviews Section */}
              <section className="bg-white dark:bg-[#202140] rounded-3xl border border-[#EDEFF4] dark:border-[#2E3856] p-5 sm:p-6 space-y-5">
                <div>
                  <h2 className="text-lg font-extrabold text-[#282E3E] dark:text-white">
                    Đánh giá học phần
                  </h2>
                  <p className="text-xs font-medium text-[#586380] dark:text-[#A8B1D1]">
                    Trung bình {Number(setDetail.avgRating || 0).toFixed(1)} / 5 sao (
                    {setDetail.ratingCount || 0} lượt đánh giá)
                  </p>
                </div>

                {isAuthenticated ? (
                  <form
                    onSubmit={handleSubmitRating}
                    className="p-4 rounded-2xl bg-[#F6F7FB] dark:bg-[#171936] border border-[#EDEFF4] dark:border-[#2E3856] space-y-3"
                  >
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="text-xs font-bold text-[#282E3E] dark:text-white">
                        Đánh giá của bạn:
                      </span>
                      <div className="flex items-center gap-1">
                        {[1, 2, 3, 4, 5].map((starValue) => {
                          const active = (hoverRating || myRating) >= starValue;
                          return (
                            <button
                              key={starValue}
                              type="button"
                              onMouseEnter={() => setHoverRating(starValue)}
                              onMouseLeave={() => setHoverRating(0)}
                              onClick={() => setMyRating(starValue)}
                              className="p-0.5 transition-transform hover:scale-110"
                            >
                              <Star
                                className={`w-5 h-5 ${
                                  active
                                    ? 'fill-[#FFCD1F] text-[#FFCD1F]'
                                    : 'text-[#939BB4] dark:text-[#586380]'
                                }`}
                              />
                            </button>
                          );
                        })}
                      </div>
                    </div>

                    <div className="flex flex-col sm:flex-row gap-2">
                      <input
                        type="text"
                        maxLength={500}
                        value={reviewComment}
                        onChange={(e) => setReviewComment(e.target.value)}
                        placeholder="Chia sẻ nhận xét về học phần này (không bắt buộc)..."
                        className="flex-1 h-10 px-3.5 rounded-xl bg-white dark:bg-[#202140] border border-[#D9DDE8] dark:border-[#2E3856] text-sm text-[#282E3E] dark:text-white placeholder-[#939BB4] focus:outline-none focus:border-[#4255FF]"
                      />
                      <button
                        type="submit"
                        disabled={submittingRating || myRating < 1}
                        className="h-10 px-5 rounded-xl bg-[#4255FF] hover:bg-[#3545E0] disabled:opacity-50 text-white text-sm font-bold inline-flex items-center justify-center gap-2 transition shrink-0"
                      >
                        {submittingRating && <Loader2 className="w-4 h-4 animate-spin" />}
                        Gửi đánh giá
                      </button>
                    </div>
                  </form>
                ) : (
                  <p className="text-xs font-medium text-[#586380] dark:text-[#A8B1D1]">
                    <Link to="/login" className="text-[#4255FF] font-bold hover:underline">
                      Đăng nhập
                    </Link>{' '}
                    để đánh giá học phần này.
                  </p>
                )}

                {ratingsList.length > 0 && (
                  <div className="divide-y divide-[#EDEFF4] dark:divide-[#2E3856]">
                    {ratingsList.map((item) => (
                      <div
                        key={`${item.userId}-${item.ratedAt}`}
                        className="py-3 flex items-start gap-3"
                      >
                        <UserAvatar
                          src={item.avatarUrl}
                          fullName={item.fullName}
                          username={item.username}
                          size="sm"
                        />
                        <div className="min-w-0 flex-1">
                          <div className="flex flex-wrap items-center gap-2">
                            <span className="text-sm font-bold text-[#282E3E] dark:text-white">
                              {item.fullName || item.username}
                            </span>
                            <div className="flex items-center gap-0.5">
                              {[1, 2, 3, 4, 5].map((s) => (
                                <Star
                                  key={s}
                                  className={`w-3.5 h-3.5 ${
                                    s <= item.rating
                                      ? 'fill-[#FFCD1F] text-[#FFCD1F]'
                                      : 'text-[#D9DDE8] dark:text-[#2E3856]'
                                  }`}
                                />
                              ))}
                            </div>
                            <span className="text-[11px] text-[#939BB4]">
                              {formatShortDate(item.ratedAt)}
                            </span>
                          </div>
                          {item.reviewComment && (
                            <p className="mt-1 text-sm text-[#586380] dark:text-[#A8B1D1]">
                              {item.reviewComment}
                            </p>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </section>
            </div>
          )}
        </div>
      </div>

      {/* Enlarged Floating Quizlet-style Bottom Pill Bar (only visible when scrolled to Terms section) */}
      {setDetail && cards.length > 0 && showTermsFloatingBar && (
        <div className="fixed bottom-5 left-1/2 -translate-x-1/2 z-30 bg-white/95 dark:bg-[#202140]/95 backdrop-blur-md border border-[#EDEFF4] dark:border-[#2E3856] shadow-[0_10px_36px_rgba(40,46,62,0.18)] rounded-full px-4 py-2.5 flex items-center gap-3">
          {/* Split Pill: Hide/Show Definition or Term + Swap target button */}
          <div className="h-11 rounded-full border-2 border-[#D9DDE8] dark:border-[#2E3856] bg-white dark:bg-[#171936] flex items-center overflow-hidden">
            <button
              type="button"
              onClick={() =>
                setHideMode((prev) => (prev === hideTarget ? null : hideTarget))
              }
              className="h-full px-5 hover:bg-[#F6F7FB] dark:hover:bg-[#2E3856] text-sm font-bold text-[#586380] dark:text-white inline-flex items-center gap-2 transition"
            >
              {hideMode === hideTarget
                ? hideTarget === 'definition'
                  ? 'Hiển thị định nghĩa'
                  : 'Hiển thị thuật ngữ'
                : hideTarget === 'definition'
                  ? 'Ẩn định nghĩa'
                  : 'Ẩn thuật ngữ'}
            </button>

            <span className="w-[1px] h-5 bg-[#D9DDE8] dark:bg-[#2E3856]" />

            <button
              type="button"
              title="Chuyển đổi ẩn Định nghĩa / Thuật ngữ"
              onClick={() => {
                const nextTarget = hideTarget === 'definition' ? 'term' : 'definition';
                setHideTarget(nextTarget);
                if (hideMode !== null) {
                  setHideMode(nextTarget);
                }
              }}
              className="h-full px-3.5 hover:bg-[#F6F7FB] dark:hover:bg-[#2E3856] text-[#586380] dark:text-[#A8B1D1] flex items-center justify-center transition"
            >
              <ArrowLeftRight className="w-4 h-4" />
            </button>
          </div>

          <button
            type="button"
            onClick={() =>
              studyModesRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' })
            }
            className="h-11 px-6 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white text-sm font-bold inline-flex items-center gap-2 transition"
          >
            Ôn tập qua hoạt động
            <ChevronDown className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Modals */}
      {setDetail && (
        <>
          <SaveToFolderModal
            open={saveFolderModalOpen}
            setId={setDetail.id}
            shareToken={effectiveToken}
            isSaved={isSaved}
            onToggleBookmark={handleToggleBookmarkOnly}
            onClose={() => setSaveFolderModalOpen(false)}
            onSuccess={(msg) => showToast(msg, 'success')}
          />

          <ShareSetModal
            open={shareModalOpen}
            title={setDetail.title}
            shareUrl={shareUrl}
            onClose={() => setShareModalOpen(false)}
            onCopied={(msg) => showToast(msg, 'success')}
          />

          <DeleteSetConfirmModal
            open={deleteModalOpen}
            isEditMode
            loading={deleting}
            onClose={() => !deleting && setDeleteModalOpen(false)}
            onConfirm={handleConfirmDelete}
          />
        </>
      )}

      {/* Toast Notification */}
      {toast && (
        <div className="fixed bottom-20 right-5 z-50 max-w-sm px-4 py-3 rounded-2xl bg-[#282E3E] dark:bg-[#2E3856] text-white text-sm font-semibold shadow-xl border border-white/10">
          {toast.message}
        </div>
      )}
    </MainLayout>
  );
}

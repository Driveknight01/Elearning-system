import React, { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import {
  AlertCircle,
  ArrowLeft,
  ArrowLeftRight,
  Globe,
  Link2,
  Loader2,
  Lock,
  Plus,
  ScanText,
  Search,
  ShieldAlert,
  Sparkles,
  Trash2,
  X,
} from 'lucide-react';
import MainLayout from '../components/layout/MainLayout';
import CardEditorRow from '../components/editor/CardEditorRow';
import BulkImportModal from '../components/editor/BulkImportModal';
import OcrScanModal from '../components/editor/OcrScanModal';
import AccessControlModal from '../components/editor/AccessControlModal';
import DeleteSetConfirmModal from '../components/editor/DeleteSetConfirmModal';
import flashcardApi from '../api/flashcardApi';
import { useAuthStore } from '../store/useAuthStore';
import { extractApiErrorMessage } from '../utils/userHelpers';
import { createDraftCard, normalizePrefillData } from '../utils/bulkCardParser';

const VISIBILITY_META = {
  PUBLIC: { label: 'Công khai', icon: Globe },
  LINK_ONLY: { label: 'Người có liên kết', icon: Link2 },
  PRIVATE: { label: 'Riêng tư', icon: Lock },
};

export default function SetEditorPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuthStore();

  const isEditMode = Boolean(id);
  const prefillData = !isEditMode ? location.state?.prefillData : null;

  // Form states
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [visibility, setVisibility] = useState('PUBLIC');
  const [termLang, setTermLang] = useState('');
  const [defLang, setDefLang] = useState('');
  const [tags, setTags] = useState([]);
  const [sourceType, setSourceType] = useState('MANUAL');
  const [cards, setCards] = useState(() => [createDraftCard(), createDraftCard()]);

  // Edit mode extras
  const [shareToken, setShareToken] = useState('');
  const [regenerateShareToken, setRegenerateShareToken] = useState(false);

  // Page & toolbar states
  const [categories, setCategories] = useState([]);
  const [loadingPage, setLoadingPage] = useState(isEditMode);
  const [accessDenied, setAccessDenied] = useState(false);
  const [savingMode, setSavingMode] = useState(null); // null | 'save' | 'study'
  const [deleting, setDeleting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [showValidationErrors, setShowValidationErrors] = useState(false);
  const [prefillBannerVisible, setPrefillBannerVisible] = useState(false);

  // Quizlet toolbar toggles ("Gợi ý" = live word-typing autocomplete)
  const [autoSuggestEnabled, setAutoSuggestEnabled] = useState(true);
  const [searchOpen, setSearchOpen] = useState(false);
  const [cardSearchQuery, setCardSearchQuery] = useState('');
  const [aiHelperNotice, setAiHelperNotice] = useState(false);

  // Modals
  const [bulkModalOpen, setBulkModalOpen] = useState(false);
  const [ocrModalOpen, setOcrModalOpen] = useState(false);
  const [accessModalOpen, setAccessModalOpen] = useState(false);
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);

  // Drag & Drop state
  const [dragIdx, setDragIdx] = useState(null);
  const [dragOverIdx, setDragOverIdx] = useState(null);

  // Sticky bar on scroll + Draft auto-save/restore
  const [isScrolled, setIsScrolled] = useState(false);
  const [draftToastOpen, setDraftToastOpen] = useState(false);
  const [draftHydrated, setDraftHydrated] = useState(isEditMode || Boolean(prefillData));
  const draftStorageKey = `smartflash_set_draft_${user?.id || 'guest'}`;

  useEffect(() => {
    const onScroll = () => setIsScrolled(window.scrollY > 72);
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  // Restore saved draft in Create mode (unless AI Chatbot prefillData is provided)
  useEffect(() => {
    if (isEditMode || prefillData) {
      setDraftHydrated(true);
      return;
    }
    try {
      const raw = localStorage.getItem(draftStorageKey);
      if (raw) {
        const parsed = JSON.parse(raw);
        const hasCardsContent =
          Array.isArray(parsed?.cards) &&
          parsed.cards.some(
            (c) => c?.term?.trim() || c?.definition?.trim() || c?.imageUrl
          );
        const hasMetaContent = Boolean(
          parsed?.title?.trim() || parsed?.description?.trim()
        );
        if (hasCardsContent || hasMetaContent) {
          setTitle(parsed.title || '');
          setDescription(parsed.description || '');
          setCategoryId(parsed.categoryId || '');
          setVisibility(parsed.visibility || 'PUBLIC');
          setTermLang(parsed.termLang || '');
          setDefLang(parsed.defLang || '');
          setTags(Array.isArray(parsed.tags) ? parsed.tags : []);
          setSourceType(parsed.sourceType || 'MANUAL');
          if (Array.isArray(parsed.cards) && parsed.cards.length > 0) {
            setCards(parsed.cards.map((c) => createDraftCard(c)));
          }
          setDraftToastOpen(true);
        }
      }
    } catch {
      // Ignore malformed draft
    } finally {
      setDraftHydrated(true);
    }
  }, [isEditMode, prefillData, draftStorageKey]);

  // Auto-save draft whenever user edits in Create mode
  useEffect(() => {
    if (isEditMode || !draftHydrated) return;
    const hasContent =
      Boolean(title.trim() || description.trim()) ||
      cards.some((c) => c.term.trim() || c.definition.trim() || c.imageUrl);

    try {
      if (!hasContent) {
        localStorage.removeItem(draftStorageKey);
        return;
      }
      localStorage.setItem(
        draftStorageKey,
        JSON.stringify({
          title,
          description,
          categoryId,
          visibility,
          termLang,
          defLang,
          tags,
          sourceType,
          cards: cards.map(({ term, definition, hint, imageUrl, audioUrl, conceptTag, imageSide }) => ({
            term,
            definition,
            hint,
            imageUrl,
            audioUrl,
            conceptTag,
            imageSide,
          })),
        })
      );
    } catch {
      // Ignore storage quota errors
    }
  }, [
    isEditMode,
    draftHydrated,
    draftStorageKey,
    title,
    description,
    categoryId,
    visibility,
    termLang,
    defLang,
    tags,
    sourceType,
    cards,
  ]);

  useEffect(() => {
    let active = true;
    flashcardApi
      .explore()
      .then((res) => {
        if (!active) return;
        const cats = res?.data?.categories;
        if (Array.isArray(cats)) {
          setCategories(cats);
        }
      })
      .catch(() => {});
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (isEditMode || !prefillData) return;
    const normalized = normalizePrefillData(prefillData);
    if (!normalized) return;

    setTitle(normalized.title);
    setDescription(normalized.description);
    setCategoryId(normalized.categoryId);
    setVisibility(normalized.visibility);
    setTermLang(normalized.termLang);
    setDefLang(normalized.defLang);
    setTags(normalized.tags);
    setSourceType(normalized.sourceType);
    setCards(normalized.cards);
    setPrefillBannerVisible(true);
  }, [isEditMode, prefillData]);

  useEffect(() => {
    if (!isEditMode) return;
    let active = true;

    const loadSetForEdit = async () => {
      setLoadingPage(true);
      setErrorMsg('');
      setAccessDenied(false);
      try {
        const res = await flashcardApi.getSetById(id);
        if (!active) return;
        const set = res?.data;
        if (!set) {
          setErrorMsg('Không tìm thấy học phần.');
          return;
        }

        const isAdmin = user?.role === 'ADMIN';
        if (!set.ownerView && !isAdmin) {
          setAccessDenied(true);
          return;
        }

        setTitle(set.title || '');
        setDescription(set.description || '');
        setCategoryId(set.category?.id ? String(set.category.id) : '');
        setVisibility(set.visibility || 'PUBLIC');
        setTermLang(set.termLang || 'en');
        setDefLang(set.defLang || 'vi');
        setTags(Array.isArray(set.tags) ? set.tags : []);
        setSourceType(set.sourceType || 'MANUAL');
        setShareToken(set.shareToken || '');

        const existingCards =
          Array.isArray(set.cards) && set.cards.length > 0
            ? set.cards.map((c) =>
                createDraftCard({
                  id: c.id,
                  term: c.term || '',
                  definition: c.definition || '',
                  hint: c.hint || '',
                  imageUrl: c.imageUrl || '',
                  audioUrl: c.audioUrl || '',
                  conceptTag: c.conceptTag || '',
                })
              )
            : [createDraftCard()];
        setCards(existingCards);
      } catch (err) {
        if (!active) return;
        if (err?.code === 'FORBIDDEN' || err?.status === 403) {
          setAccessDenied(true);
        } else {
          setErrorMsg(extractApiErrorMessage(err, 'Không thể tải dữ liệu học phần để chỉnh sửa.'));
        }
      } finally {
        if (active) setLoadingPage(false);
      }
    };

    loadSetForEdit();
    return () => {
      active = false;
    };
  }, [id, isEditMode, user?.role]);

  const filledCardsCount = useMemo(
    () => cards.filter((c) => c.term.trim() || c.definition.trim()).length,
    [cards]
  );

  const displayedCards = useMemo(() => {
    const q = cardSearchQuery.trim().toLowerCase();
    return cards
      .map((card, index) => ({ card, index }))
      .filter(({ card }) => {
        if (!q) return true;
        return (
          card.term.toLowerCase().includes(q) ||
          card.definition.toLowerCase().includes(q) ||
          (card.hint || '').toLowerCase().includes(q)
        );
      });
  }, [cards, cardSearchQuery]);

  // Card handlers
  const handleCardChange = (index, field, value) => {
    setCards((prev) =>
      prev.map((c, i) => (i === index ? { ...c, [field]: value } : c))
    );
  };

  const handleApplySuggestion = (index, suggestion, typedSide) => {
    setCards((prev) =>
      prev.map((c, i) => {
        if (i !== index) return c;
        if (typedSide === 'term') {
          return {
            ...c,
            term: suggestion.term,
            definition: c.definition.trim() ? c.definition : suggestion.definition,
          };
        }
        return {
          ...c,
          definition: suggestion.definition,
          term: c.term.trim() ? c.term : suggestion.term,
        };
      })
    );
  };

  const handleAddCard = () => {
    setCards((prev) => [...prev, createDraftCard()]);
  };

  const handleInsertAfter = (index) => {
    setCards((prev) => {
      const next = [...prev];
      next.splice(index + 1, 0, createDraftCard());
      return next;
    });
  };

  const handleDeleteCard = (index) => {
    if (cards.length <= 1) return;
    setCards((prev) => prev.filter((_, i) => i !== index));
  };

  const handleMoveCard = (fromIdx, toIdx) => {
    if (fromIdx === toIdx || toIdx < 0 || toIdx >= cards.length) return;
    setCards((prev) => {
      const next = [...prev];
      const [moved] = next.splice(fromIdx, 1);
      next.splice(toIdx, 0, moved);
      return next;
    });
  };

  const handleDragStart = (e, index) => {
    setDragIdx(index);
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData('text/plain', String(index));
  };

  const handleDragOver = (e, index) => {
    e.preventDefault();
    if (dragOverIdx !== index) setDragOverIdx(index);
  };

  const handleDrop = (e, dropIndex) => {
    e.preventDefault();
    if (dragIdx !== null && dragIdx !== dropIndex) {
      handleMoveCard(dragIdx, dropIndex);
    }
    setDragIdx(null);
    setDragOverIdx(null);
  };

  const handleDragEnd = () => {
    setDragIdx(null);
    setDragOverIdx(null);
  };

  const handleSwapAllTermsAndDefs = () => {
    setCards((prev) =>
      prev.map((c) => ({
        ...c,
        term: c.definition,
        definition: c.term,
        imageSide: c.imageSide === 'term' ? 'definition' : 'term',
      }))
    );
    setTermLang(defLang);
    setDefLang(termLang);
  };

  const handleConfirmDelete = async () => {
    if (isEditMode) {
      setDeleting(true);
      try {
        await flashcardApi.deleteSet(id);
        setDeleteModalOpen(false);
        navigate('/library', { state: { toast: 'Đã xóa học phần.' } });
      } catch (err) {
        setErrorMsg(extractApiErrorMessage(err, 'Không thể xóa học phần.'));
        setDeleteModalOpen(false);
      } finally {
        setDeleting(false);
      }
      return;
    }

    setTitle('');
    setDescription('');
    setCards([createDraftCard(), createDraftCard()]);
    setShowValidationErrors(false);
    setErrorMsg('');
    setDraftToastOpen(false);
    try {
      localStorage.removeItem(draftStorageKey);
    } catch {}
    setDeleteModalOpen(false);
  };

  const mergeImportedCards = (incomingCards, { replaceExisting = false } = {}) => {
    const newDrafts = incomingCards.map((item) =>
      createDraftCard({
        term: item.term || '',
        definition: item.definition || '',
        hint: item.hint || '',
      })
    );

    if (replaceExisting) {
      setCards(newDrafts.length > 0 ? newDrafts : [createDraftCard()]);
      return;
    }

    setCards((prev) => {
      const hasOnlyBlankPlaceholders = prev.every(
        (c) => !c.term.trim() && !c.definition.trim() && !c.imageUrl
      );
      return hasOnlyBlankPlaceholders ? newDrafts : [...prev, ...newDrafts];
    });
  };

  const handleBulkImport = (importedCards, options) => {
    mergeImportedCards(importedCards, options);
    if (!isEditMode && sourceType === 'MANUAL') {
      setSourceType('IMPORT');
    }
  };

  const handleOcrImport = (extractedCards) => {
    mergeImportedCards(extractedCards, { replaceExisting: false });
    if (!isEditMode && sourceType === 'MANUAL') {
      setSourceType('OCR');
    }
  };

  const saveSet = async (afterAction = 'save') => {
    setErrorMsg('');

    const trimmedTitle = title.trim();
    const nonBlankCards = cards.filter(
      (c) => c.term.trim() || c.definition.trim() || c.imageUrl
    );
    const cardsToValidate = nonBlankCards.length > 0 ? nonBlankCards : cards;
    const hasIncompleteCard = cardsToValidate.some(
      (c) => !c.term.trim() || !c.definition.trim()
    );

    if (!trimmedTitle) {
      setShowValidationErrors(true);
      setErrorMsg('Vui lòng nhập tiêu đề cho học phần.');
      window.scrollTo({ top: 0, behavior: 'smooth' });
      return;
    }

    if (cardsToValidate.length === 0 || hasIncompleteCard) {
      setShowValidationErrors(true);
      setErrorMsg('Mỗi thẻ cần có đầy đủ Thuật ngữ và Định nghĩa.');
      return;
    }

    const payloadCards = cardsToValidate.map((c, idx) => ({
      ...(c.id ? { id: c.id } : {}),
      term: c.term.trim(),
      definition: c.definition.trim(),
      ...(c.hint?.trim() ? { hint: c.hint.trim() } : {}),
      ...(c.imageUrl?.trim() ? { imageUrl: c.imageUrl.trim() } : {}),
      ...(c.audioUrl?.trim() ? { audioUrl: c.audioUrl.trim() } : {}),
      ...(c.conceptTag?.trim() ? { conceptTag: c.conceptTag.trim() } : {}),
      orderIndex: idx,
    }));

    const basePayload = {
      title: trimmedTitle,
      description: description.trim() || null,
      categoryId: categoryId ? Number(categoryId) : null,
      visibility,
      termLang: termLang || 'en',
      defLang: defLang || 'vi',
      tags,
      cards: payloadCards,
    };

    setSavingMode(afterAction);
    try {
      const res = isEditMode
        ? await flashcardApi.updateSet(id, { ...basePayload, regenerateShareToken })
        : await flashcardApi.createSet({ ...basePayload, sourceType });

      const savedId = res?.data?.id || id;
      if (!isEditMode) {
        try {
          localStorage.removeItem(draftStorageKey);
        } catch {}
      }
      if (afterAction === 'study' && savedId) {
        navigate(`/sets/${savedId}`);
      } else {
        navigate('/library', {
          state: {
            toast: isEditMode
              ? `Đã lưu thay đổi cho "${trimmedTitle}".`
              : `Đã tạo học phần "${trimmedTitle}" thành công!`,
          },
        });
      }
    } catch (err) {
      setErrorMsg(extractApiErrorMessage(err, 'Không thể lưu học phần. Vui lòng thử lại.'));
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } finally {
      setSavingMode(null);
    }
  };

  if (loadingPage) {
    return (
      <MainLayout hideSidebar>
        <div className="min-h-[60vh] flex flex-col items-center justify-center gap-3 text-sm font-semibold text-[#586380] dark:text-[#A8B1D1]">
          <Loader2 className="w-7 h-7 animate-spin text-[#4255FF]" />
          Đang tải trình soạn thảo...
        </div>
      </MainLayout>
    );
  }

  if (accessDenied) {
    return (
      <MainLayout hideSidebar>
        <div className="max-w-lg mx-auto px-4 py-20 text-center">
          <div className="bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] p-8 shadow-sm space-y-4">
            <div className="w-12 h-12 rounded-2xl bg-rose-50 dark:bg-rose-900/25 text-rose-600 dark:text-rose-400 flex items-center justify-center mx-auto">
              <ShieldAlert className="w-6 h-6" />
            </div>
            <h2 className="text-xl font-extrabold text-[#282E3E] dark:text-white">
              Không có quyền chỉnh sửa
            </h2>
            <p className="text-sm text-[#586380] dark:text-[#A8B1D1]">
              Chỉ tác giả sở hữu học phần hoặc Quản trị viên mới có thể chỉnh sửa.
            </p>
            <Link
              to="/library"
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white text-sm font-bold transition"
            >
              <ArrowLeft className="w-4 h-4" />
              Quay lại Thư viện
            </Link>
          </div>
        </div>
      </MainLayout>
    );
  }

  const visMeta = VISIBILITY_META[visibility] || VISIBILITY_META.PUBLIC;
  const VisIcon = visMeta.icon;
  const selectedCategoryName = categories.find((c) => String(c.id) === String(categoryId))?.name;

  const handleDiscardDraftAndReset = () => {
    try {
      localStorage.removeItem(draftStorageKey);
    } catch {}
    setTitle('');
    setDescription('');
    setCategoryId('');
    setVisibility('PUBLIC');
    setTermLang('');
    setDefLang('');
    setTags([]);
    setSourceType('MANUAL');
    setCards([createDraftCard(), createDraftCard()]);
    setShowValidationErrors(false);
    setErrorMsg('');
    setDraftToastOpen(false);
  };

  return (
    <MainLayout hideSidebar stickyHeader={false}>
      {/* Sticky Top Title & Action Bar (pins to top-0 when MainLayout header scrolls away) */}
      <div
        className={`sticky top-0 z-30 bg-white/95 dark:bg-[#0A082D]/95 backdrop-blur-md transition-all ${
          isScrolled ? 'border-b border-[#EDEFF4] dark:border-[#2E3856] shadow-xs' : ''
        }`}
      >
        <div className="w-full max-w-[1440px] mx-auto px-4 sm:px-8 py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <h1 className="text-[20px] sm:text-[22px] font-extrabold tracking-tight text-[#282E3E] dark:text-white">
            {isEditMode ? 'Chỉnh sửa học phần' : 'Tạo một học phần mới'}
          </h1>

          <div className="flex items-center gap-2.5 shrink-0">
            <button
              type="button"
              disabled={savingMode !== null}
              onClick={() => saveSet('save')}
              className="h-10 px-5 rounded-full bg-[#EDEFFF] hover:bg-[#DFE3FF] dark:bg-[#202140] dark:hover:bg-[#2E3856] disabled:opacity-60 text-[#4255FF] dark:text-[#9AA7FF] text-sm font-bold inline-flex items-center gap-2 transition"
            >
              {savingMode === 'save' && <Loader2 className="w-4 h-4 animate-spin" />}
              {isEditMode ? 'Lưu' : 'Tạo'}
            </button>

            <button
              type="button"
              disabled={savingMode !== null}
              onClick={() => saveSet('study')}
              className="h-10 px-5 rounded-full bg-[#4255FF] hover:bg-[#3545E0] disabled:opacity-60 text-white text-sm font-bold inline-flex items-center gap-2 shadow-sm transition"
            >
              {savingMode === 'study' && <Loader2 className="w-4 h-4 animate-spin" />}
              {isEditMode ? 'Lưu và ôn luyện' : 'Tạo và ôn luyện'}
            </button>
          </div>
        </div>
      </div>

      <div className="w-full max-w-[1440px] mx-auto px-4 sm:px-8 pt-1 pb-24">
        {/* Visibility Pill Row (mb-7 = 28px gap down to Title textbox) */}
        <div className="mb-7 flex flex-wrap items-center gap-2.5">
          <button
            type="button"
            onClick={() => setAccessModalOpen(true)}
            className="inline-flex items-center gap-1.5 h-8 px-3.5 rounded-full bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#202140] dark:hover:bg-[#2E3856] text-[13px] font-bold text-[#282E3E] dark:text-[#E2E8F0] transition"
          >
            <VisIcon className="w-3.5 h-3.5 text-[#586380] dark:text-[#A8B1D1]" />
            <span>{visMeta.label}</span>
          </button>

          <span className="text-[13px] font-medium text-[#939BB4]">
            {filledCardsCount} thẻ{selectedCategoryName ? ` • ${selectedCategoryName}` : ''}
          </span>
        </div>

        {/* AI Chatbot Prefill Banner */}
        {prefillBannerVisible && (
          <div className="mb-5 p-3.5 rounded-xl bg-[#EDEFFF] dark:bg-[#202140] border border-[#4255FF]/25 flex items-start justify-between gap-3">
            <div className="flex items-start gap-2.5">
              <Sparkles className="w-4 h-4 text-[#4255FF] shrink-0 mt-0.5" />
              <p className="text-xs sm:text-sm font-semibold text-[#282E3E] dark:text-[#E2E8F0]">
                Đã điền sẵn dữ liệu từ AI Chatbot — hãy rà soát và bấm <strong>Tạo</strong> để lưu.
              </p>
            </div>
            <button
              type="button"
              onClick={() => setPrefillBannerVisible(false)}
              className="p-1 rounded-full text-[#586380] hover:bg-white/60 dark:hover:bg-[#2E3856]"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

        {/* Error Banner */}
        {errorMsg && (
          <div className="mb-5 p-3.5 rounded-xl bg-rose-50 dark:bg-rose-900/25 border border-rose-200 dark:border-rose-800/50 text-sm font-semibold text-rose-700 dark:text-rose-300 flex items-center gap-2.5">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{errorMsg}</span>
          </div>
        )}

        {/* Title & Description Inputs — identical h-12 size, mb-7 (28px) down to toolbar */}
        <div className="mb-7 space-y-3">
          <input
            type="text"
            maxLength={200}
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Tiêu đề"
            className={`w-full h-12 px-4 rounded-xl bg-[#F6F7FB] dark:bg-[#202140] border-2 text-[15px] font-semibold text-[#282E3E] dark:text-white placeholder:text-[#586380] dark:placeholder:text-[#A8B1D1] outline-none transition ${
              showValidationErrors && !title.trim()
                ? 'border-rose-500'
                : 'border-transparent focus:border-[#4255FF] focus:bg-white dark:focus:bg-[#171836]'
            }`}
          />

          <input
            type="text"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Thêm mô tả..."
            className="w-full h-12 px-4 rounded-xl bg-[#F6F7FB] dark:bg-[#202140] border-2 border-transparent focus:border-[#4255FF] focus:bg-white dark:focus:bg-[#171836] text-[15px] font-medium text-[#282E3E] dark:text-white placeholder:text-[#939BB4] outline-none transition"
          />
        </div>

        {/* Quizlet Action Toolbar Row (mb-7 = 28px down to Card 1; NO Keyboard button, NO Thêm sơ đồ) */}
        <div className="mb-7 flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap items-center gap-2.5">
            <button
              type="button"
              onClick={() => setBulkModalOpen(true)}
              className="inline-flex items-center gap-1.5 h-10 px-4 rounded-full bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#202140] dark:hover:bg-[#2E3856] text-[13px] font-bold text-[#282E3E] dark:text-white transition"
            >
              <Plus className="w-4 h-4 text-[#586380] dark:text-[#A8B1D1]" />
              <span>Nhập</span>
            </button>

            <button
              type="button"
              onClick={() => setOcrModalOpen(true)}
              className="inline-flex items-center gap-1.5 h-10 px-4 rounded-full bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#202140] dark:hover:bg-[#2E3856] text-[13px] font-bold text-[#282E3E] dark:text-white transition"
            >
              <ScanText className="w-4 h-4 text-[#4255FF]" />
              <span>Quét ảnh OCR</span>
            </button>

            <button
              type="button"
              onClick={() => setAiHelperNotice((p) => !p)}
              className="inline-flex items-center gap-1.5 h-10 px-4 rounded-full bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#202140] dark:hover:bg-[#2E3856] text-[13px] font-bold text-[#282E3E] dark:text-white transition"
            >
              <Sparkles className="w-4 h-4 text-[#586380] dark:text-[#A8B1D1]" />
              <span>Trợ lý thông minh</span>
            </button>
          </div>

          {/* Right Controls: Gợi ý (autocomplete toggle) + Search + Swap (Lật) + Red Delete */}
          <div className="flex items-center gap-2.5">
            <button
              type="button"
              role="switch"
              aria-checked={autoSuggestEnabled}
              onClick={() => setAutoSuggestEnabled((p) => !p)}
              title="Bật/tắt gợi ý từ vựng khi đang gõ"
              className="inline-flex items-center gap-2 mr-1 cursor-pointer select-none"
            >
              <span className="text-[13px] font-bold text-[#282E3E] dark:text-white">
                Gợi ý
              </span>
              <span
                className={`w-10 h-6 rounded-full p-0.5 transition-colors inline-flex items-center ${
                  autoSuggestEnabled ? 'bg-[#4255FF]' : 'bg-[#D9DDE8] dark:bg-[#2E3856]'
                }`}
              >
                <span
                  className={`w-5 h-5 rounded-full bg-white shadow transition-transform ${
                    autoSuggestEnabled ? 'translate-x-4' : 'translate-x-0'
                  }`}
                />
              </span>
            </button>

            <button
              type="button"
              onClick={() => setSearchOpen((p) => !p)}
              title="Tìm thẻ trong bộ"
              aria-label="Tìm thẻ"
              className={`w-9 h-9 rounded-full flex items-center justify-center transition ${
                searchOpen
                  ? 'bg-[#EDEFFF] text-[#4255FF] dark:bg-[#2E3856] dark:text-[#8E9BFF]'
                  : 'bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#202140] dark:hover:bg-[#2E3856] text-[#586380] dark:text-[#A8B1D1]'
              }`}
            >
              <Search className="w-4 h-4" />
            </button>

            <button
              type="button"
              onClick={handleSwapAllTermsAndDefs}
              title="Lật Thuật ngữ ↔ Định nghĩa"
              aria-label="Lật thuật ngữ và định nghĩa"
              className="w-9 h-9 rounded-full bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#202140] dark:hover:bg-[#2E3856] text-[#586380] dark:text-[#A8B1D1] flex items-center justify-center transition"
            >
              <ArrowLeftRight className="w-4 h-4" />
            </button>

            <button
              type="button"
              onClick={() => setDeleteModalOpen(true)}
              title={isEditMode ? 'Xóa học phần này' : 'Xóa học phần đang tạo'}
              aria-label="Xóa học phần"
              className="w-9 h-9 rounded-full bg-[#C81E32] hover:bg-[#A91829] text-white flex items-center justify-center transition"
            >
              <Trash2 className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Inline Smart Assistant Notice */}
        {aiHelperNotice && (
          <div className="mb-7 p-3.5 rounded-xl bg-[#F6F7FB] dark:bg-[#202140] border border-[#EDEFF4] dark:border-[#2E3856] flex items-center justify-between gap-3 text-xs sm:text-sm text-[#586380] dark:text-[#A8B1D1]">
            <span>
              <strong className="text-[#282E3E] dark:text-white">Trợ lý thông minh:</strong>{' '}
              Bật công tắc <strong>Gợi ý</strong> để hiện gợi ý từ vựng khi gõ, hoặc bấm <strong>Hình ảnh</strong> trên thẻ để tìm ảnh minh họa / tạo ảnh AI.
            </span>
            <button
              type="button"
              onClick={() => setAiHelperNotice(false)}
              className="p-1 rounded-full hover:bg-[#EDEFF4] dark:hover:bg-[#2E3856]"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

        {/* Inline Card Search Bar */}
        {searchOpen && (
          <div className="mb-7 relative">
            <Search className="w-4 h-4 text-[#586380] absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
            <input
              type="text"
              value={cardSearchQuery}
              onChange={(e) => setCardSearchQuery(e.target.value)}
              placeholder="Tìm kiếm trong các thẻ đang soạn..."
              className="w-full h-10 pl-10 pr-10 rounded-xl bg-[#F6F7FB] dark:bg-[#202140] border border-[#EDEFF4] dark:border-[#2E3856] focus:border-[#4255FF] text-sm font-medium text-[#282E3E] dark:text-white outline-none transition"
            />
            {cardSearchQuery && (
              <button
                type="button"
                onClick={() => setCardSearchQuery('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-[#586380] hover:text-[#282E3E]"
              >
                <X className="w-4 h-4" />
              </button>
            )}
          </div>
        )}

        {/* Card Rows */}
        <div className="space-y-5">
          {displayedCards.length === 0 ? (
            <div className="py-12 text-center rounded-2xl bg-[#F6F7FB] dark:bg-[#202140] text-sm font-semibold text-[#586380] dark:text-[#A8B1D1]">
              Không tìm thấy thẻ nào khớp với "{cardSearchQuery}".
            </div>
          ) : (
            displayedCards.map(({ card, index }) => (
              <CardEditorRow
                key={card.clientKey}
                card={card}
                index={index}
                total={cards.length}
                allCards={cards}
                termLang={termLang}
                defLang={defLang}
                onChangeTermLang={setTermLang}
                onChangeDefLang={setDefLang}
                autoSuggestEnabled={autoSuggestEnabled}
                isDragging={dragIdx === index}
                isDragOver={dragOverIdx === index && dragIdx !== index}
                showValidationErrors={showValidationErrors}
                onChange={handleCardChange}
                onApplySuggestion={handleApplySuggestion}
                onDelete={handleDeleteCard}
                onMoveUp={(i) => handleMoveCard(i, i - 1)}
                onMoveDown={(i) => handleMoveCard(i, i + 1)}
                onInsertAfter={handleInsertAfter}
                onDragStart={handleDragStart}
                onDragOver={handleDragOver}
                onDrop={handleDrop}
                onDragEnd={handleDragEnd}
              />
            ))
          )}
        </div>

        {/* Centered Quizlet "Thêm thẻ" pill */}
        <div className="pt-7 flex justify-center">
          <button
            type="button"
            onClick={handleAddCard}
            className="h-11 px-8 rounded-full bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#202140] dark:hover:bg-[#2E3856] text-[14px] font-bold text-[#282E3E] dark:text-white shadow-[0_1px_4px_rgba(40,46,62,0.06)] transition"
          >
            Thêm thẻ
          </button>
        </div>
      </div>

      {/* Modals */}
      <BulkImportModal
        open={bulkModalOpen}
        onClose={() => setBulkModalOpen(false)}
        onImportCards={handleBulkImport}
      />

      <OcrScanModal
        open={ocrModalOpen}
        onClose={() => setOcrModalOpen(false)}
        defaultSourceLang={termLang}
        defaultTargetLang={defLang}
        onAddExtractedCards={handleOcrImport}
      />

      <AccessControlModal
        open={accessModalOpen}
        onClose={() => setAccessModalOpen(false)}
        visibility={visibility}
        onSaveVisibility={setVisibility}
        isEditMode={isEditMode}
        setId={id}
        shareToken={shareToken}
        regenerateShareToken={regenerateShareToken}
        setRegenerateShareToken={setRegenerateShareToken}
      />

      <DeleteSetConfirmModal
        open={deleteModalOpen}
        isEditMode={isEditMode}
        loading={deleting}
        onClose={() => setDeleteModalOpen(false)}
        onConfirm={handleConfirmDelete}
      />

      {/* Bottom-right Restored Draft Toast */}
      {draftToastOpen && (
        <div className="fixed bottom-6 right-6 z-40 max-w-md w-[calc(100vw-3rem)] rounded-2xl bg-[#1F2437] text-white px-5 py-4 shadow-2xl border border-white/10 space-y-3">
          <p className="text-[14px] font-bold leading-snug">
            Chúng tôi đã khôi phục bản nháp gần nhất của bạn. Hãy tiếp tục tại điểm bạn đã tạm dừng!
          </p>
          <div className="flex items-center justify-end gap-4">
            <button
              type="button"
              onClick={handleDiscardDraftAndReset}
              className="text-[13px] font-bold text-white/90 hover:text-white transition"
            >
              Tạo học phần mới
            </button>
            <button
              type="button"
              onClick={() => setDraftToastOpen(false)}
              className="text-[13px] font-bold text-white hover:underline transition"
            >
              Tiếp tục sửa bản nháp
            </button>
          </div>
        </div>
      )}
    </MainLayout>
  );
}

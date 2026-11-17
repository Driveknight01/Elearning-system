import React, { useEffect, useState } from 'react';
import {
  Check,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Keyboard,
  Layers,
  Lightbulb,
  Minimize2,
  Settings,
  Sparkles,
  Undo2,
  Volume2,
  X,
} from 'lucide-react';

/**
 * Quizlet-style Fullscreen Flashcards Study Modal (matches reference Image 2).
 */
export default function FullscreenFlashcardsModal({
  open,
  title = '',
  cards = [],
  termLang = 'en',
  defLang = 'vi',
  startWithDef = false,
  autoSpeak = false,
  tts,
  onOpenOptions,
  onSwitchToLearn,
  onClose,
}) {
  const [index, setIndex] = useState(0);
  const [flipCount, setFlipCount] = useState(0);
  const [skipTransition, setSkipTransition] = useState(false);
  const [slideAnim, setSlideAnim] = useState('');
  const [slideKey, setSlideKey] = useState(0);
  const [showHint, setShowHint] = useState(false);

  const [trackProgress, setTrackProgress] = useState(true);
  const [learningIds, setLearningIds] = useState([]);
  const [knownIds, setKnownIds] = useState([]);
  const [history, setHistory] = useState([]);

  const isFlipped = flipCount % 2 === 1;
  const activeCard = cards[index] || null;

  useEffect(() => {
    if (!open) return;
    setIndex(0);
    setFlipCount(0);
    setShowHint(false);
    setLearningIds([]);
    setKnownIds([]);
    setHistory([]);
  }, [open, cards.length]);

  const resetFront = (direction) => {
    setSkipTransition(true);
    setFlipCount(0);
    setShowHint(false);
    setSlideAnim(direction === 'prev' ? 'animate-slide-prev' : 'animate-slide-next');
    setSlideKey((k) => k + 1);
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        setSkipTransition(false);
      });
    });
  };

  const handleFlip = () => {
    setSkipTransition(false);
    setFlipCount((prev) => prev + 1);
  };

  // Toggle hint inline without flipping the card
  const handleToggleHint = (e) => {
    e.stopPropagation();
    setShowHint((prev) => !prev);
  };

  const getFrontText = (card) => (startWithDef ? card.definition : card.term);
  const getBackText = (card) => (startWithDef ? card.term : card.definition);
  const getFrontLang = () => (startWithDef ? defLang : termLang);
  const getBackLang = () => (startWithDef ? termLang : defLang);

  const getHintText = (card) => {
    if (!card) return '';
    if (card.hint && card.hint.trim()) return card.hint.trim();
    const target = getBackText(card) || '';
    const firstWord = target.split(/\s+/)[0] || '';
    return firstWord ? `${firstWord.slice(0, 3)}... (${target.length} ký tự)` : '';
  };

  useEffect(() => {
    if (!open || !autoSpeak || !activeCard || !tts?.isSupported) return;
    const text = isFlipped ? getBackText(activeCard) : getFrontText(activeCard);
    const lang = isFlipped ? getBackLang() : getFrontLang();
    tts.speak(text, lang, `fs-${activeCard.id}-${isFlipped ? 'b' : 'f'}`);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, autoSpeak, index, flipCount, activeCard?.id]);

  const goNext = () => {
    if (cards.length <= 1) return;
    setIndex((prev) => (prev + 1 < cards.length ? prev + 1 : 0));
    resetFront('next');
  };

  const goPrev = () => {
    if (cards.length <= 1) return;
    setIndex((prev) => (prev > 0 ? prev - 1 : cards.length - 1));
    resetFront('prev');
  };

  const markCard = (status) => {
    if (!activeCard) return;
    setHistory((prev) => [...prev, { index, cardId: activeCard.id, status }]);
    if (status === 'learning') {
      setLearningIds((prev) => (prev.includes(activeCard.id) ? prev : [...prev, activeCard.id]));
      setKnownIds((prev) => prev.filter((id) => id !== activeCard.id));
    } else {
      setKnownIds((prev) => (prev.includes(activeCard.id) ? prev : [...prev, activeCard.id]));
      setLearningIds((prev) => prev.filter((id) => id !== activeCard.id));
    }
    goNext();
  };

  const handleUndo = () => {
    if (history.length === 0) return;
    const last = history[history.length - 1];
    setHistory((prev) => prev.slice(0, -1));
    setLearningIds((prev) => prev.filter((id) => id !== last.cardId));
    setKnownIds((prev) => prev.filter((id) => id !== last.cardId));
    setIndex(last.index);
    resetFront('prev');
  };

  useEffect(() => {
    if (!open) return undefined;
    const onKeyDown = (e) => {
      if (e.key === 'Escape') {
        onClose();
      } else if (e.code === 'Space') {
        e.preventDefault();
        handleFlip();
      } else if (e.key === 'ArrowRight') {
        e.preventDefault();
        goNext();
      } else if (e.key === 'ArrowLeft') {
        e.preventDefault();
        goPrev();
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, index, cards.length]);

  if (!open || !activeCard) return null;

  return (
    <div className="fixed inset-0 z-50 bg-white dark:bg-[#0A082D] flex flex-col">
      {/* Clean Top Bar (No bottom border, matches Image 2) */}
      <header className="px-6 sm:px-10 h-16 flex items-center justify-between gap-4 bg-white dark:bg-[#0A082D]">
        <button
          type="button"
          onClick={onClose}
          className="inline-flex items-center gap-2 text-sm font-extrabold text-[#282E3E] dark:text-white hover:opacity-80 transition"
        >
          <Layers className="w-5 h-5 text-[#4255FF]" />
          <span>Thẻ ghi nhớ</span>
          <ChevronDown className="w-4 h-4 text-[#586380]" />
        </button>

        <div className="text-center min-w-0">
          <p className="text-sm font-extrabold text-[#282E3E] dark:text-white tabular-nums">
            {index + 1} / {cards.length}
          </p>
          <p className="text-xs font-semibold text-[#586380] dark:text-[#A8B1D1] truncate max-w-[260px] sm:max-w-md">
            {title}
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => {
              onClose();
              onSwitchToLearn?.();
            }}
            className="hidden sm:inline-flex items-center gap-1.5 h-9 px-4 rounded-full bg-[#F6F7FB] hover:bg-[#EDEFFF] dark:bg-[#202140] dark:hover:bg-[#2E3856] text-xs font-bold text-[#282E3E] dark:text-white transition"
          >
            <Sparkles className="w-3.5 h-3.5 text-[#4255FF]" />
            Biến thuật ngữ thành câu hỏi
          </button>

          <button
            type="button"
            onClick={onOpenOptions}
            aria-label="Tùy chọn"
            className="w-9 h-9 rounded-full hover:bg-[#F6F7FB] dark:hover:bg-[#202140] text-[#586380] dark:text-[#A8B1D1] flex items-center justify-center transition"
          >
            <Settings className="w-4.5 h-4.5" />
          </button>

          <button
            type="button"
            onClick={onClose}
            aria-label="Đóng toàn màn hình"
            className="w-9 h-9 rounded-full hover:bg-[#F6F7FB] dark:hover:bg-[#202140] text-[#586380] dark:text-[#A8B1D1] flex items-center justify-center transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
      </header>

      {/* Main Content */}
      <div className="flex-1 flex flex-col items-center justify-center px-4 sm:px-8 pb-6 overflow-y-auto">
        <div className="w-full max-w-[960px] space-y-3.5">
          {/* Learning / Known Counters */}
          {trackProgress && (
            <div className="flex items-center justify-between text-xs font-extrabold px-1">
              <div className="flex items-center gap-2 text-[#D97706]">
                <span className="px-3 py-0.5 rounded-full bg-orange-50 dark:bg-orange-950/50 border border-orange-300 dark:border-orange-700">
                  {learningIds.length}
                </span>
                <span>Đang học</span>
              </div>

              <div className="flex items-center gap-2 text-[#10B981]">
                <span>Đã biết</span>
                <span className="px-3 py-0.5 rounded-full bg-emerald-50 dark:bg-emerald-950/50 border border-emerald-300 dark:border-emerald-700">
                  {knownIds.length}
                </span>
              </div>
            </div>
          )}

          {/* 3D Continuous Flip Card */}
          <div key={slideKey} className={slideAnim}>
            <div
              role="button"
              tabIndex={0}
              onClick={handleFlip}
              className="perspective-1000 w-full h-[440px] sm:h-[500px] cursor-pointer select-none focus:outline-none"
            >
              <div
                className={`relative w-full h-full transform-style-3d ${
                  skipTransition ? 'transition-none' : 'transition-transform duration-500 ease-out'
                }`}
                style={{ transform: `rotateX(${flipCount * 180}deg)` }}
              >
                {/* FRONT */}
                <div className="absolute inset-0 backface-hidden bg-white dark:bg-[#202140] rounded-3xl border border-[#EDEFF4]/80 dark:border-[#2E3856] shadow-[0_10px_40px_rgba(40,46,62,0.08)] flex flex-col justify-between overflow-hidden">
                  <div className="px-7 pt-6 flex items-center justify-between">
                    <button
                      type="button"
                      onClick={handleToggleHint}
                      className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold transition ${
                        showHint
                          ? 'bg-amber-100 text-amber-800 dark:bg-amber-900/50 dark:text-amber-200'
                          : 'text-[#586380] hover:bg-[#F6F7FB] dark:text-[#A8B1D1] dark:hover:bg-[#2E3856]'
                      }`}
                    >
                      <Lightbulb className="w-4 h-4" />
                      <span>{showHint ? 'Ẩn gợi ý' : 'Hiển thị gợi ý'}</span>
                    </button>

                    {tts?.isSupported && (
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          tts.speak(getFrontText(activeCard), getFrontLang(), `fs-f-${activeCard.id}`);
                        }}
                        className="w-9 h-9 rounded-full text-[#586380] hover:bg-[#EDEFFF] hover:text-[#4255FF] dark:text-[#A8B1D1] flex items-center justify-center transition"
                      >
                        <Volume2 className="w-4.5 h-4.5" />
                      </button>
                    )}
                  </div>

                  <div className="flex-1 flex flex-col items-center justify-center px-8 text-center gap-4">
                    <p className="text-3xl sm:text-4xl font-normal text-[#282E3E] dark:text-white break-words">
                      {getFrontText(activeCard)}
                    </p>
                    {showHint && (
                      <p className="px-4 py-2 rounded-xl bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800 text-xs sm:text-sm font-semibold text-amber-800 dark:text-amber-200">
                        Gợi ý: {getHintText(activeCard)}
                      </p>
                    )}
                  </div>

                  <div className="bg-[#DCE2FF]/80 dark:bg-[#2E3856] py-2.5 px-4 flex items-center justify-center gap-2 text-xs font-semibold text-[#282E3E] dark:text-[#E2E8F0]">
                    <Keyboard className="w-4 h-4 text-amber-600 dark:text-amber-300" />
                    <span>Phím tắt: Nhấn</span>
                    <kbd className="px-2 py-0.5 rounded bg-white dark:bg-[#171936] border border-[#C7D0FF] dark:border-[#3A466B] text-[11px] font-bold">
                      phím cách
                    </kbd>
                    <span>hoặc nhấp vào thẻ để lật</span>
                  </div>
                </div>

                {/* BACK */}
                <div className="absolute inset-0 backface-hidden rotate-x-180 bg-white dark:bg-[#202140] rounded-3xl border border-[#EDEFF4]/80 dark:border-[#2E3856] shadow-[0_10px_40px_rgba(40,46,62,0.08)] flex flex-col justify-between overflow-hidden">
                  <div className="px-7 pt-6 flex items-center justify-between">
                    <span className="text-xs font-bold uppercase tracking-wider text-[#4255FF] dark:text-[#8E9BFF]">
                      Mặt sau
                    </span>

                    {tts?.isSupported && (
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          tts.speak(getBackText(activeCard), getBackLang(), `fs-b-${activeCard.id}`);
                        }}
                        className="w-9 h-9 rounded-full text-[#586380] hover:bg-[#EDEFFF] hover:text-[#4255FF] dark:text-[#A8B1D1] flex items-center justify-center transition"
                      >
                        <Volume2 className="w-4.5 h-4.5" />
                      </button>
                    )}
                  </div>

                  <div className="flex-1 flex flex-col items-center justify-center px-8 text-center gap-4">
                    <p className="text-2xl sm:text-3xl font-normal text-[#282E3E] dark:text-white break-words">
                      {getBackText(activeCard)}
                    </p>
                    {showHint && (
                      <p className="px-4 py-2 rounded-xl bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800 text-xs font-semibold text-amber-800 dark:text-amber-200">
                        Gợi ý: {getHintText(activeCard)}
                      </p>
                    )}
                  </div>

                  <div className="h-10" />
                </div>
              </div>
            </div>
          </div>

          {/* Bottom Controls */}
          <div className="pt-2 flex items-center justify-between gap-4">
            <label className="inline-flex items-center gap-2.5 cursor-pointer select-none">
              <span className="text-xs font-extrabold text-[#4255FF] dark:text-[#8E9BFF]">
                Theo dõi tiến độ
              </span>
              <button
                type="button"
                onClick={() => setTrackProgress((p) => !p)}
                className={`w-10 h-5.5 rounded-full p-0.5 transition-colors ${
                  trackProgress ? 'bg-[#4255FF]' : 'bg-[#D9DDE8] dark:bg-[#2E3856]'
                }`}
              >
                <span
                  className={`block w-4.5 h-4.5 rounded-full bg-white shadow transition-transform ${
                    trackProgress ? 'translate-x-4.5' : 'translate-x-0'
                  }`}
                />
              </button>
            </label>

            {trackProgress ? (
              <div className="flex items-center gap-3">
                <button
                  type="button"
                  onClick={() => markCard('learning')}
                  title="Đang học"
                  className="w-20 h-12 rounded-full bg-[#F6F7FB] dark:bg-[#202140] hover:bg-orange-50 dark:hover:bg-orange-950/30 text-[#EA580C] flex items-center justify-center transition"
                >
                  <X className="w-5 h-5 stroke-[2.5]" />
                </button>
                <button
                  type="button"
                  onClick={() => markCard('known')}
                  title="Đã biết"
                  className="w-20 h-12 rounded-full bg-[#F6F7FB] dark:bg-[#202140] hover:bg-emerald-50 dark:hover:bg-emerald-950/30 text-[#10B981] flex items-center justify-center transition"
                >
                  <Check className="w-5 h-5 stroke-[2.5]" />
                </button>
              </div>
            ) : (
              <div className="flex items-center gap-3">
                <button
                  type="button"
                  onClick={goPrev}
                  className="w-12 h-12 rounded-full bg-[#F6F7FB] dark:bg-[#202140] flex items-center justify-center text-[#586380] dark:text-white"
                >
                  <ChevronLeft className="w-5 h-5" />
                </button>
                <button
                  type="button"
                  onClick={goNext}
                  className="w-12 h-12 rounded-full bg-[#F6F7FB] dark:bg-[#202140] flex items-center justify-center text-[#586380] dark:text-white"
                >
                  <ChevronRight className="w-5 h-5" />
                </button>
              </div>
            )}

            <div className="flex items-center gap-2">
              <button
                type="button"
                disabled={history.length === 0}
                onClick={handleUndo}
                title="Hoàn tác"
                className="w-10 h-10 rounded-full text-[#586380] hover:bg-[#F6F7FB] dark:text-[#A8B1D1] dark:hover:bg-[#202140] disabled:opacity-40 flex items-center justify-center transition"
              >
                <Undo2 className="w-4.5 h-4.5" />
              </button>

              <button
                type="button"
                onClick={onClose}
                title="Thu nhỏ"
                className="w-10 h-10 rounded-full bg-[#F6F7FB] dark:bg-[#202140] text-[#586380] dark:text-[#A8B1D1] flex items-center justify-center transition"
              >
                <Minimize2 className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

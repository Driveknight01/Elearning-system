import React, { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  ArrowLeft,
  ArrowRight,
  Edit3,
  Lightbulb,
  Maximize2,
  Pause,
  Play,
  Settings,
  Shuffle,
  Star,
  Volume2,
} from 'lucide-react';
import SortFlashcardsModal from './SortFlashcardsModal';
import DeckOptionsModal from './DeckOptionsModal';
import FullscreenFlashcardsModal from './FullscreenFlashcardsModal';

export default function CardPreviewDeck({
  setId,
  setTitle = '',
  isOwner = false,
  cards = [],
  termLang = 'en',
  defLang = 'vi',
  starredCardIds = [],
  togglingStarId = null,
  onToggleStar,
  onSelectLearnMode,
  tts,
}) {
  const [currentIndex, setCurrentIndex] = useState(0);
  // Continuous circular flip: increments 0 -> 1 -> 2 -> 3...
  const [flipCount, setFlipCount] = useState(0);
  const [skipTransition, setSkipTransition] = useState(false);

  // Horizontal slide animation state
  const [slideDirection, setSlideDirection] = useState('');
  const [slideKey, setSlideKey] = useState(0);

  const [showHint, setShowHint] = useState(false);
  const [isShuffled, setIsShuffled] = useState(false);
  const [cardStatusBanner, setCardStatusBanner] = useState(null);
  const [isPlaying, setIsPlaying] = useState(false);

  // Deck options
  const [autoSpeak, setAutoSpeak] = useState(false);
  const [startWithDef, setStartWithDef] = useState(false);
  const [shuffledOrder, setShuffledOrder] = useState([]);

  // Modals
  const [sortModalOpen, setSortModalOpen] = useState(false);
  const [sortMode, setSortMode] = useState('browse');
  const [optionsModalOpen, setOptionsModalOpen] = useState(false);
  const [fullscreenOpen, setFullscreenOpen] = useState(false);

  const isFlipped = flipCount % 2 === 1;

  const displayCards = useMemo(() => {
    if (!isShuffled || shuffledOrder.length !== cards.length) {
      return cards;
    }
    return shuffledOrder.map((idx) => cards[idx]).filter(Boolean);
  }, [cards, isShuffled, shuffledOrder]);

  const resetToFrontWithSlide = (direction) => {
    setSkipTransition(true);
    setFlipCount(0);
    setShowHint(false);
    if (direction) {
      setSlideDirection(direction === 'prev' ? 'animate-slide-prev' : 'animate-slide-next');
      setSlideKey((k) => k + 1);
    }
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        setSkipTransition(false);
      });
    });
  };

  useEffect(() => {
    setCurrentIndex(0);
    resetToFrontWithSlide('');
  }, [cards.length]);

  // Clear card bottom status banner after 2.4s
  useEffect(() => {
    if (!cardStatusBanner) return undefined;
    const t = setTimeout(() => setCardStatusBanner(null), 2400);
    return () => clearTimeout(t);
  }, [cardStatusBanner]);

  const safeIndex = displayCards.length > 0 ? Math.min(currentIndex, displayCards.length - 1) : 0;
  const activeCard = displayCards[safeIndex] || null;
  const isStarred = activeCard ? starredCardIds.includes(activeCard.id) : false;

  const getFrontText = (card) => (startWithDef ? card.definition : card.term);
  const getBackText = (card) => (startWithDef ? card.term : card.definition);
  const getFrontLang = () => (startWithDef ? defLang : termLang);
  const getBackLang = () => (startWithDef ? termLang : defLang);

  const getHintText = (card) => {
    if (!card) return '';
    if (card.hint && card.hint.trim()) return card.hint.trim();
    const back = getBackText(card) || '';
    const firstWord = back.split(/\s+/)[0] || '';
    return firstWord ? `${firstWord.slice(0, 3)}... (${back.length} ký tự)` : '';
  };

  // Auto-TTS when switching cards or flipping
  useEffect(() => {
    if (!autoSpeak || !activeCard || !tts?.isSupported || fullscreenOpen) return;
    const text = isFlipped ? getBackText(activeCard) : getFrontText(activeCard);
    const lang = isFlipped ? getBackLang() : getFrontLang();
    const key = `deck-${activeCard.id}-${isFlipped ? 'b' : 'f'}`;
    tts.speak(text, lang, key);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [autoSpeak, safeIndex, flipCount, activeCard?.id, fullscreenOpen]);

  // Auto-Play interval when `isPlaying` is true
  useEffect(() => {
    if (!isPlaying || displayCards.length <= 1 || fullscreenOpen) return undefined;
    const interval = setInterval(() => {
      setCurrentIndex((prev) => (prev < displayCards.length - 1 ? prev + 1 : 0));
      resetToFrontWithSlide('next');
    }, 3500);
    return () => clearInterval(interval);
  }, [isPlaying, displayCards.length, fullscreenOpen]);

  const handleToggleShuffle = () => {
    if (!isShuffled) {
      const indices = cards.map((_, i) => i);
      for (let i = indices.length - 1; i > 0; i -= 1) {
        const j = Math.floor(Math.random() * (i + 1));
        [indices[i], indices[j]] = [indices[j], indices[i]];
      }
      setShuffledOrder(indices);
      setIsShuffled(true);
      setCardStatusBanner('Trộn thẻ đang BẬT');
    } else {
      setIsShuffled(false);
      setCardStatusBanner('Trộn thẻ đang TẮT');
    }
    setCurrentIndex(0);
    resetToFrontWithSlide('next');
  };

  const handleTogglePlay = () => {
    setIsPlaying((prev) => {
      const next = !prev;
      setCardStatusBanner(next ? 'Tự động phát đang BẬT' : 'Tự động phát đang TẮT');
      return next;
    });
  };

  const goPrev = () => {
    if (displayCards.length <= 1) return;
    setCurrentIndex((prev) => (prev > 0 ? prev - 1 : displayCards.length - 1));
    resetToFrontWithSlide('prev');
  };

  const goNext = () => {
    if (displayCards.length <= 1) return;
    setCurrentIndex((prev) => (prev < displayCards.length - 1 ? prev + 1 : 0));
    resetToFrontWithSlide('next');
  };

  const handleFlip = () => {
    setSkipTransition(false);
    setFlipCount((prev) => prev + 1);
  };

  // Clicking "Hiển thị gợi ý" toggles hint at top-left WITHOUT flipping the card
  const handleToggleHint = (e) => {
    e.stopPropagation();
    setShowHint((prev) => !prev);
  };

  // Keyboard shortcuts when fullscreen is closed
  useEffect(() => {
    if (fullscreenOpen || sortModalOpen || optionsModalOpen) return undefined;
    const onKeyDown = (e) => {
      const tag = e.target?.tagName?.toLowerCase();
      if (tag === 'input' || tag === 'textarea' || tag === 'select' || e.target?.isContentEditable) {
        return;
      }
      if (!activeCard) return;

      if (e.code === 'Space') {
        e.preventDefault();
        handleFlip();
      } else if (e.key === 'ArrowLeft') {
        e.preventDefault();
        goPrev();
      } else if (e.key === 'ArrowRight') {
        e.preventDefault();
        goNext();
      } else if (e.key === 'a' || e.key === 'A') {
        e.preventDefault();
        const text = isFlipped ? getBackText(activeCard) : getFrontText(activeCard);
        const lang = isFlipped ? getBackLang() : getFrontLang();
        tts?.speak(text, lang, `deck-${activeCard.id}-${isFlipped ? 'b' : 'f'}`);
      } else if (e.key === 's' || e.key === 'S') {
        e.preventDefault();
        onToggleStar?.(activeCard);
      }
    };

    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeCard, isFlipped, displayCards.length, fullscreenOpen, sortModalOpen, optionsModalOpen]);

  if (!activeCard) {
    return (
      <div className="bg-white dark:bg-[#202140] rounded-3xl border border-[#EDEFF4] dark:border-[#2E3856] p-10 text-center">
        <p className="text-sm font-semibold text-[#586380] dark:text-[#A8B1D1]">
          Học phần này chưa có thẻ nào.
        </p>
      </div>
    );
  }

  const frontSpeakKey = `deck-${activeCard.id}-front`;
  const backSpeakKey = `deck-${activeCard.id}-back`;

  return (
    <div className="space-y-4">
      {/* Horizontal Slide Wrapper + 3D Continuous Circular Flip Card */}
      <div key={slideKey} className={slideDirection}>
        <div
          role="button"
          tabIndex={0}
          onClick={handleFlip}
          onKeyDown={(e) => {
            if (e.key === 'Enter') handleFlip();
          }}
          aria-label="Lật thẻ flashcard"
          className="perspective-1000 w-full h-[350px] sm:h-[400px] cursor-pointer select-none focus:outline-none"
        >
          <div
            className={`relative w-full h-full transform-style-3d ${
              skipTransition ? 'transition-none' : 'transition-transform duration-500 ease-out'
            }`}
            style={{ transform: `rotateX(${flipCount * 180}deg)` }}
          >
            {/* FRONT FACE */}
            <div className="absolute inset-0 backface-hidden bg-white dark:bg-[#202140] rounded-3xl border border-[#EDEFF4] dark:border-[#2E3856] shadow-[0_6px_28px_rgba(40,46,62,0.08)] flex flex-col justify-between overflow-hidden">
              <div className="px-6 pt-5 flex items-center justify-between gap-3">
                <button
                  type="button"
                  onClick={handleToggleHint}
                  className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-bold transition ${
                    showHint
                      ? 'bg-amber-100 text-amber-800 dark:bg-amber-900/50 dark:text-amber-200'
                      : 'text-[#586380] hover:bg-[#F6F7FB] dark:text-[#A8B1D1] dark:hover:bg-[#2E3856]'
                  }`}
                >
                  <Lightbulb className="w-4 h-4" />
                  <span>{showHint ? 'Ẩn gợi ý' : 'Hiển thị gợi ý'}</span>
                </button>

                <div className="flex items-center gap-1">
                  {isOwner && setId && (
                    <Link
                      to={`/sets/${setId}/edit`}
                      onClick={(e) => e.stopPropagation()}
                      title="Chỉnh sửa thẻ"
                      className="w-9 h-9 rounded-full flex items-center justify-center text-[#586380] hover:bg-[#F6F7FB] dark:text-[#A8B1D1] dark:hover:bg-[#2E3856] transition"
                    >
                      <Edit3 className="w-4 h-4" />
                    </Link>
                  )}

                  {tts?.isSupported && (
                    <button
                      type="button"
                      title="Phát âm (Phím A)"
                      onClick={(e) => {
                        e.stopPropagation();
                        tts.speak(getFrontText(activeCard), getFrontLang(), frontSpeakKey);
                      }}
                      className={`w-9 h-9 rounded-full flex items-center justify-center transition ${
                        tts.speakingKey === frontSpeakKey
                          ? 'bg-[#EDEFFF] text-[#4255FF] ring-2 ring-[#4255FF]/30 dark:bg-[#2E3856] dark:text-[#8E9BFF]'
                          : 'text-[#586380] hover:bg-[#EDEFFF] hover:text-[#4255FF] dark:text-[#A8B1D1] dark:hover:bg-[#2E3856]'
                      }`}
                    >
                      <Volume2 className="w-4.5 h-4.5" />
                    </button>
                  )}

                  <button
                    type="button"
                    disabled={togglingStarId === activeCard.id}
                    title={isStarred ? 'Bỏ gắn sao (Phím S)' : 'Gắn sao thẻ khó (Phím S)'}
                    onClick={(e) => {
                      e.stopPropagation();
                      onToggleStar?.(activeCard);
                    }}
                    className={`w-9 h-9 rounded-full flex items-center justify-center transition ${
                      isStarred
                        ? 'text-[#FFCD1F] bg-amber-50 dark:bg-amber-900/30'
                        : 'text-[#586380] hover:bg-[#F6F7FB] dark:text-[#A8B1D1] dark:hover:bg-[#2E3856]'
                    }`}
                  >
                    <Star className={`w-4.5 h-4.5 ${isStarred ? 'fill-[#FFCD1F] text-[#FFCD1F]' : ''}`} />
                  </button>
                </div>
              </div>

              <div className="flex-1 flex flex-col items-center justify-center px-6 text-center overflow-y-auto">
                <p className="text-2xl sm:text-[32px] font-medium text-[#282E3E] dark:text-white leading-snug break-words max-w-full">
                  {getFrontText(activeCard)}
                </p>
                {showHint && (
                  <p className="mt-4 px-4 py-2 rounded-xl bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800/60 text-xs sm:text-sm text-amber-800 dark:text-amber-200 font-semibold">
                    Gợi ý: {getHintText(activeCard)}
                  </p>
                )}
              </div>

              {/* Card Bottom Status Banner (Play / Shuffle) */}
              {cardStatusBanner ? (
                <div className="bg-[#DCE2FF] dark:bg-[#2E3856] py-2.5 px-4 text-center text-xs font-bold text-[#282E3E] dark:text-white">
                  {cardStatusBanner}
                </div>
              ) : (
                <div className="h-5" />
              )}
            </div>

            {/* BACK FACE */}
            <div className="absolute inset-0 backface-hidden rotate-x-180 bg-white dark:bg-[#202140] rounded-3xl border border-[#EDEFF4] dark:border-[#2E3856] shadow-[0_6px_28px_rgba(40,46,62,0.08)] flex flex-col justify-between overflow-hidden">
              <div className="px-6 pt-5 flex items-center justify-between gap-3">
                <span className="text-[11px] font-bold uppercase tracking-wider text-[#4255FF] dark:text-[#8E9BFF]">
                  {startWithDef ? 'Thuật ngữ' : 'Định nghĩa'}
                </span>

                <div className="flex items-center gap-1">
                  {isOwner && setId && (
                    <Link
                      to={`/sets/${setId}/edit`}
                      onClick={(e) => e.stopPropagation()}
                      title="Chỉnh sửa thẻ"
                      className="w-9 h-9 rounded-full flex items-center justify-center text-[#586380] hover:bg-[#F6F7FB] dark:text-[#A8B1D1] dark:hover:bg-[#2E3856] transition"
                    >
                      <Edit3 className="w-4 h-4" />
                    </Link>
                  )}

                  {tts?.isSupported && (
                    <button
                      type="button"
                      title="Phát âm mặt sau (Phím A)"
                      onClick={(e) => {
                        e.stopPropagation();
                        tts.speak(getBackText(activeCard), getBackLang(), backSpeakKey);
                      }}
                      className={`w-9 h-9 rounded-full flex items-center justify-center transition ${
                        tts.speakingKey === backSpeakKey
                          ? 'bg-[#EDEFFF] text-[#4255FF] ring-2 ring-[#4255FF]/30 dark:bg-[#2E3856] dark:text-[#8E9BFF]'
                          : 'text-[#586380] hover:bg-[#EDEFFF] hover:text-[#4255FF] dark:text-[#A8B1D1] dark:hover:bg-[#2E3856]'
                      }`}
                    >
                      <Volume2 className="w-4.5 h-4.5" />
                    </button>
                  )}

                  <button
                    type="button"
                    disabled={togglingStarId === activeCard.id}
                    title={isStarred ? 'Bỏ gắn sao (Phím S)' : 'Gắn sao thẻ khó (Phím S)'}
                    onClick={(e) => {
                      e.stopPropagation();
                      onToggleStar?.(activeCard);
                    }}
                    className={`w-9 h-9 rounded-full flex items-center justify-center transition ${
                      isStarred
                        ? 'text-[#FFCD1F] bg-amber-50 dark:bg-amber-900/30'
                        : 'text-[#586380] hover:bg-[#F6F7FB] dark:text-[#A8B1D1] dark:hover:bg-[#2E3856]'
                    }`}
                  >
                    <Star className={`w-4.5 h-4.5 ${isStarred ? 'fill-[#FFCD1F] text-[#FFCD1F]' : ''}`} />
                  </button>
                </div>
              </div>

              <div className="flex-1 flex flex-col items-center justify-center px-6 gap-4 overflow-y-auto">
                <div className="flex flex-col sm:flex-row items-center justify-center gap-5">
                  <p className="text-xl sm:text-[28px] font-normal text-[#282E3E] dark:text-white text-center leading-relaxed break-words max-w-xl">
                    {getBackText(activeCard)}
                  </p>
                  {activeCard.imageUrl && (
                    <img
                      src={activeCard.imageUrl}
                      alt={activeCard.term}
                      className="w-32 h-32 sm:w-40 sm:h-40 object-cover rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] shrink-0"
                    />
                  )}
                </div>

                {showHint && (
                  <p className="px-4 py-2 rounded-xl bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800/60 text-xs sm:text-sm text-amber-800 dark:text-amber-200 font-semibold">
                    Gợi ý: {getHintText(activeCard)}
                  </p>
                )}
              </div>

              {cardStatusBanner ? (
                <div className="bg-[#DCE2FF] dark:bg-[#2E3856] py-2.5 px-4 text-center text-xs font-bold text-[#282E3E] dark:text-white">
                  {cardStatusBanner}
                </div>
              ) : (
                <div className="h-5" />
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Bottom Controls Bar (Matches Images 1 & 3) */}
      <div className="flex flex-wrap items-center justify-between gap-3 pt-1">
        {/* Left: Sắp xếp thẻ ghi nhớ (same height & font size as Học với bạn bè) */}
        <button
          type="button"
          onClick={() => setSortModalOpen(true)}
          className="h-11 px-6 rounded-full bg-[#ECEFFF] hover:bg-[#E0E4FF] dark:bg-[#202140] dark:hover:bg-[#2E3856] text-[#4255FF] dark:text-[#8E9BFF] text-sm font-bold transition"
        >
          Sắp xếp thẻ ghi nhớ
        </button>

        {/* Center: <-  1 / 15  -> */}
        <div className="flex items-center gap-4">
          <button
            type="button"
            onClick={goPrev}
            disabled={displayCards.length <= 1}
            aria-label="Thẻ trước"
            className="w-11 h-11 rounded-full bg-[#F6F7FB] dark:bg-[#202140] hover:bg-[#EDEFFF] dark:hover:bg-[#2E3856] disabled:opacity-40 text-[#586380] dark:text-[#E2E8F0] flex items-center justify-center transition"
          >
            <ArrowLeft className="w-4.5 h-4.5" />
          </button>

          <span className="min-w-[54px] text-center text-sm font-bold text-[#282E3E] dark:text-white tabular-nums">
            {safeIndex + 1} / {displayCards.length}
          </span>

          <button
            type="button"
            onClick={goNext}
            disabled={displayCards.length <= 1}
            aria-label="Thẻ tiếp theo"
            className="w-11 h-11 rounded-full bg-[#F6F7FB] dark:bg-[#202140] hover:bg-[#EDEFFF] dark:hover:bg-[#2E3856] disabled:opacity-40 text-[#586380] dark:text-[#E2E8F0] flex items-center justify-center transition"
          >
            <ArrowRight className="w-4.5 h-4.5" />
          </button>
        </div>

        {/* Right 4 Buttons: Bắt đầu (Play/Pause), Trộn thẻ (Shuffle), Tùy chọn (Settings), Toàn màn hình */}
        <div className="flex items-center gap-1.5">
          <button
            type="button"
            disabled={displayCards.length <= 1}
            onClick={handleTogglePlay}
            title={isPlaying ? 'Tạm dừng tự động chạy' : 'Bắt đầu tự động chạy'}
            aria-label={isPlaying ? 'Tạm dừng' : 'Bắt đầu'}
            className={`w-9 h-9 rounded-full flex items-center justify-center transition ${
              isPlaying
                ? 'bg-[#EDEFFF] text-[#4255FF] ring-2 ring-[#4255FF] dark:bg-[#2E3856] dark:text-[#8E9BFF]'
                : 'bg-[#F6F7FB] text-[#586380] hover:bg-[#EDEFFF] hover:text-[#4255FF] dark:bg-[#202140] dark:text-[#A8B1D1]'
            }`}
          >
            {isPlaying ? <Pause className="w-4 h-4 fill-current" /> : <Play className="w-4 h-4 fill-current" />}
          </button>

          <button
            type="button"
            onClick={handleToggleShuffle}
            disabled={cards.length <= 1}
            title="Trộn thẻ"
            aria-label="Trộn thẻ"
            className={`w-9 h-9 rounded-full flex items-center justify-center transition ${
              isShuffled
                ? 'bg-[#EDEFFF] text-[#4255FF] ring-2 ring-[#4255FF] dark:bg-[#2E3856] dark:text-[#8E9BFF]'
                : 'bg-[#F6F7FB] text-[#586380] hover:bg-[#EDEFFF] hover:text-[#4255FF] dark:bg-[#202140] dark:text-[#A8B1D1]'
            }`}
          >
            <Shuffle className="w-4 h-4" />
          </button>

          <button
            type="button"
            onClick={() => setOptionsModalOpen(true)}
            title="Tùy chọn"
            aria-label="Tùy chọn"
            className="w-9 h-9 rounded-full text-[#586380] hover:bg-[#F6F7FB] dark:text-[#A8B1D1] dark:hover:bg-[#2E3856] flex items-center justify-center transition"
          >
            <Settings className="w-4.5 h-4.5" />
          </button>

          <button
            type="button"
            onClick={() => setFullscreenOpen(true)}
            title="Toàn màn hình"
            aria-label="Toàn màn hình"
            className="w-9 h-9 rounded-full text-[#586380] hover:bg-[#F6F7FB] dark:text-[#A8B1D1] dark:hover:bg-[#2E3856] flex items-center justify-center transition"
          >
            <Maximize2 className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Card Progress Bar */}
      <div className="h-[3px] w-full bg-[#EDEFF4] dark:bg-[#2E3856] rounded-full overflow-hidden">
        <div
          className="h-full bg-[#4255FF] dark:bg-[#8E9BFF] transition-all duration-300"
          style={{
            width: `${displayCards.length > 0 ? ((safeIndex + 1) / displayCards.length) * 100 : 0}%`,
          }}
        />
      </div>

      {/* Modals */}
      <SortFlashcardsModal
        open={sortModalOpen}
        currentMode={sortMode}
        onClose={() => setSortModalOpen(false)}
        onStart={(mode) => {
          setSortMode(mode);
          if (mode === 'basic' || mode === 'spaced') {
            setFullscreenOpen(true);
          }
        }}
      />

      <DeckOptionsModal
        open={optionsModalOpen}
        autoSpeak={autoSpeak}
        onToggleAutoSpeak={() => setAutoSpeak((p) => !p)}
        startWithDef={startWithDef}
        onToggleStartWithDef={() => {
          setStartWithDef((p) => !p);
          resetToFrontWithSlide('');
        }}
        onResetDeck={() => {
          setIsPlaying(false);
          setIsShuffled(false);
          setCurrentIndex(0);
          resetToFrontWithSlide('prev');
        }}
        onClose={() => setOptionsModalOpen(false)}
      />

      <FullscreenFlashcardsModal
        open={fullscreenOpen}
        title={setTitle}
        cards={displayCards}
        termLang={termLang}
        defLang={defLang}
        startWithDef={startWithDef}
        autoSpeak={autoSpeak}
        tts={tts}
        onOpenOptions={() => setOptionsModalOpen(true)}
        onSwitchToLearn={onSelectLearnMode}
        onClose={() => setFullscreenOpen(false)}
      />
    </div>
  );
}

import React, { useRef, useState } from 'react';
import {
  ArrowLeft,
  ArrowRight,
  BrainCircuit,
  FileCheck2,
  Layers,
  Radio,
  Users,
} from 'lucide-react';
import matchArt from '../../assets/game-match-art.png';
import blocksArt from '../../assets/game-blocks-art.png';
import gemsArt from '../../assets/game-gems-art.png';
import blastArt from '../../assets/game-blast-art.png';

export const PRIMARY_STUDY_MODES = [
  {
    key: 'flashcards',
    label: 'Thẻ ghi nhớ',
    isNew: true,
    icon: Layers,
    iconColor: 'text-[#4255FF] dark:text-[#8E9BFF]',
    phaseNote: 'Phase 5 (T5.4)',
  },
  {
    key: 'learn',
    label: 'Học',
    isNew: false,
    icon: BrainCircuit,
    iconColor: 'text-[#4255FF] dark:text-[#8E9BFF]',
    phaseNote: 'Phase 6 (T6.4)',
  },
  {
    key: 'test',
    label: 'Kiểm tra',
    isNew: false,
    icon: FileCheck2,
    iconColor: 'text-[#4255FF] dark:text-[#8E9BFF]',
    phaseNote: 'Phase 6 (T6.4)',
  },
];

export const FUN_GAMES = [
  {
    key: 'match',
    label: 'Ghép thẻ',
    isNew: false,
    artwork: matchArt,
    phaseNote: 'Phase 6 (T6.5)',
  },
  {
    key: 'blocks',
    label: 'Khối hộp',
    isNew: false,
    artwork: blocksArt,
    phaseNote: 'Phase 6 (T6.6)',
  },
  {
    key: 'gems',
    label: 'Đá quý',
    isNew: true,
    artwork: gemsArt,
    phaseNote: 'Phase 6 (T6.6)',
  },
  {
    key: 'blast',
    label: 'Bắn thiên thạch',
    isNew: false,
    artwork: blastArt,
    phaseNote: 'Phase 6 (T6.5)',
  },
];

export function SocialAndStudyModeBar({ cardCount = 0, onSelectMode }) {
  const disabled = cardCount === 0;

  return (
    <div className="space-y-5">
      {/* Row 1: 2 soft lavender pill buttons (Học với bạn bè / Chơi trực tiếp với bạn bè) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <button
          type="button"
          disabled={disabled}
          onClick={() =>
            onSelectMode?.({
              key: 'study-friends',
              label: 'Học với bạn bè',
              phaseNote: 'Phase 6 (T6.3 / T6.7)',
            })
          }
          className="h-11 px-5 rounded-full bg-[#ECEFFF] hover:bg-[#E0E4FF] dark:bg-[#202140] dark:hover:bg-[#2E3856] disabled:opacity-50 text-[#4255FF] dark:text-[#8E9BFF] font-bold text-sm inline-flex items-center justify-center gap-2 transition"
        >
          <Users className="w-4 h-4" />
          <span>Học với bạn bè</span>
          <span className="px-2 py-0.5 rounded-full text-[11px] font-extrabold bg-[#98F5FF] text-[#0D2B45]">
            Mới
          </span>
        </button>

        <button
          type="button"
          disabled={disabled}
          onClick={() =>
            onSelectMode?.({
              key: 'live',
              label: 'Chơi trực tiếp với bạn bè',
              phaseNote: 'Phase 6 (T6.7)',
            })
          }
          className="h-11 px-5 rounded-full bg-[#ECEFFF] hover:bg-[#E0E4FF] dark:bg-[#202140] dark:hover:bg-[#2E3856] disabled:opacity-50 text-[#4255FF] dark:text-[#8E9BFF] font-bold text-sm inline-flex items-center justify-center gap-2 transition"
        >
          <Radio className="w-4 h-4" />
          <span>Chơi trực tiếp với bạn bè</span>
        </button>
      </div>

      {/* Row 2: 3 Quizlet-style primary mode cards with reduced corner radius + bottom blue hover bar */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        {PRIMARY_STUDY_MODES.map((mode) => {
          const Icon = mode.icon;
          return (
            <button
              key={mode.key}
              type="button"
              disabled={disabled}
              onClick={() => onSelectMode?.(mode)}
              className="relative overflow-hidden h-12 px-5 rounded-lg bg-[#F6F7FB] hover:bg-[#F1F3FA] dark:bg-[#202140] dark:hover:bg-[#2E3856] border border-[#EDEFF4] dark:border-[#2E3856] disabled:opacity-50 flex items-center justify-center gap-2.5 transition shadow-[0_2px_6px_rgba(40,46,62,0.03)] after:content-[''] after:absolute after:inset-x-0 after:bottom-0 after:h-[4px] after:bg-[#A8B1FF] after:opacity-0 hover:after:opacity-100 after:transition-opacity"
            >
              <Icon className={`w-5 h-5 ${mode.iconColor}`} />
              <span className="text-[15px] font-extrabold text-[#282E3E] dark:text-white">
                {mode.label}
              </span>
              {mode.isNew && (
                <span className="px-2 py-0.5 rounded-full text-[11px] font-extrabold bg-[#98F5FF] text-[#0D2B45]">
                  Mới
                </span>
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
}

export function FunGamesSection({ cardCount = 0, onSelectMode }) {
  const disabled = cardCount === 0;
  const trackRef = useRef(null);
  const [canScrollLeft, setCanScrollLeft] = useState(false);
  const [canScrollRight, setCanScrollRight] = useState(true);

  const updateScrollButtons = () => {
    const el = trackRef.current;
    if (!el) return;
    setCanScrollLeft(el.scrollLeft > 8);
    setCanScrollRight(el.scrollLeft + el.clientWidth < el.scrollWidth - 8);
  };

  const scrollTrack = (direction) => {
    const el = trackRef.current;
    if (!el) return;
    const step = 260;
    el.scrollBy({ left: direction === 'right' ? step : -step, behavior: 'smooth' });
  };

  return (
    <section className="space-y-4">
      <h2 className="text-[20px] font-extrabold text-[#282E3E] dark:text-white tracking-tight">
        Nắm vững nội dung học qua những trò chơi vui nhộn.
      </h2>

      <div className="relative">
        {canScrollLeft && (
          <button
            type="button"
            onClick={() => scrollTrack('left')}
            aria-label="Trò chơi trước"
            className="hidden sm:flex absolute left-2 top-1/2 -translate-y-1/2 z-20 w-9 h-9 rounded-full bg-white dark:bg-[#2E3856] border border-[#EDEFF4] dark:border-[#3D4870] shadow-md items-center justify-center text-[#282E3E] dark:text-white hover:scale-105 transition"
          >
            <ArrowLeft className="w-4 h-4" />
          </button>
        )}

        {canScrollRight && (
          <button
            type="button"
            onClick={() => scrollTrack('right')}
            aria-label="Trò chơi tiếp theo"
            className="hidden sm:flex absolute right-2 top-1/2 -translate-y-1/2 z-20 w-9 h-9 rounded-full bg-white dark:bg-[#2E3856] border border-[#EDEFF4] dark:border-[#3D4870] shadow-md items-center justify-center text-[#282E3E] dark:text-white hover:scale-105 transition"
          >
            <ArrowRight className="w-4 h-4" />
          </button>
        )}

        {/* Right-edge soft fade like HomePage horizontal carousel */}
        {canScrollRight && (
          <div className="pointer-events-none hidden sm:block absolute top-0 right-0 bottom-1 w-14 z-10 bg-gradient-to-l from-white dark:from-[#0A082D] to-transparent" />
        )}

        <div
          ref={trackRef}
          onScroll={updateScrollButtons}
          className="flex gap-4 overflow-x-auto snap-x snap-mandatory pb-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
        >
          {FUN_GAMES.map((game) => (
            <button
              key={game.key}
              type="button"
              disabled={disabled}
              onClick={() => onSelectMode?.(game)}
              className="snap-start shrink-0 w-[235px] sm:w-[245px] group text-left bg-[#DCE2FF]/75 hover:bg-[#D0D8FF] dark:bg-[#202140] dark:hover:bg-[#2E3856] border border-transparent dark:border-[#2E3856] rounded-2xl p-3.5 flex flex-col gap-3 transition-all disabled:opacity-50"
            >
              <div className="flex items-center gap-2">
                <span className="text-[16px] font-extrabold text-[#282E3E] dark:text-white">
                  {game.label}
                </span>
                {game.isNew && (
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-extrabold bg-[#98F5FF] text-[#0D2B45] uppercase tracking-wide">
                    Mới
                  </span>
                )}
              </div>

              <div className="w-full aspect-[16/9] rounded-xl overflow-hidden bg-white/40 dark:bg-[#171936]">
                <img
                  src={game.artwork}
                  alt={game.label}
                  className="w-full h-full object-cover group-hover:scale-[1.03] transition-transform duration-300"
                />
              </div>
            </button>
          ))}
        </div>
      </div>
    </section>
  );
}

export default SocialAndStudyModeBar;

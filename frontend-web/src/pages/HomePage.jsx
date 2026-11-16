import React, { useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Sparkles,
  Brain,
  Trophy,
  ArrowRight,
  RotateCw,
  MoreVertical,
  ChevronLeft,
  ChevronRight,
  Layers,
  Folder,
  TrendingUp,
  Gamepad2,
  Rocket,
  Grid2X2,
  CheckCircle2,
  XCircle,
  X,
  FlaskConical,
  Snowflake,
  PieChart,
} from 'lucide-react';
import MainLayout from '../components/layout/MainLayout';
import { useAuthStore } from '../store/useAuthStore';
import {
  DKT_QUICK_QUESTIONS,
  FEATURED_SETS,
  GAME_MODES_PROMO,
  HOME_BANNERS,
  JUMP_BACK_IN_ITEMS,
  RECENT_SETS,
  RECOMMENDED_BY_STUDY,
} from '../data/homeMockData';

export default function HomePage() {
  const { isAuthenticated, user } = useAuthStore();

  // Guest flip card state
  const [previewFlipped, setPreviewFlipped] = useState(false);

  // Jump back in carousel state
  const [jumpIndex, setJumpIndex] = useState(0);
  const jumpTrackRef = useRef(null);

  // Interactive DKT quick quiz state
  const [questionIdx, setQuestionIdx] = useState(0);
  const [selectedOption, setSelectedOption] = useState(null);

  // Active game promo tab ('match' | 'blast' | 'blocks')
  const [activeGameId, setActiveGameId] = useState('match');

  // Floating AI helper modal state
  const [aiHelperOpen, setAiHelperOpen] = useState(false);

  const handleJumpScroll = (nextIdx) => {
    const clamped = Math.max(0, Math.min(nextIdx, JUMP_BACK_IN_ITEMS.length - 1));
    setJumpIndex(clamped);
    if (jumpTrackRef.current) {
      const cardWidth = jumpTrackRef.current.firstElementChild?.offsetWidth || 560;
      jumpTrackRef.current.scrollTo({
        left: clamped * (cardWidth + 16),
        behavior: 'smooth',
      });
    }
  };

  const handleJumpTrackScroll = () => {
    const el = jumpTrackRef.current;
    if (!el) return;
    const cardWidth = el.firstElementChild?.offsetWidth || 560;
    const approxIdx = Math.round(el.scrollLeft / (cardWidth + 16));
    const clamped = Math.max(0, Math.min(approxIdx, JUMP_BACK_IN_ITEMS.length - 1));
    if (clamped !== jumpIndex) setJumpIndex(clamped);
  };

  if (isAuthenticated && user) {
    const currentQuestion = DKT_QUICK_QUESTIONS[questionIdx];
    const activeGame =
      GAME_MODES_PROMO.find((g) => g.id === activeGameId) || GAME_MODES_PROMO[0];

    return (
      <MainLayout>
        <div className="max-w-[820px] mx-auto px-5 sm:px-8 pt-6 pb-24 space-y-10">
          {/* Block 1: Jump back in (1 full card + partial peek of next card) */}
          <section>
            <div className="flex items-center justify-between mb-3.5">
              <h2 className="text-[18px] font-bold tracking-tight text-[#282E3E] dark:text-white">
                Tiếp tục học
              </h2>
            </div>

            <div className="relative">
              {jumpIndex > 0 && (
                <button
                  type="button"
                  onClick={() => handleJumpScroll(jumpIndex - 1)}
                  aria-label="Thẻ trước"
                  className="hidden sm:flex absolute left-3 top-[43%] -translate-y-1/2 z-20 w-9 h-9 rounded-full bg-white dark:bg-[#2E3856] border border-[#EDEFF4] dark:border-[#3D4870] shadow-md items-center justify-center text-[#282E3E] dark:text-white hover:scale-105 transition"
                >
                  <ChevronLeft className="w-5 h-5" />
                </button>
              )}

              {jumpIndex < JUMP_BACK_IN_ITEMS.length - 1 && (
                <button
                  type="button"
                  onClick={() => handleJumpScroll(jumpIndex + 1)}
                  aria-label="Thẻ tiếp theo"
                  className="hidden sm:flex absolute right-4 top-[43%] -translate-y-1/2 z-20 w-9 h-9 rounded-full bg-white dark:bg-[#2E3856] border border-[#EDEFF4] dark:border-[#3D4870] shadow-md items-center justify-center text-[#282E3E] dark:text-white hover:scale-105 transition"
                >
                  <ChevronRight className="w-5 h-5" />
                </button>
              )}

              {/* Subtle right-edge fade over the peeking next card */}
              <div className="pointer-events-none hidden sm:block absolute top-0 right-0 bottom-6 w-16 z-10 bg-gradient-to-l from-white dark:from-[#0A082D] to-transparent" />

              <div
                ref={jumpTrackRef}
                onScroll={handleJumpTrackScroll}
                className="flex gap-4 overflow-x-auto snap-x snap-mandatory pb-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
              >
                {JUMP_BACK_IN_ITEMS.map((item) => (
                  <div
                    key={item.id}
                    className="snap-start shrink-0 w-[90%] sm:w-[79%] rounded-2xl border-2 border-[#D9DDE8] dark:border-[#2E3856] bg-white dark:bg-[#202140] p-6 relative overflow-hidden min-h-[212px] flex flex-col justify-between"
                  >
                    {/* Light Mode Illustration */}
                    <img
                      src={item.banner}
                      alt=""
                      aria-hidden="true"
                      className="block dark:hidden pointer-events-none select-none absolute right-0 bottom-0 w-44 sm:w-52 h-auto object-contain opacity-95"
                    />

                    {/* Dark Mode Layered Mini-Card Illustration */}
                    <div
                      aria-hidden="true"
                      className="hidden dark:block pointer-events-none select-none absolute right-0 bottom-0 w-56 h-36 overflow-hidden"
                    >
                      <div className="absolute -right-6 -bottom-8 w-52 h-40 rounded-tl-[48px] bg-gradient-to-br from-[#17467B]/70 to-[#112A56]/40" />
                      <div className="absolute right-24 bottom-9 w-24 h-14 rounded-lg bg-[#0A082D] border border-[#3E497A] p-2 shadow-md">
                        <div className="flex items-center justify-between">
                          <div className="space-y-1">
                            <div className="w-8 h-1.5 rounded bg-[#2E3856]" />
                            <div className="w-10 h-1.5 rounded bg-[#2E3856]" />
                          </div>
                          <PieChart className="w-4 h-4 text-[#7B8BFF]" />
                        </div>
                        <div className="mt-2 w-full h-1.5 rounded bg-[#202140]" />
                      </div>
                      <div className="absolute right-2 bottom-12 w-24 h-14 rounded-lg bg-[#0A082D] border border-[#4255FF]/70 p-2 shadow-md">
                        <div className="flex items-center justify-between h-full">
                          <div className="space-y-1.5">
                            <div className="w-8 h-1.5 rounded bg-[#2E3856]" />
                            <div className="w-10 h-1.5 rounded bg-[#2E3856]" />
                          </div>
                          <FlaskConical className="w-5 h-5 text-[#6675FF]" />
                        </div>
                      </div>
                      <div className="absolute right-6 -bottom-4 w-24 h-12 rounded-lg bg-[#0A082D] border border-[#3E497A] p-2">
                        <div className="flex items-center justify-between">
                          <div className="w-8 h-1.5 rounded bg-[#2E3856]" />
                          <Snowflake className="w-4 h-4 text-[#6675FF]" />
                        </div>
                      </div>
                    </div>

                    <div className="relative z-10 max-w-[66%]">
                      <h3 className="text-[20px] font-bold tracking-tight text-[#282E3E] dark:text-white">
                        {item.title}
                      </h3>

                      {item.showProgress ? (
                        <div className="mt-3 space-y-1.5">
                          <div className="w-full max-w-[260px] h-2 rounded-full bg-[#EDEFF4] dark:bg-[#2E3856] overflow-hidden">
                            <div
                              className="h-full rounded-full bg-[#23B26D]"
                              style={{ width: `${item.progressPercent}%` }}
                            />
                          </div>
                          <p className="text-[13px] font-medium text-[#586380] dark:text-[#A8B1D1]">
                            {item.progressLabel}
                          </p>
                        </div>
                      ) : (
                        <p className="mt-2 text-[14px] font-medium text-[#586380] dark:text-[#A8B1D1]">
                          {item.subtitle}
                        </p>
                      )}
                    </div>

                    <button
                      type="button"
                      aria-label="Tùy chọn"
                      className="absolute top-5 right-4 z-10 w-8 h-8 rounded-full flex items-center justify-center text-[#586380] dark:text-[#A8B1D1] hover:bg-[#F6F7FB] dark:hover:bg-[#2E3856] transition-colors"
                    >
                      <MoreVertical className="w-4 h-4" />
                    </button>

                    <div className="relative z-10 pt-5">
                      <Link
                        to={item.to}
                        className="inline-flex items-center justify-center h-10 px-5 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white text-[14px] font-bold transition-colors"
                      >
                        {item.ctaLabel}
                      </Link>
                    </div>
                  </div>
                ))}
              </div>

              {/* Pagination Dots */}
              <div className="mt-3 flex items-center justify-center gap-1.5">
                {JUMP_BACK_IN_ITEMS.map((item, idx) => (
                  <button
                    key={item.id}
                    type="button"
                    aria-label={`Chuyển đến thẻ ${idx + 1}`}
                    onClick={() => handleJumpScroll(idx)}
                    className={`h-1.5 rounded-full transition-all ${
                      jumpIndex === idx
                        ? 'w-3 bg-[#4255FF] dark:bg-white'
                        : 'w-1.5 bg-[#D9DDE8] dark:bg-[#3D4870]'
                    }`}
                  />
                ))}
              </div>
            </div>
          </section>

          {/* Block 2: Recents (2x2 Grid) */}
          <section>
            <h2 className="text-[18px] font-bold tracking-tight text-[#282E3E] dark:text-white mb-3.5">
              Gần đây
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {RECENT_SETS.map((item) => (
                <Link
                  key={item.id}
                  to={item.to}
                  className="flex items-center gap-3.5 p-3 rounded-xl hover:bg-[#EAECEF] dark:hover:bg-[#202140] transition-colors"
                >
                  <div
                    className={`w-11 h-11 rounded-xl flex items-center justify-center shrink-0 ${
                      item.type === 'folder'
                        ? 'bg-[#E6F8EF] text-[#23B26D] dark:bg-[#193A37] dark:text-[#5EEAD4]'
                        : 'bg-[#EBF4FF] text-[#3B82F6] dark:bg-[#202140] dark:text-[#7B8BFF]'
                    }`}
                  >
                    {item.type === 'folder' ? (
                      <Folder className="w-5 h-5" />
                    ) : (
                      <Layers className="w-5 h-5" />
                    )}
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-[15px] font-bold text-[#282E3E] dark:text-white truncate">
                      {item.title}
                    </p>
                    <p className="text-[13px] font-medium text-[#586380] dark:text-[#A8B1D1] truncate">
                      {item.meta}
                    </p>
                  </div>
                </Link>
              ))}
            </div>
          </section>

          {/* Block 3: Interactive Weak-Card Quick Question (1 Block - DKT powered) */}
          <section>
            <div className="flex items-start justify-between mb-3">
              <div className="space-y-0.5">
                <p className="text-[13px] leading-tight font-semibold text-[#586380] dark:text-[#A8B1D1]">
                  Luyện nhanh thẻ hay quên (DKT)
                </p>
                <h2 className="text-[20px] leading-snug font-extrabold tracking-tight text-[#282E3E] dark:text-white">
                  {currentQuestion.setTitle}
                </h2>
              </div>
              <button
                type="button"
                aria-label="Tùy chọn câu hỏi"
                className="w-8 h-8 rounded-full flex items-center justify-center text-[#586380] dark:text-[#A8B1D1] hover:bg-[#F6F7FB] dark:hover:bg-[#202140]"
              >
                <MoreVertical className="w-4 h-4" />
              </button>
            </div>

            <div className="rounded-2xl border-2 border-[#D9DDE8] dark:border-[#2E3856] bg-white dark:bg-[#202140] p-6">
              <div className="flex items-center justify-between text-[13px] font-bold text-[#586380] dark:text-[#A8B1D1] mb-3">
                <span>
                  {questionIdx + 1} / {DKT_QUICK_QUESTIONS.length}
                </span>
                {selectedOption !== null && (
                  <button
                    type="button"
                    onClick={() => {
                      setSelectedOption(null);
                      setQuestionIdx((prev) => (prev + 1) % DKT_QUICK_QUESTIONS.length);
                    }}
                    className="text-[#4255FF] dark:text-[#93A3FF] hover:underline font-bold"
                  >
                    Câu tiếp theo →
                  </button>
                )}
              </div>

              <p className="text-[18px] font-semibold text-[#282E3E] dark:text-white mb-5">
                {currentQuestion.prompt}
              </p>

              <div className="space-y-2.5">
                {currentQuestion.options.map((opt, idx) => {
                  const isSelected = selectedOption === idx;
                  const isCorrect = idx === currentQuestion.correctIndex;
                  const answered = selectedOption !== null;

                  let optionStyle =
                    'border-[#D9DDE8] dark:border-[#353D68] bg-white dark:bg-[#202140] hover:bg-[#F6F7FB] dark:hover:bg-[#2E3856] hover:border-[#3B82F6] dark:hover:border-[#60A5FA] text-[#282E3E] dark:text-[#F6F7FB]';

                  if (answered) {
                    if (isCorrect) {
                      optionStyle =
                        'border-[#23B26D] bg-[#E6F8EF] dark:bg-[#143D2D] text-[#116941] dark:text-[#6EE7B7] font-semibold';
                    } else if (isSelected) {
                      optionStyle =
                        'border-[#FF725B] bg-[#FFF0ED] dark:bg-[#431F25] text-[#B42318] dark:text-[#FDA29B] font-semibold';
                    } else {
                      optionStyle =
                        'border-[#EDEFF4] dark:border-[#2E3856] bg-white dark:bg-[#202140] text-[#939BB4] dark:text-[#697499]';
                    }
                  }

                  return (
                    <button
                      key={opt}
                      type="button"
                      onClick={() => setSelectedOption(idx)}
                      className={`w-full min-h-[48px] px-4 py-3 rounded-xl border-2 text-left text-[15px] font-medium flex items-center justify-between gap-3 transition-colors ${optionStyle}`}
                    >
                      <span>{opt}</span>
                      {answered && isCorrect && (
                        <CheckCircle2 className="w-5 h-5 text-[#23B26D] shrink-0" />
                      )}
                      {answered && isSelected && !isCorrect && (
                        <XCircle className="w-5 h-5 text-[#FF725B] shrink-0" />
                      )}
                    </button>
                  );
                })}
              </div>
            </div>
          </section>

          {/* Block 4: Game Promo + Study/Create Cards (with uploaded banners) */}
          <section className="space-y-6">
            <div>
              <div className="flex flex-wrap items-center justify-between gap-3 mb-3">
                <p className="text-[14px] font-bold text-[#586380] dark:text-[#A8B1D1]">
                  Đổi gió với trò chơi
                </p>

                <div className="inline-flex p-1 rounded-full bg-[#F6F7FB] dark:bg-[#202140] border border-[#D9DDE8] dark:border-[#2E3856]">
                  {GAME_MODES_PROMO.map((mode) => (
                    <button
                      key={mode.id}
                      type="button"
                      onClick={() => setActiveGameId(mode.id)}
                      className={`px-3.5 py-1 rounded-full text-[12px] font-bold transition-colors ${
                        activeGameId === mode.id
                          ? 'bg-white dark:bg-[#2E3856] text-[#282E3E] dark:text-white shadow-sm'
                          : 'text-[#586380] dark:text-[#A8B1D1] hover:text-[#282E3E] dark:hover:text-white'
                      }`}
                    >
                      {mode.tabLabel}
                    </button>
                  ))}
                </div>
              </div>

              <div className="rounded-2xl border-2 border-[#D9DDE8] dark:border-[#2E3856] bg-white dark:bg-[#202140] p-5 sm:p-6 grid grid-cols-1 md:grid-cols-12 gap-5 items-center">
                <div className="md:col-span-6 flex flex-col justify-between h-full space-y-4">
                  <div className="flex items-start justify-between">
                    <div
                      className={`w-10 h-10 rounded-xl ${activeGame.badgeBg} ${activeGame.accentColor} flex items-center justify-center`}
                    >
                      {activeGame.id === 'blast' ? (
                        <Rocket className="w-5 h-5" />
                      ) : activeGame.id === 'blocks' ? (
                        <Grid2X2 className="w-5 h-5" />
                      ) : (
                        <Gamepad2 className="w-5 h-5" />
                      )}
                    </div>
                    <button
                      type="button"
                      aria-label="Tùy chọn"
                      className="w-8 h-8 rounded-full flex items-center justify-center text-[#586380] dark:text-[#A8B1D1] hover:bg-[#F6F7FB] dark:hover:bg-[#2E3856]"
                    >
                      <MoreVertical className="w-4 h-4" />
                    </button>
                  </div>

                  <div>
                    <h3 className="text-[20px] font-bold tracking-tight text-[#282E3E] dark:text-white">
                      {activeGame.setTitle}
                    </h3>
                    <p className="mt-1.5 text-[14px] text-[#586380] dark:text-[#A8B1D1] leading-relaxed">
                      {activeGame.description}
                    </p>
                  </div>

                  <div>
                    <Link
                      to={activeGame.to}
                      className="inline-flex items-center justify-center w-full sm:w-auto min-w-[200px] h-11 px-6 rounded-full bg-[#EDEFFF] hover:bg-[#DFE3FF] dark:bg-[#2E3856] dark:hover:bg-[#3A476B] text-[#4255FF] dark:text-white text-[14px] font-bold transition-colors"
                    >
                      {activeGame.ctaLabel}
                    </Link>
                  </div>
                </div>

                <div className="md:col-span-6">
                  <div className="rounded-xl overflow-hidden aspect-[16/9] bg-[#F6F7FB] dark:bg-[#0A082D]">
                    <img
                      src={activeGame.banner}
                      alt={activeGame.tabLabel}
                      className="w-full h-full object-cover"
                    />
                  </div>
                </div>
              </div>
            </div>

            {/* Up next: Flashcards + Create your own flashcards */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
              <div className="rounded-2xl border-2 border-[#D9DDE8] dark:border-[#2E3856] bg-white dark:bg-[#202140] p-5 flex flex-col justify-between gap-4">
                <div className="rounded-xl overflow-hidden aspect-[16/9] bg-[#F6F7FB] dark:bg-[#0A082D]">
                  <img
                    src={HOME_BANNERS.upNextFlashcards}
                    alt="Ôn tập thẻ ghi nhớ"
                    className="w-full h-full object-cover"
                  />
                </div>
                <div>
                  <p className="text-[12px] font-bold text-[#586380] dark:text-[#A8B1D1]">
                    Tiếp theo: Thẻ ghi nhớ
                  </p>
                  <h3 className="text-[18px] font-bold text-[#282E3E] dark:text-white mt-0.5">
                    CHAPTER 1 - IT
                  </h3>
                  <p className="text-[13px] text-[#586380] dark:text-[#A8B1D1] mt-1">
                    Lật thẻ ôn lại thuật ngữ và định nghĩa
                  </p>
                </div>
                <Link
                  to="/explore?mode=flashcards"
                  className="w-full h-11 rounded-full bg-[#EDEFFF] hover:bg-[#DFE3FF] dark:bg-[#2E3856] dark:hover:bg-[#3A476B] text-[#4255FF] dark:text-white text-[14px] font-bold flex items-center justify-center transition-colors"
                >
                  Ôn với Thẻ ghi nhớ
                </Link>
              </div>

              <div className="rounded-2xl border-2 border-[#D9DDE8] dark:border-[#2E3856] bg-white dark:bg-[#202140] p-5 flex flex-col justify-between gap-4">
                <div className="rounded-xl overflow-hidden aspect-[16/9] bg-[#F6F7FB] dark:bg-[#0A082D]">
                  <img
                    src={HOME_BANNERS.createFlashcards}
                    alt="Tạo bộ thẻ ghi nhớ"
                    className="w-full h-full object-cover"
                  />
                </div>
                <div>
                  <p className="text-[12px] font-bold text-[#586380] dark:text-[#A8B1D1]">
                    Học đúng nội dung bạn cần
                  </p>
                  <h3 className="text-[18px] font-bold text-[#282E3E] dark:text-white mt-0.5">
                    Tạo bộ thẻ của riêng bạn
                  </h3>
                  <p className="text-[13px] text-[#586380] dark:text-[#A8B1D1] mt-1">
                    Nhập thủ công, dán danh sách hoặc quét ảnh OCR tự động
                  </p>
                </div>
                <Link
                  to="/library"
                  className="w-full h-11 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white text-[14px] font-bold flex items-center justify-center transition-colors"
                >
                  Tạo bộ thẻ
                </Link>
              </div>
            </div>
          </section>

          {/* Block 5A: Recommended based on recent studying */}
          <SetCarouselSection
            eyebrow="Dựa trên nội dung bạn vừa học"
            title="CHAPTER 1 - IT & Tiếng Anh"
            items={RECOMMENDED_BY_STUDY}
          />

          {/* Block 5B: Featured Sets (Staff Picks) */}
          <SetCarouselSection
            eyebrow="Đề xuất nổi bật"
            title="Bộ thẻ nên thử trên SmartFlash"
            items={FEATURED_SETS}
          />
        </div>

        {/* Floating Bottom-Center AI Button ("Hỏi SmartFlash ✨") */}
        <div className="fixed bottom-5 left-1/2 -translate-x-1/2 z-30 flex flex-col items-center">
          {aiHelperOpen && (
            <div className="mb-3 w-80 sm:w-96 rounded-2xl bg-white dark:bg-[#202140] border border-[#EDEFF4] dark:border-[#2E3856] shadow-2xl p-4">
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2">
                  <Sparkles className="w-4 h-4 text-[#4255FF] dark:text-[#93A3FF]" />
                  <span className="text-sm font-bold text-[#282E3E] dark:text-white">
                    Trợ lý AI SmartFlash
                  </span>
                </div>
                <button
                  type="button"
                  onClick={() => setAiHelperOpen(false)}
                  className="p-1 rounded-full text-[#586380] dark:text-[#A8B1D1] hover:bg-[#F6F7FB] dark:hover:bg-[#2E3856]"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
              <p className="text-xs text-[#586380] dark:text-[#A8B1D1] leading-relaxed">
                Khung trò chuyện AI tùy biến theo bộ thẻ đang học & tự động sinh bộ thẻ forward sang trang tạo (Phase 4).
              </p>
            </div>
          )}

          <button
            type="button"
            onClick={() => setAiHelperOpen((prev) => !prev)}
            className="h-11 px-5 rounded-full bg-[#1A1D28] hover:bg-[#282E3E] text-white dark:bg-white dark:hover:bg-[#EDEFF4] dark:text-[#1A1D28] shadow-lg text-[14px] font-bold flex items-center gap-2 transition-colors"
          >
            <span>Hỏi SmartFlash</span>
            <Sparkles className="w-4 h-4" />
          </button>
        </div>
      </MainLayout>
    );
  }

  // Guest Home View (Light + Dark mode compatible)
  return (
    <MainLayout>
      <div className="max-w-[820px] mx-auto px-5 sm:px-8 py-12 sm:py-16 space-y-14">
        <section className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
          <div className="lg:col-span-7 space-y-5">
            <h1 className="text-3xl sm:text-5xl font-extrabold text-[#282E3E] dark:text-white tracking-tight leading-[1.15]">
              Học nhanh hơn với thẻ ghi nhớ thông minh.
            </h1>
            <p className="text-base sm:text-lg text-[#586380] dark:text-[#A8B1D1] leading-relaxed max-w-xl">
              Kết hợp lặp lại ngắt quãng FSRS-4.5, quét ảnh OCR, trợ lý AI và bộ trò chơi ôn tập trực tiếp trên Web và Android.
            </p>
            <div className="flex flex-wrap items-center gap-3 pt-1">
              <Link
                to="/register"
                className="inline-flex items-center gap-2 bg-[#4255FF] hover:bg-[#3545E0] text-white font-bold px-6 py-3 rounded-full transition-colors"
              >
                <span>Đăng ký miễn phí</span>
                <ArrowRight className="w-4 h-4" />
              </Link>
              <Link
                to="/explore"
                className="inline-flex items-center gap-2 bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#202140] dark:hover:bg-[#2E3856] text-[#282E3E] dark:text-white font-bold px-6 py-3 rounded-full transition-colors"
              >
                <span>Khám phá bộ thẻ</span>
              </Link>
            </div>
          </div>

          <div className="lg:col-span-5">
            <button
              type="button"
              onClick={() => setPreviewFlipped((prev) => !prev)}
              className="w-full text-left bg-white dark:bg-[#202140] rounded-3xl border border-[#EDEFF4] dark:border-[#2E3856] shadow-md hover:shadow-lg p-7 min-h-[220px] flex flex-col justify-between transition group"
            >
              <div className="flex items-center justify-between text-xs font-bold text-[#586380] dark:text-[#A8B1D1]">
                <span>{previewFlipped ? 'Định nghĩa' : 'Thuật ngữ'}</span>
                <span className="inline-flex items-center gap-1 text-[#4255FF] dark:text-[#93A3FF]">
                  <RotateCw className="w-3.5 h-3.5 group-hover:rotate-90 transition-transform" />
                  Nhấn để lật
                </span>
              </div>

              <div className="py-6 text-center">
                <p className="text-xl sm:text-2xl font-extrabold text-[#282E3E] dark:text-white">
                  {previewFlipped
                    ? 'Ôn tập đúng thời điểm sắp quên để ghi nhớ dài hạn'
                    : 'Spaced Repetition (FSRS-4.5)'}
                </p>
              </div>

              <div className="flex items-center justify-between text-xs text-[#586380] dark:text-[#A8B1D1]">
                <span>Thẻ mẫu 1 / 1</span>
                <span className="font-semibold text-[#23B26D]">SmartFlash AI</span>
              </div>
            </button>
          </div>
        </section>

        <section className="grid grid-cols-1 md:grid-cols-3 gap-5">
          <div className="bg-white dark:bg-[#202140] p-6 rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856]">
            <div className="w-10 h-10 rounded-xl bg-[#EDEFFF] dark:bg-[#2E3856] text-[#4255FF] dark:text-[#93A3FF] flex items-center justify-center mb-4">
              <Brain className="w-5 h-5" />
            </div>
            <h3 className="text-base font-bold text-[#282E3E] dark:text-white">FSRS-4.5 & DKT</h3>
            <p className="mt-1.5 text-sm text-[#586380] dark:text-[#A8B1D1] leading-relaxed">
              Dự đoán xác suất nhớ và tự động sắp xếp lịch ôn tập tối ưu cho từng thẻ.
            </p>
          </div>

          <div className="bg-white dark:bg-[#202140] p-6 rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856]">
            <div className="w-10 h-10 rounded-xl bg-amber-50 dark:bg-[#2E3856] text-amber-600 dark:text-[#FFCD1F] flex items-center justify-center mb-4">
              <Sparkles className="w-5 h-5" />
            </div>
            <h3 className="text-base font-bold text-[#282E3E] dark:text-white">Quét OCR & AI Tutor</h3>
            <p className="mt-1.5 text-sm text-[#586380] dark:text-[#A8B1D1] leading-relaxed">
              Chụp ảnh tài liệu tạo thẻ tự động và chat cùng AI sinh bộ thẻ theo ngữ cảnh.
            </p>
          </div>

          <div className="bg-white dark:bg-[#202140] p-6 rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856]">
            <div className="w-10 h-10 rounded-xl bg-purple-50 dark:bg-[#2E3856] text-purple-600 dark:text-[#C084FC] flex items-center justify-center mb-4">
              <Trophy className="w-5 h-5" />
            </div>
            <h3 className="text-base font-bold text-[#282E3E] dark:text-white">Minigames & Live Arena</h3>
            <p className="mt-1.5 text-sm text-[#586380] dark:text-[#A8B1D1] leading-relaxed">
              Ghép thẻ, Blast, Đá quý, Khối hộp và phòng thi đấu trực tiếp qua mã PIN.
            </p>
          </div>
        </section>
      </div>
    </MainLayout>
  );
}

function SetCarouselSection({ eyebrow, title, items }) {
  const trackRef = useRef(null);
  const [activeIndex, setActiveIndex] = useState(0);
  const [canScrollLeft, setCanScrollLeft] = useState(false);
  const [canScrollRight, setCanScrollRight] = useState(items.length > 2);

  const syncScrollState = () => {
    const el = trackRef.current;
    if (!el) return;
    const maxScroll = el.scrollWidth - el.clientWidth;
    setCanScrollLeft(el.scrollLeft > 4);
    setCanScrollRight(el.scrollLeft < maxScroll - 4);

    const cardWidth = el.firstElementChild?.offsetWidth || 278;
    const approxIdx = Math.round(el.scrollLeft / (cardWidth + 16));
    const clamped = Math.max(0, Math.min(approxIdx, items.length - 1));
    setActiveIndex(clamped);
  };

  const scrollToCard = (nextIdx) => {
    const el = trackRef.current;
    if (!el) return;
    const clamped = Math.max(0, Math.min(nextIdx, items.length - 1));
    setActiveIndex(clamped);
    const cardWidth = el.firstElementChild?.offsetWidth || 278;
    el.scrollTo({
      left: clamped * (cardWidth + 16),
      behavior: 'smooth',
    });
  };

  return (
    <section>
      <div className="flex items-start justify-between mb-3.5">
        <div className="space-y-0.5">
          <p className="text-[13px] leading-tight font-semibold text-[#586380] dark:text-[#A8B1D1]">
            {eyebrow}
          </p>
          <h2 className="text-[20px] leading-snug font-extrabold tracking-tight text-[#282E3E] dark:text-white">
            {title}
          </h2>
        </div>
        <button
          type="button"
          aria-label="Tùy chọn mục đề xuất"
          className="w-8 h-8 rounded-full flex items-center justify-center text-[#586380] dark:text-[#A8B1D1] hover:bg-[#F6F7FB] dark:hover:bg-[#202140]"
        >
          <MoreVertical className="w-4 h-4" />
        </button>
      </div>

      <div className="relative">
        {canScrollLeft && (
          <>
            <div className="pointer-events-none hidden sm:block absolute top-0 left-0 bottom-6 w-12 z-10 bg-gradient-to-r from-white dark:from-[#0A082D] to-transparent" />
            <button
              type="button"
              onClick={() => scrollToCard(activeIndex - 1)}
              aria-label="Thẻ trước"
              className="hidden sm:flex absolute left-3 top-[43%] -translate-y-1/2 z-20 w-9 h-9 rounded-full bg-white dark:bg-[#2E3856] border border-[#EDEFF4] dark:border-[#3D4870] shadow-md items-center justify-center text-[#282E3E] dark:text-white hover:scale-105 transition"
            >
              <ChevronLeft className="w-5 h-5" />
            </button>
          </>
        )}

        {canScrollRight && (
          <>
            <div className="pointer-events-none hidden sm:block absolute top-0 right-0 bottom-6 w-16 z-10 bg-gradient-to-l from-white dark:from-[#0A082D] to-transparent" />
            <button
              type="button"
              onClick={() => scrollToCard(activeIndex + 1)}
              aria-label="Thẻ tiếp theo"
              className="hidden sm:flex absolute right-4 top-[43%] -translate-y-1/2 z-20 w-9 h-9 rounded-full bg-white dark:bg-[#2E3856] border border-[#EDEFF4] dark:border-[#3D4870] shadow-md items-center justify-center text-[#282E3E] dark:text-white hover:scale-105 transition"
            >
              <ChevronRight className="w-5 h-5" />
            </button>
          </>
        )}

        <div
          ref={trackRef}
          onScroll={syncScrollState}
          className="flex gap-4 overflow-x-auto snap-x snap-mandatory pb-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
        >
          {items.map((setItem) => (
            <Link
              key={setItem.id}
              to={setItem.to}
              className="snap-start shrink-0 w-[80%] sm:w-[278px] rounded-2xl border-2 border-[#D9DDE8] dark:border-[#2E3856] bg-white dark:bg-[#202140] p-5 hover:border-[#3B82F6] dark:hover:border-[#60A5FA] transition-colors flex flex-col justify-between min-h-[182px]"
            >
              <div className="flex items-start justify-between gap-2">
                <div className="w-11 h-11 rounded-xl bg-[#F6F7FB] dark:bg-[#2E3856] text-[#586380] dark:text-[#A8B1D1] flex items-center justify-center shrink-0">
                  <Layers className="w-5 h-5" />
                </div>

                {setItem.badgeText && (
                  <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-[#F6F7FB] dark:bg-[#2E3856] text-[11px] font-bold text-[#586380] dark:text-[#C4CCF0]">
                    <TrendingUp className="w-3 h-3" />
                    {setItem.badgeText}
                  </span>
                )}
              </div>

              <div className="mt-4">
                <h3 className="text-[16px] leading-snug font-bold text-[#282E3E] dark:text-white line-clamp-2">
                  {setItem.title}
                </h3>

                <div className="mt-1.5 flex items-center justify-between gap-2 text-[13.5px] font-semibold text-[#586380] dark:text-[#A8B1D1]">
                  <span className="truncate">
                    {setItem.cardsCount} thẻ • bởi {setItem.author}
                  </span>
                  <MoreVertical className="w-4 h-4 shrink-0 opacity-60" />
                </div>
              </div>
            </Link>
          ))}
        </div>

        {/* Pagination Dots */}
        <div className="mt-3 flex items-center justify-center gap-1.5">
          {items.map((item, idx) => (
            <button
              key={item.id}
              type="button"
              aria-label={`Chuyển đến thẻ ${idx + 1}`}
              onClick={() => scrollToCard(idx)}
              className={`h-1.5 rounded-full transition-all ${
                activeIndex === idx
                  ? 'w-3 bg-[#4255FF] dark:bg-white'
                  : 'w-1.5 bg-[#D9DDE8] dark:bg-[#3D4870]'
              }`}
            />
          ))}
        </div>
      </div>
    </section>
  );
}

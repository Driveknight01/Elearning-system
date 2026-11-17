import React, { useEffect, useMemo, useState } from 'react';
import { BrainCircuit, Check, X } from 'lucide-react';

/**
 * Interactive Quizlet-style "Câu hỏi mẫu cho học phần này" widget (matches reference Image 1).
 */
export default function SampleQuestionWidget({ cards = [], onOpenLearnMode }) {
  const [questionIdx, setQuestionIdx] = useState(0);
  const [selectedOption, setSelectedOption] = useState(null);
  const [revealed, setRevealed] = useState(false);

  const sampleQuestions = useMemo(() => {
    if (!Array.isArray(cards) || cards.length < 2) return [];
    const limit = Math.min(cards.length, 7);
    return cards.slice(0, limit).map((targetCard, idx) => {
      const distractors = cards
        .filter((c) => c.id !== targetCard.id && c.term)
        .slice(0, 3)
        .map((c) => c.term);

      const rawOptions = [targetCard.term, ...distractors];
      const shift = idx % rawOptions.length;
      const options = [...rawOptions.slice(shift), ...rawOptions.slice(0, shift)];

      return {
        id: targetCard.id,
        prompt: targetCard.definition,
        correctTerm: targetCard.term,
        options,
      };
    });
  }, [cards]);

  const safeIdx = sampleQuestions.length > 0 ? Math.min(questionIdx, sampleQuestions.length - 1) : 0;
  const activeQuestion = sampleQuestions[safeIdx] || null;
  const showResult = selectedOption !== null || revealed;
  const isUserCorrect = selectedOption === activeQuestion?.correctTerm;

  const handleNextQuestion = () => {
    if (sampleQuestions.length === 0) return;
    setQuestionIdx((prev) => (prev + 1 < sampleQuestions.length ? prev + 1 : 0));
    setSelectedOption(null);
    setRevealed(false);
  };

  // Press any key to continue when answered
  useEffect(() => {
    if (!showResult) return undefined;
    const onKeyDown = (e) => {
      const tag = e.target?.tagName?.toLowerCase();
      if (tag === 'input' || tag === 'textarea' || tag === 'select' || e.target?.isContentEditable) {
        return;
      }
      if (e.metaKey || e.ctrlKey || e.altKey || e.key === 'Escape') return;
      handleNextQuestion();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [showResult, sampleQuestions.length]);

  if (!activeQuestion) return null;

  const handleSelectOption = (opt) => {
    if (showResult) return;
    setSelectedOption(opt);
  };

  return (
    <section className="space-y-4">
      <h2 className="text-[20px] font-extrabold text-[#282E3E] dark:text-white tracking-tight">
        Câu hỏi mẫu cho học phần này
      </h2>

      <div className="bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4]/80 dark:border-[#2E3856] shadow-[0_4px_20px_rgba(40,46,62,0.06)] overflow-hidden">
        {/* Soft Top Bar */}
        <div className="px-6 py-4 bg-[#F6F7FB] dark:bg-[#1A1C3A] grid grid-cols-3 items-center">
          <div className="flex items-center gap-2 text-[15px] font-bold text-[#282E3E] dark:text-white">
            <BrainCircuit className="w-5 h-5 text-[#4255FF] dark:text-[#8E9BFF]" />
            <span>Học</span>
          </div>

          <span className="text-center text-[15px] font-bold text-[#282E3E] dark:text-white tabular-nums">
            {safeIdx + 1} / {sampleQuestions.length}
          </span>

          <div className="text-right">
            <button
              type="button"
              onClick={onOpenLearnMode}
              className="text-sm font-bold text-[#586380] dark:text-[#A8B1D1] hover:text-[#4255FF] dark:hover:text-[#8E9BFF] transition"
            >
              Dùng chế độ Học
            </button>
          </div>
        </div>

        {/* Question Body (taller vertical spacing + +1 text size step) */}
        <div className="px-6 sm:px-8 py-8 sm:py-9 space-y-11">
          <p className="text-[19px] font-normal text-[#282E3E] dark:text-white leading-relaxed">
            {activeQuestion.prompt}
          </p>

          <div className="space-y-4">
            {!showResult ? (
              <p className="text-sm font-bold text-[#586380] dark:text-[#A8B1D1]">
                Chọn đáp án đúng
              </p>
            ) : isUserCorrect ? (
              <p className="text-base font-extrabold text-[#10B981]">
                Tuyệt vời! Chính xác!
              </p>
            ) : (
              <p className="text-base font-extrabold text-[#D97706]">
                Đừng lo, bạn vẫn đang học mà!
              </p>
            )}

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
              {activeQuestion.options.map((opt, idx) => {
                const isSelected = selectedOption === opt;
                const isCorrectOpt = opt === activeQuestion.correctTerm;

                let btnClass =
                  'bg-white dark:bg-[#171936] border-2 border-[#EDEFF4] dark:border-[#2E3856] hover:border-[#939BB4] dark:hover:border-[#586380] text-[#586380] dark:text-[#A8B1D1] hover:text-[#282E3E] dark:hover:text-white';
                if (showResult) {
                  if (isCorrectOpt) {
                    btnClass = isUserCorrect
                      ? 'bg-white dark:bg-[#171936] border-2 border-[#10B981] text-[#282E3E] dark:text-white'
                      : 'bg-white dark:bg-[#171936] border-2 border-dashed border-[#10B981] text-[#282E3E] dark:text-white';
                  } else if (isSelected && !isCorrectOpt) {
                    btnClass =
                      'bg-white dark:bg-[#171936] border-2 border-[#F59E0B] text-[#282E3E] dark:text-white';
                  } else {
                    btnClass =
                      'bg-white dark:bg-[#171936] border-2 border-[#EDEFF4] dark:border-[#2E3856] text-[#939BB4] opacity-55';
                  }
                }

                return (
                  <button
                    key={`${activeQuestion.id}-${idx}-${opt}`}
                    type="button"
                    onClick={() => handleSelectOption(opt)}
                    className={`min-h-[58px] px-4 py-3.5 rounded-lg text-left flex items-center gap-3 transition ${btnClass}`}
                  >
                    {showResult && isCorrectOpt ? (
                      <Check className="w-5 h-5 text-[#10B981] stroke-[2.5] shrink-0" />
                    ) : showResult && isSelected && !isCorrectOpt ? (
                      <X className="w-5 h-5 text-[#D97706] stroke-[2.5] shrink-0" />
                    ) : (
                      <span className="w-6 h-6 rounded-full bg-[#F6F7FB] dark:bg-[#2E3856] text-xs font-bold text-[#586380] dark:text-[#A8B1D1] flex items-center justify-center shrink-0">
                        {idx + 1}
                      </span>
                    )}
                    <span className="text-[15px] font-semibold truncate">{opt}</span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Bottom Bar */}
          {!showResult ? (
            <div className="flex items-center justify-center pt-2">
              <button
                type="button"
                onClick={() => setRevealed(true)}
                className="text-sm font-bold text-[#4255FF] dark:text-[#8E9BFF] hover:underline"
              >
                Bạn không biết?
              </button>
            </div>
          ) : (
            <div className="flex items-center justify-between gap-4 pt-2">
              <span className="text-sm font-bold text-[#586380] dark:text-[#A8B1D1]">
                Nhấn phím bất kỳ để tiếp tục
              </span>
              <button
                type="button"
                onClick={handleNextQuestion}
                className="h-10 px-6 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white text-[15px] font-bold transition"
              >
                Tiếp tục
              </button>
            </div>
          )}
        </div>
      </div>
    </section>
  );
}

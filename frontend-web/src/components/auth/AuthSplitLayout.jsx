import React from 'react';
import { Link } from 'react-router-dom';
import { Sparkles, Brain, Trophy, Layers } from 'lucide-react';

/**
 * Quizlet-inspired 2-column authentication shell.
 * Left column showcases brand value & flashcard preview; right column hosts the auth form.
 */
export default function AuthSplitLayout({ mode = 'login', children }) {
  const isLogin = mode === 'login';

  return (
    <div className="min-h-screen flex flex-col lg:flex-row bg-white">
      {/* Left Brand Illustration Panel (Quizlet-style) */}
      <div className="hidden lg:flex lg:w-1/2 relative overflow-hidden bg-gradient-to-br from-[#4255FF] via-[#3446E3] to-[#2E3856] text-white p-12 flex-col justify-between">
        {/* Decorative background blobs */}
        <div className="absolute -top-24 -left-24 w-96 h-96 rounded-full bg-white/10 blur-2xl pointer-events-none" />
        <div className="absolute bottom-10 -right-20 w-80 h-80 rounded-full bg-[#FFCD1F]/20 blur-3xl pointer-events-none" />

        <div className="relative z-10">
          <Link to="/" className="inline-flex items-center gap-3 group">
            <div className="w-11 h-11 rounded-xl bg-white text-quizlet-blue flex items-center justify-center font-black text-xl shadow-lg group-hover:scale-105 transition">
              SF
            </div>
            <span className="text-2xl font-black tracking-tight">SmartFlash</span>
          </Link>

          <h1 className="mt-14 text-4xl xl:text-5xl font-extrabold leading-tight tracking-tight">
            {isLogin
              ? 'Chào mừng trở lại. Tiếp tục chuỗi học của bạn!'
              : 'Học thông minh hơn, ghi nhớ lâu hơn mỗi ngày.'}
          </h1>
          <p className="mt-5 text-blue-100 text-lg max-w-md leading-relaxed">
            Kết hợp lặp lại ngắt quãng <span className="font-semibold text-white">FSRS-4.5</span>, dự đoán trí nhớ{' '}
            <span className="font-semibold text-white">DKT AI</span> và các trò chơi ôn tập chuẩn Quizlet.
          </p>
        </div>

        {/* Floating Flashcard Preview Card */}
        <div className="relative z-10 my-8">
          <div className="bg-white/10 backdrop-blur-md border border-white/20 rounded-2xl p-6 max-w-md shadow-2xl">
            <div className="flex items-center justify-between text-xs font-semibold uppercase tracking-wider text-blue-200 mb-3">
              <span className="inline-flex items-center gap-1.5">
                <Layers className="w-4 h-4 text-[#FFCD1F]" />
                Thẻ ghi nhớ thông minh
              </span>
              <span className="bg-emerald-400/20 text-emerald-200 px-2.5 py-0.5 rounded-full">
                Mastery 92%
              </span>
            </div>
            <p className="text-2xl font-bold text-white">Spaced Repetition</p>
            <p className="mt-2 text-sm text-blue-100">
              Ôn tập đúng thời điểm trí nhớ sắp quên để tối ưu hóa thời gian học.
            </p>
            <div className="mt-5 flex items-center gap-2 text-xs font-semibold">
              <span className="inline-flex items-center gap-1 bg-white/15 px-3 py-1.5 rounded-lg">
                <Brain className="w-3.5 h-3.5 text-[#FFCD1F]" /> FSRS-4.5
              </span>
              <span className="inline-flex items-center gap-1 bg-white/15 px-3 py-1.5 rounded-lg">
                <Sparkles className="w-3.5 h-3.5 text-[#FFCD1F]" /> AI Tutor & OCR
              </span>
              <span className="inline-flex items-center gap-1 bg-white/15 px-3 py-1.5 rounded-lg">
                <Trophy className="w-3.5 h-3.5 text-[#FFCD1F]" /> Live Arena
              </span>
            </div>
          </div>
        </div>

        <div className="relative z-10 text-sm text-blue-200">
          © 2026 SmartFlash — Hệ thống học tập đa nền tảng cá nhân hóa
        </div>
      </div>

      {/* Right Form Panel */}
      <div className="w-full lg:w-1/2 flex flex-col justify-center px-6 py-10 sm:px-12 lg:px-16 xl:px-24">
        <div className="w-full max-w-md mx-auto">
          {/* Mobile Brand Header */}
          <div className="lg:hidden mb-8 flex items-center justify-between">
            <Link to="/" className="inline-flex items-center gap-2.5">
              <div className="w-9 h-9 rounded-xl bg-quizlet-blue text-white flex items-center justify-center font-black text-lg">
                SF
              </div>
              <span className="text-xl font-black text-quizlet-blue">SmartFlash</span>
            </Link>
            <Link to="/" className="text-sm font-semibold text-quizlet-textMuted hover:text-quizlet-textDark">
              Trang chủ
            </Link>
          </div>

          {/* Quizlet-style Sign Up / Log In Tab Switcher */}
          <div className="flex border-b border-gray-200 mb-8">
            <Link
              to="/register"
              className={`pb-3.5 pr-6 text-lg font-bold border-b-2 transition ${
                !isLogin
                  ? 'border-quizlet-blue text-quizlet-textDark'
                  : 'border-transparent text-quizlet-textMuted hover:text-quizlet-textDark'
              }`}
            >
              Đăng ký
            </Link>
            <Link
              to="/login"
              className={`pb-3.5 px-6 text-lg font-bold border-b-2 transition ${
                isLogin
                  ? 'border-quizlet-blue text-quizlet-textDark'
                  : 'border-transparent text-quizlet-textMuted hover:text-quizlet-textDark'
              }`}
            >
              Đăng nhập
            </Link>
          </div>

          {children}
        </div>
      </div>
    </div>
  );
}

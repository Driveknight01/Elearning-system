import React from 'react';
import { Link } from 'react-router-dom';
import { BookOpen, Sparkles, Trophy, Brain } from 'lucide-react';

export default function HomePage() {
  return (
    <div className="min-h-screen bg-quizlet-bg">
      {/* Header */}
      <header className="bg-white border-b border-gray-200 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-quizlet-blue flex items-center justify-center text-white font-bold text-xl shadow-md">
              SF
            </div>
            <span className="text-2xl font-black text-quizlet-blue tracking-tight">SmartFlash</span>
          </div>

          <div className="flex items-center space-x-4">
            <Link
              to="/explore"
              className="text-quizlet-textMuted hover:text-quizlet-textDark font-medium px-3 py-2 rounded-lg transition"
            >
              Khám phá
            </Link>
            <Link
              to="/login"
              className="text-quizlet-blue hover:bg-blue-50 font-semibold px-4 py-2 rounded-lg transition"
            >
              Đăng nhập
            </Link>
            <Link
              to="/register"
              className="bg-quizlet-blue hover:bg-blue-600 text-white font-semibold px-4 py-2 rounded-lg shadow-sm transition"
            >
              Đăng ký
            </Link>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
        <div className="text-center max-w-3xl mx-auto">
          <div className="inline-flex items-center space-x-2 bg-blue-50 text-quizlet-blue px-4 py-1.5 rounded-full text-sm font-semibold mb-6">
            <Sparkles className="w-4 h-4" />
            <span>Thuật toán lặp lại ngắt quãng FSRS-4.5 & Deep Knowledge Tracing (DKT)</span>
          </div>
          <h1 className="text-5xl font-extrabold text-quizlet-textDark tracking-tight leading-tight">
            Ghi nhớ mọi kiến thức nhanh hơn và bền bỉ hơn.
          </h1>
          <p className="mt-6 text-lg text-quizlet-textMuted leading-relaxed">
            Học tập đa nền tảng kết hợp thuật toán trí tuệ nhân tạo, quét ảnh OCR, trợ lý thông minh và 4 minigames thi đấu trực tiếp theo chuẩn Quizlet.
          </p>
          <div className="mt-8 flex justify-center gap-4">
            <Link
              to="/explore"
              className="bg-quizlet-blue hover:bg-blue-600 text-white font-bold px-8 py-3.5 rounded-xl shadow-lg shadow-blue-500/25 transition"
            >
              Bắt đầu học ngay
            </Link>
          </div>
        </div>

        {/* Feature Cards Grid */}
        <div className="mt-20 grid grid-cols-1 md:grid-cols-3 gap-8">
          <div className="bg-white p-8 rounded-2xl shadow-sm border border-gray-100 hover:shadow-md transition">
            <div className="w-12 h-12 rounded-xl bg-blue-100 text-quizlet-blue flex items-center justify-center mb-6">
              <Brain className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-bold text-quizlet-textDark">FSRS-4.5 & DKT Engine</h3>
            <p className="mt-3 text-quizlet-textMuted leading-relaxed">
              Tính toán độ khó, độ ổn định và dự đoán xác suất nhớ từng thẻ để tự động sắp xếp thời gian ôn tập lý tưởng.
            </p>
          </div>

          <div className="bg-white p-8 rounded-2xl shadow-sm border border-gray-100 hover:shadow-md transition">
            <div className="w-12 h-12 rounded-xl bg-yellow-100 text-yellow-600 flex items-center justify-center mb-6">
              <Sparkles className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-bold text-quizlet-textDark">Quét ảnh OCR & AI Tutor</h3>
            <p className="mt-3 text-quizlet-textMuted leading-relaxed">
              Trích xuất thẻ tự động từ ảnh chụp giáo trình bằng OCR và trợ lý AI sinh bộ thẻ chuẩn theo ngữ cảnh.
            </p>
          </div>

          <div className="bg-white p-8 rounded-2xl shadow-sm border border-gray-100 hover:shadow-md transition">
            <div className="w-12 h-12 rounded-xl bg-purple-100 text-purple-600 flex items-center justify-center mb-6">
              <Trophy className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-bold text-quizlet-textDark">Đấu trường Live & Minigames</h3>
            <p className="mt-3 text-quizlet-textMuted leading-relaxed">
              Ghép thẻ, Bắn thiên thạch, Đá quý, Khối hộp và phòng thi đấu trực tiếp qua WebSocket mã PIN 6 số.
            </p>
          </div>
        </div>
      </main>
    </div>
  );
}

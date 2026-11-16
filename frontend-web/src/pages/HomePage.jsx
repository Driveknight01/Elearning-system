import React from 'react';
import { Link } from 'react-router-dom';
import { Sparkles, Trophy, Brain, ArrowRight, UserCheck } from 'lucide-react';
import MainLayout from '../components/layout/MainLayout';
import { useAuthStore } from '../store/useAuthStore';

export default function HomePage() {
  const { isAuthenticated, user } = useAuthStore();

  return (
    <MainLayout>
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-16">
        <div className="text-center max-w-3xl mx-auto">
          <div className="inline-flex items-center space-x-2 bg-blue-50 text-quizlet-blue px-4 py-1.5 rounded-full text-sm font-semibold mb-6 border border-blue-100">
            <Sparkles className="w-4 h-4" />
            <span>Thuật toán lặp lại ngắt quãng FSRS-4.5 & Deep Knowledge Tracing (DKT)</span>
          </div>
          <h1 className="text-4xl sm:text-5xl font-extrabold text-quizlet-textDark tracking-tight leading-tight">
            Ghi nhớ mọi kiến thức nhanh hơn và bền bỉ hơn.
          </h1>
          <p className="mt-6 text-base sm:text-lg text-quizlet-textMuted leading-relaxed">
            Học tập đa nền tảng kết hợp thuật toán trí tuệ nhân tạo, quét ảnh OCR, trợ lý thông minh và 4 minigames thi đấu trực tiếp theo chuẩn Quizlet.
          </p>

          <div className="mt-8 flex flex-wrap justify-center gap-3 sm:gap-4">
            <Link
              to="/explore"
              className="inline-flex items-center gap-2 bg-quizlet-blue hover:bg-blue-600 text-white font-bold px-7 py-3.5 rounded-xl shadow-lg shadow-blue-500/20 transition"
            >
              <span>Bắt đầu học ngay</span>
              <ArrowRight className="w-4 h-4" />
            </Link>

            {isAuthenticated && user?.username ? (
              <Link
                to={`/users/${user.username}`}
                className="inline-flex items-center gap-2 bg-white hover:bg-gray-50 text-quizlet-textDark border border-gray-200 font-bold px-6 py-3.5 rounded-xl transition"
              >
                <UserCheck className="w-4 h-4 text-quizlet-blue" />
                <span>Xem trang cá nhân</span>
              </Link>
            ) : (
              <Link
                to="/register"
                className="inline-flex items-center gap-2 bg-white hover:bg-gray-50 text-quizlet-textDark border border-gray-200 font-bold px-6 py-3.5 rounded-xl transition"
              >
                <span>Tạo tài khoản miễn phí</span>
              </Link>
            )}
          </div>
        </div>

        {/* Feature Cards Grid */}
        <div className="mt-16 grid grid-cols-1 md:grid-cols-3 gap-6 sm:gap-8">
          <div className="bg-white p-7 rounded-2xl shadow-sm border border-gray-200/80 hover:shadow-md transition">
            <div className="w-12 h-12 rounded-xl bg-blue-100 text-quizlet-blue flex items-center justify-center mb-5">
              <Brain className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-bold text-quizlet-textDark">FSRS-4.5 & DKT Engine</h3>
            <p className="mt-2.5 text-sm sm:text-base text-quizlet-textMuted leading-relaxed">
              Tính toán độ khó, độ ổn định và dự đoán xác suất nhớ từng thẻ để tự động sắp xếp thời gian ôn tập lý tưởng.
            </p>
          </div>

          <div className="bg-white p-7 rounded-2xl shadow-sm border border-gray-200/80 hover:shadow-md transition">
            <div className="w-12 h-12 rounded-xl bg-yellow-100 text-yellow-600 flex items-center justify-center mb-5">
              <Sparkles className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-bold text-quizlet-textDark">Quét ảnh OCR & AI Tutor</h3>
            <p className="mt-2.5 text-sm sm:text-base text-quizlet-textMuted leading-relaxed">
              Trích xuất thẻ tự động từ ảnh chụp giáo trình bằng OCR và trợ lý AI sinh bộ thẻ chuẩn theo ngữ cảnh.
            </p>
          </div>

          <div className="bg-white p-7 rounded-2xl shadow-sm border border-gray-200/80 hover:shadow-md transition">
            <div className="w-12 h-12 rounded-xl bg-purple-100 text-purple-600 flex items-center justify-center mb-5">
              <Trophy className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-bold text-quizlet-textDark">Đấu trường Live & Minigames</h3>
            <p className="mt-2.5 text-sm sm:text-base text-quizlet-textMuted leading-relaxed">
              Ghép thẻ, Bắn thiên thạch, Đá quý, Khối hộp và phòng thi đấu trực tiếp qua WebSocket mã PIN 6 số.
            </p>
          </div>
        </div>
      </div>
    </MainLayout>
  );
}

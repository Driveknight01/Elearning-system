import React, { useEffect } from 'react';
import { Routes, Route } from 'react-router-dom';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import ProtectedRoute from './routes/ProtectedRoute';
import { useAuthStore } from './store/useAuthStore';

function RolePlaceholder({ title, description }) {
  return (
    <div className="min-h-screen flex items-center justify-center bg-quizlet-bg px-4">
      <div className="bg-white rounded-2xl border border-gray-200 p-8 max-w-md w-full text-center shadow-sm">
        <h2 className="text-2xl font-extrabold text-quizlet-textDark">{title}</h2>
        <p className="mt-2 text-sm text-quizlet-textMuted">{description}</p>
      </div>
    </div>
  );
}

function App() {
  const { accessToken, user, fetchCurrentUser } = useAuthStore();

  useEffect(() => {
    if (accessToken && !user) {
      fetchCurrentUser();
    }
  }, [accessToken, user, fetchCurrentUser]);

  return (
    <Routes>
      {/* Public / Guest Routes */}
      <Route path="/" element={<HomePage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      {/* Authenticated Learner / Teacher / Admin Routes */}
      <Route
        path="/library"
        element={
          <ProtectedRoute allowedRoles={['USER', 'TEACHER', 'ADMIN']}>
            <RolePlaceholder
              title="Thư viện cá nhân"
              description="Khu vực dành cho người dùng đã đăng nhập (sẽ hoàn thiện ở Phase 3)."
            />
          </ProtectedRoute>
        }
      />

      {/* Teacher + Admin Routes */}
      <Route
        path="/teacher"
        element={
          <ProtectedRoute allowedRoles={['TEACHER', 'ADMIN']}>
            <RolePlaceholder
              title="Không gian Giáo viên"
              description="Quản lý lớp học & chỉ định hoạt động (sẽ hoàn thiện ở Phase 7)."
            />
          </ProtectedRoute>
        }
      />

      {/* Admin-only Routes */}
      <Route
        path="/admin"
        element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <RolePlaceholder
              title="Cổng Quản trị Admin"
              description="Quản trị hệ thống & kiểm duyệt (sẽ hoàn thiện ở Phase 8)."
            />
          </ProtectedRoute>
        }
      />

      <Route
        path="*"
        element={
          <div className="min-h-screen flex items-center justify-center bg-quizlet-bg">
            <div className="text-center">
              <h2 className="text-4xl font-extrabold text-quizlet-textDark">404</h2>
              <p className="mt-2 text-quizlet-textMuted">Trang không tồn tại hoặc đang phát triển</p>
            </div>
          </div>
        }
      />
    </Routes>
  );
}

export default App;

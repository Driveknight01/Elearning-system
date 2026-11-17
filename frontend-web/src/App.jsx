import React, { useEffect } from 'react';
import { Routes, Route } from 'react-router-dom';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import UserProfilePage from './pages/UserProfilePage';
import SettingsPage from './pages/SettingsPage';
import LandingExplorePage from './pages/LandingExplorePage';
import LibraryPage from './pages/LibraryPage';
import ProtectedRoute from './routes/ProtectedRoute';
import MainLayout from './components/layout/MainLayout';
import { useAuthStore } from './store/useAuthStore';
import { useThemeStore } from './store/useThemeStore';

function RolePlaceholder({ title, description }) {
  return (
    <MainLayout>
      <div className="max-w-4xl mx-auto px-4 py-16 flex items-center justify-center">
        <div className="bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] p-8 max-w-md w-full text-center shadow-sm">
          <h2 className="text-2xl font-extrabold text-[#282E3E] dark:text-white">{title}</h2>
          <p className="mt-2 text-sm text-[#586380] dark:text-[#A8B1D1]">{description}</p>
        </div>
      </div>
    </MainLayout>
  );
}

function App() {
  const { accessToken, user, fetchCurrentUser } = useAuthStore();
  useThemeStore();

  useEffect(() => {
    if (accessToken && !user) {
      fetchCurrentUser();
    }
  }, [accessToken, user, fetchCurrentUser]);

  return (
    <Routes>
      {/* Public / Guest Routes */}
      <Route path="/" element={<HomePage />} />
      <Route path="/explore" element={<LandingExplorePage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/users/:username" element={<UserProfilePage />} />
      <Route
        path="/sets/:id"
        element={
          <RolePlaceholder
            title="Chi tiết Bộ thẻ Flashcard"
            description="Trang xem trước thẻ lật, phát âm TTS và chọn chế độ học sẽ hoàn thiện ở task T3.9."
          />
        }
      />

      {/* Authenticated Learner / Teacher / Admin Routes */}
      <Route
        path="/settings"
        element={
          <ProtectedRoute allowedRoles={['USER', 'TEACHER', 'ADMIN']}>
            <SettingsPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/library"
        element={
          <ProtectedRoute allowedRoles={['USER', 'TEACHER', 'ADMIN']}>
            <LibraryPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/sets/create"
        element={
          <ProtectedRoute allowedRoles={['USER', 'TEACHER', 'ADMIN']}>
            <RolePlaceholder
              title="Tạo Bộ thẻ Mới"
              description="Trình soạn thảo bộ thẻ kèm Import Copy-Paste, quét OCR và ảnh AI sẽ hoàn thiện ở task T3.8."
            />
          </ProtectedRoute>
        }
      />

      <Route
        path="/sets/:id/edit"
        element={
          <ProtectedRoute allowedRoles={['USER', 'TEACHER', 'ADMIN']}>
            <RolePlaceholder
              title="Chỉnh sửa Bộ thẻ"
              description="Trình soạn thảo chỉnh sửa bộ thẻ sẽ hoàn thiện ở task T3.8."
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
          <MainLayout>
            <div className="min-h-[calc(100vh-4rem)] flex items-center justify-center px-4">
              <div className="text-center">
                <h2 className="text-4xl font-extrabold text-[#282E3E] dark:text-white">404</h2>
                <p className="mt-2 text-[#586380] dark:text-[#A8B1D1]">Trang không tồn tại hoặc đang phát triển</p>
              </div>
            </div>
          </MainLayout>
        }
      />
    </Routes>
  );
}

export default App;

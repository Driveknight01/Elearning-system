import React from 'react';
import { Link, Navigate, useLocation } from 'react-router-dom';
import { ShieldAlert, ArrowLeft } from 'lucide-react';
import { useAuthStore } from '../store/useAuthStore';

/**
 * Role-based route guard supporting SmartFlash's 4 roles: GUEST, USER, TEACHER, ADMIN.
 *
 * - If {@code requireAuth} is true (default) and the user is not authenticated, redirects to {@code /login}.
 * - If {@code allowedRoles} is non-empty and the user's role is not included, renders a 403 Forbidden view.
 */
export default function ProtectedRoute({
  children,
  allowedRoles = [],
  requireAuth = true,
}) {
  const location = useLocation();
  const { isAuthenticated, user, hasRole } = useAuthStore();

  if (requireAuth && !isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (allowedRoles.length > 0 && !hasRole(allowedRoles)) {
    return (
      <div className="min-h-screen bg-white dark:bg-[#0A082D] flex items-center justify-center px-4">
        <div className="max-w-md w-full bg-white dark:bg-[#202140] rounded-2xl shadow-sm border border-[#EDEFF4] dark:border-[#2E3856] p-8 text-center">
          <div className="w-14 h-14 rounded-2xl bg-red-50 dark:bg-[#431F25] text-quizlet-red flex items-center justify-center mx-auto mb-4">
            <ShieldAlert className="w-7 h-7" />
          </div>
          <h1 className="text-2xl font-extrabold text-[#282E3E] dark:text-white">403 — Không có quyền truy cập</h1>
          <p className="mt-2 text-sm text-[#586380] dark:text-[#A8B1D1]">
            Tài khoản hiện tại ({user?.role || 'GUEST'}) không có quyền mở trang này.
          </p>
          <Link
            to="/"
            className="mt-6 inline-flex items-center gap-2 bg-[#4255FF] hover:bg-[#3545E0] text-white font-semibold px-5 py-2.5 rounded-full transition"
          >
            <ArrowLeft className="w-4 h-4" />
            Quay về Trang chủ
          </Link>
        </div>
      </div>
    );
  }

  return children;
}

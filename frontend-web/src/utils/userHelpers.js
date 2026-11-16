/**
 * Shared presentation and error-formatting helpers for user profiles and layout menus.
 */

export const ROLE_META = {
  USER: {
    label: 'Học viên',
    badgeClass: 'bg-blue-50 text-quizlet-blue border-blue-200',
  },
  TEACHER: {
    label: 'Giáo viên',
    badgeClass: 'bg-purple-50 text-quizlet-purple border-purple-200',
  },
  ADMIN: {
    label: 'Quản trị viên',
    badgeClass: 'bg-amber-50 text-amber-700 border-amber-200',
  },
  GUEST: {
    label: 'Khách',
    badgeClass: 'bg-gray-100 text-quizlet-textMuted border-gray-200',
  },
};

export const VISIBILITY_META = {
  PUBLIC: {
    label: 'Công khai',
    description: 'Mọi người (kể cả Khách chưa đăng nhập) đều có thể xem trang cá nhân này.',
  },
  LOGGED_IN_ONLY: {
    label: 'Chỉ thành viên',
    description: 'Chỉ người dùng đã đăng nhập SmartFlash mới xem được trang cá nhân này.',
  },
  PRIVATE: {
    label: 'Riêng tư',
    description: 'Chỉ mình bạn (và Quản trị viên) có thể xem trang cá nhân này.',
  },
};

export function getRoleMeta(role) {
  return ROLE_META[role] || ROLE_META.USER;
}

export function getInitials(fullName, username) {
  const source = (fullName || username || 'SF').trim();
  const parts = source.split(/\s+/).filter(Boolean);
  if (parts.length >= 2) {
    return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase();
  }
  return source.slice(0, 2).toUpperCase();
}

export function extractApiErrorMessage(err, fallback = 'Đã có lỗi xảy ra. Vui lòng thử lại.') {
  if (!err) return fallback;
  if (err.errors && typeof err.errors === 'object') {
    const firstFieldMsg = Object.values(err.errors).find((v) => typeof v === 'string' && v.trim());
    if (firstFieldMsg) return firstFieldMsg;
  }
  if (typeof err.message === 'string' && err.message.trim()) {
    return err.message;
  }
  return fallback;
}

export function formatJoinedDate(isoString) {
  if (!isoString) return '';
  try {
    return new Intl.DateTimeFormat('vi-VN', {
      month: 'long',
      year: 'numeric',
    }).format(new Date(isoString));
  } catch {
    return '';
  }
}

export function formatShortDate(isoString) {
  if (!isoString) return '';
  try {
    return new Intl.DateTimeFormat('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    }).format(new Date(isoString));
  } catch {
    return '';
  }
}

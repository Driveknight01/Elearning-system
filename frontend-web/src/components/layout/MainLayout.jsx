import React, { useEffect, useRef, useState } from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import {
  Menu,
  X,
  Search,
  Home,
  Compass,
  FolderKanban,
  GraduationCap,
  ShieldCheck,
  User as UserIcon,
  Settings,
  LogOut,
  Plus,
  Bell,
  Flame,
  ChevronDown,
  BadgeCheck,
  Sparkles,
} from 'lucide-react';
import { useAuthStore } from '../../store/useAuthStore';
import UserAvatar from '../common/UserAvatar';
import { getRoleMeta } from '../../utils/userHelpers';

export default function MainLayout({ children }) {
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated, user, logout } = useAuthStore();

  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [mobileDrawerOpen, setMobileDrawerOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [accountMenuOpen, setAccountMenuOpen] = useState(false);
  const [bellOpen, setBellOpen] = useState(false);

  const accountMenuRef = useRef(null);
  const bellMenuRef = useRef(null);
  const searchInputRef = useRef(null);

  const role = isAuthenticated ? user?.role || 'USER' : 'GUEST';
  const roleMeta = getRoleMeta(role);

  useEffect(() => {
    setMobileDrawerOpen(false);
    setAccountMenuOpen(false);
    setBellOpen(false);
  }, [location.pathname, location.search]);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (accountMenuRef.current && !accountMenuRef.current.contains(event.target)) {
        setAccountMenuOpen(false);
      }
      if (bellMenuRef.current && !bellMenuRef.current.contains(event.target)) {
        setBellOpen(false);
      }
    };
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        setAccountMenuOpen(false);
        setBellOpen(false);
        setMobileDrawerOpen(false);
      }
      if (
        event.key === '/' &&
        document.activeElement?.tagName !== 'INPUT' &&
        document.activeElement?.tagName !== 'TEXTAREA' &&
        document.activeElement?.tagName !== 'SELECT'
      ) {
        event.preventDefault();
        searchInputRef.current?.focus();
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, []);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    const trimmed = searchQuery.trim();
    if (!trimmed) {
      navigate('/explore');
      return;
    }
    if (trimmed.startsWith('@') && trimmed.length > 1) {
      navigate(`/users/${encodeURIComponent(trimmed.slice(1))}`);
      return;
    }
    navigate(`/explore?q=${encodeURIComponent(trimmed)}`);
  };

  const handleLogout = async () => {
    setAccountMenuOpen(false);
    await logout();
    navigate('/');
  };

  const primaryNavItems = [
    { to: '/', label: 'Trang chủ', icon: Home, end: true },
    { to: '/explore', label: 'Khám phá', icon: Compass },
    ...(isAuthenticated
      ? [{ to: '/library', label: 'Thư viện của tôi', icon: FolderKanban }]
      : []),
  ];

  const workspaceNavItems = [
    ...(role === 'TEACHER' || role === 'ADMIN'
      ? [{ to: '/teacher', label: 'Lớp học & Giao bài', icon: GraduationCap }]
      : []),
    ...(role === 'ADMIN'
      ? [{ to: '/admin', label: 'Cổng Quản trị', icon: ShieldCheck }]
      : []),
  ];

  const profilePath = user?.username ? `/users/${user.username}` : '/login';

  const renderNavLink = (item, collapsed = false) => {
    const Icon = item.icon;
    return (
      <NavLink
        key={item.to}
        to={item.to}
        end={item.end}
        title={collapsed ? item.label : undefined}
        className={({ isActive }) =>
          `flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-semibold transition ${
            isActive
              ? 'bg-blue-50 text-quizlet-blue'
              : 'text-quizlet-textMuted hover:bg-gray-100 hover:text-quizlet-textDark'
          } ${collapsed ? 'justify-center px-2.5' : ''}`
        }
      >
        <Icon className="w-5 h-5 shrink-0" />
        {!collapsed && <span className="truncate">{item.label}</span>}
      </NavLink>
    );
  };

  return (
    <div className="min-h-screen bg-quizlet-bg flex flex-col">
      {/* Sticky Quizlet-style Topbar */}
      <header className="sticky top-0 z-40 h-16 bg-white border-b border-gray-200 px-3 sm:px-6 flex items-center justify-between gap-3">
        {/* Left: Sidebar Toggle + Brand */}
        <div className="flex items-center gap-2 sm:gap-3 shrink-0">
          <button
            type="button"
            onClick={() => {
              if (window.innerWidth < 1024) {
                setMobileDrawerOpen((prev) => !prev);
              } else {
                setSidebarCollapsed((prev) => !prev);
              }
            }}
            aria-label="Mở hoặc thu gọn thanh điều hướng"
            className="p-2 rounded-xl text-quizlet-textMuted hover:bg-gray-100 hover:text-quizlet-textDark transition"
          >
            <Menu className="w-5 h-5" />
          </button>

          <Link to="/" className="flex items-center gap-2.5 group">
            <div className="w-9 h-9 rounded-xl bg-quizlet-blue text-white flex items-center justify-center font-black text-base shadow-sm group-hover:scale-105 transition">
              SF
            </div>
            <span className="text-xl font-black text-quizlet-blue tracking-tight hidden sm:inline">
              SmartFlash
            </span>
          </Link>
        </div>

        {/* Center: Search Bar */}
        <form
          onSubmit={handleSearchSubmit}
          className="flex-1 max-w-xl mx-1 sm:mx-4 relative"
        >
          <Search className="w-4 h-4 text-quizlet-textMuted absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
          <input
            ref={searchInputRef}
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Tìm bộ thẻ, chủ đề hoặc @username..."
            className="w-full h-10 pl-10 pr-14 rounded-full bg-quizlet-bg border border-transparent focus:bg-white focus:border-quizlet-blue focus:ring-2 focus:ring-blue-100 text-sm text-quizlet-textDark placeholder:text-quizlet-textMuted/80 outline-none transition"
          />
          {searchQuery ? (
            <button
              type="button"
              onClick={() => setSearchQuery('')}
              aria-label="Xóa từ khóa"
              className="absolute right-3 top-1/2 -translate-y-1/2 p-1 rounded-full text-quizlet-textMuted hover:text-quizlet-textDark hover:bg-gray-200/70 transition"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          ) : (
            <kbd className="hidden md:inline-flex items-center justify-center absolute right-3 top-1/2 -translate-y-1/2 px-1.5 py-0.5 text-[11px] font-bold text-quizlet-textMuted bg-white border border-gray-200 rounded-md">
              /
            </kbd>
          )}
        </form>

        {/* Right: Quick Create, Streak, Notifications, Account Menu */}
        <div className="flex items-center gap-2 sm:gap-2.5 shrink-0">
          {isAuthenticated && user ? (
            <>
              <Link
                to="/library"
                title="Tạo bộ thẻ hoặc thư mục mới"
                className="inline-flex items-center gap-1.5 h-9 px-3 rounded-xl bg-quizlet-blue hover:bg-blue-600 text-white text-sm font-bold shadow-sm transition"
              >
                <Plus className="w-4 h-4" />
                <span className="hidden md:inline">Tạo</span>
              </Link>

              <Link
                to={profilePath}
                title="Chuỗi ngày học hiện tại"
                className="hidden sm:inline-flex items-center gap-1 h-9 px-2.5 rounded-xl bg-amber-50 border border-amber-200/80 text-amber-700 text-xs font-extrabold hover:bg-amber-100/70 transition"
              >
                <Flame className="w-4 h-4 text-orange-500 fill-orange-400" />
                <span>{user.currentStreak ?? 0}</span>
              </Link>

              {/* Notification Bell */}
              <div className="relative" ref={bellMenuRef}>
                <button
                  type="button"
                  onClick={() => setBellOpen((prev) => !prev)}
                  aria-label="Thông báo"
                  className="w-9 h-9 rounded-xl border border-gray-200 bg-white hover:bg-gray-50 flex items-center justify-center text-quizlet-textMuted hover:text-quizlet-textDark transition"
                >
                  <Bell className="w-4 h-4" />
                </button>

                {bellOpen && (
                  <div className="absolute right-0 mt-2 w-72 bg-white rounded-2xl border border-gray-200 shadow-xl p-4 z-50">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-sm font-bold text-quizlet-textDark">Thông báo</span>
                      <span className="text-[11px] font-semibold text-quizlet-blue bg-blue-50 px-2 py-0.5 rounded-full">
                        Mới
                      </span>
                    </div>
                    <p className="text-xs text-quizlet-textMuted leading-relaxed">
                      Chào mừng <span className="font-semibold text-quizlet-textDark">{user.fullName || user.username}</span>! Hệ thống thông báo nhắc học FSRS và bảng tin lớp học sẽ mở ở các Phase tiếp theo.
                    </p>
                  </div>
                )}
              </div>

              {/* Account Dropdown */}
              <div className="relative" ref={accountMenuRef}>
                <button
                  type="button"
                  onClick={() => setAccountMenuOpen((prev) => !prev)}
                  aria-expanded={accountMenuOpen}
                  aria-haspopup="menu"
                  className="inline-flex items-center gap-2 pl-1.5 pr-2.5 py-1 rounded-full border border-gray-200 bg-white hover:bg-gray-50 transition"
                >
                  <UserAvatar
                    src={user.avatarUrl}
                    fullName={user.fullName}
                    username={user.username}
                    size="xs"
                  />
                  <span className="text-sm font-bold text-quizlet-textDark max-w-[110px] truncate hidden md:inline">
                    {user.fullName || user.username}
                  </span>
                  <ChevronDown
                    className={`w-4 h-4 text-quizlet-textMuted transition-transform ${
                      accountMenuOpen ? 'rotate-180' : ''
                    }`}
                  />
                </button>

                {accountMenuOpen && (
                  <div
                    role="menu"
                    className="absolute right-0 mt-2 w-72 bg-white rounded-2xl border border-gray-200 shadow-xl overflow-hidden z-50"
                  >
                    {/* User Summary Header */}
                    <div className="p-4 bg-gradient-to-br from-quizlet-bg to-white border-b border-gray-100">
                      <div className="flex items-center gap-3">
                        <UserAvatar
                          src={user.avatarUrl}
                          fullName={user.fullName}
                          username={user.username}
                          size="md"
                        />
                        <div className="min-w-0 flex-1">
                          <div className="flex items-center gap-1">
                            <p className="text-sm font-extrabold text-quizlet-textDark truncate">
                              {user.fullName || user.username}
                            </p>
                            {user.verifiedTeacher && (
                              <BadgeCheck
                                className="w-4 h-4 text-quizlet-blue shrink-0"
                                title="Giáo viên đã xác minh"
                              />
                            )}
                          </div>
                          <p className="text-xs text-quizlet-textMuted truncate">@{user.username}</p>
                          <span
                            className={`mt-1.5 inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-bold border ${roleMeta.badgeClass}`}
                          >
                            {roleMeta.label}
                          </span>
                        </div>
                      </div>
                    </div>

                    {/* Menu Links */}
                    <div className="p-2 space-y-0.5">
                      <Link
                        to={profilePath}
                        role="menuitem"
                        className="flex items-center gap-2.5 px-3 py-2 rounded-xl text-sm font-semibold text-quizlet-textDark hover:bg-blue-50 hover:text-quizlet-blue transition"
                      >
                        <UserIcon className="w-4 h-4" />
                        <span>Trang cá nhân</span>
                      </Link>
                      <Link
                        to={`${profilePath}?tab=settings`}
                        role="menuitem"
                        className="flex items-center gap-2.5 px-3 py-2 rounded-xl text-sm font-semibold text-quizlet-textDark hover:bg-blue-50 hover:text-quizlet-blue transition"
                      >
                        <Settings className="w-4 h-4" />
                        <span>Cài đặt hồ sơ & Quyền riêng tư</span>
                      </Link>
                      <Link
                        to="/library"
                        role="menuitem"
                        className="flex items-center gap-2.5 px-3 py-2 rounded-xl text-sm font-semibold text-quizlet-textDark hover:bg-blue-50 hover:text-quizlet-blue transition"
                      >
                        <FolderKanban className="w-4 h-4" />
                        <span>Thư viện của tôi</span>
                      </Link>
                      {(role === 'TEACHER' || role === 'ADMIN') && (
                        <Link
                          to="/teacher"
                          role="menuitem"
                          className="flex items-center gap-2.5 px-3 py-2 rounded-xl text-sm font-semibold text-quizlet-textDark hover:bg-purple-50 hover:text-quizlet-purple transition"
                        >
                          <GraduationCap className="w-4 h-4" />
                          <span>Không gian Giáo viên</span>
                        </Link>
                      )}
                      {role === 'ADMIN' && (
                        <Link
                          to="/admin"
                          role="menuitem"
                          className="flex items-center gap-2.5 px-3 py-2 rounded-xl text-sm font-semibold text-quizlet-textDark hover:bg-amber-50 hover:text-amber-700 transition"
                        >
                          <ShieldCheck className="w-4 h-4" />
                          <span>Cổng Quản trị Admin</span>
                        </Link>
                      )}
                    </div>

                    <div className="p-2 border-t border-gray-100">
                      <button
                        type="button"
                        onClick={handleLogout}
                        role="menuitem"
                        className="w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-sm font-semibold text-quizlet-red hover:bg-red-50 transition"
                      >
                        <LogOut className="w-4 h-4" />
                        <span>Đăng xuất</span>
                      </button>
                    </div>
                  </div>
                )}
              </div>
            </>
          ) : (
            <div className="flex items-center gap-2">
              <Link
                to="/login"
                className="text-sm font-bold text-quizlet-blue hover:bg-blue-50 px-3.5 py-2 rounded-xl transition"
              >
                Đăng nhập
              </Link>
              <Link
                to="/register"
                className="text-sm font-bold bg-quizlet-blue hover:bg-blue-600 text-white px-4 py-2 rounded-xl shadow-sm transition"
              >
                Đăng ký
              </Link>
            </div>
          )}
        </div>
      </header>

      {/* Body: Left Sidebar + Main Content */}
      <div className="flex flex-1 min-h-0">
        {/* Desktop Sidebar */}
        <aside
          className={`hidden lg:flex flex-col justify-between bg-white border-r border-gray-200 sticky top-16 h-[calc(100vh-4rem)] transition-all duration-200 shrink-0 ${
            sidebarCollapsed ? 'w-20 px-2.5 py-4' : 'w-64 p-4'
          }`}
        >
          <div className="space-y-6 overflow-y-auto">
            <nav className="space-y-1">
              {primaryNavItems.map((item) => renderNavLink(item, sidebarCollapsed))}
            </nav>

            {workspaceNavItems.length > 0 && (
              <div>
                {!sidebarCollapsed && (
                  <p className="px-3.5 mb-2 text-[11px] font-extrabold uppercase tracking-wider text-quizlet-textMuted">
                    Không gian chuyên biệt
                  </p>
                )}
                <nav className="space-y-1">
                  {workspaceNavItems.map((item) => renderNavLink(item, sidebarCollapsed))}
                </nav>
              </div>
            )}

            {isAuthenticated && user && (
              <div>
                {!sidebarCollapsed && (
                  <p className="px-3.5 mb-2 text-[11px] font-extrabold uppercase tracking-wider text-quizlet-textMuted">
                    Tài khoản
                  </p>
                )}
                <nav className="space-y-1">
                  {renderNavLink(
                    { to: profilePath, label: 'Hồ sơ cá nhân', icon: UserIcon, end: true },
                    sidebarCollapsed
                  )}
                </nav>
              </div>
            )}
          </div>

          {/* Bottom Sidebar Summary / CTA */}
          {!sidebarCollapsed && (
            <div className="pt-4 border-t border-gray-100">
              {isAuthenticated && user ? (
                <Link
                  to={`${profilePath}?tab=settings`}
                  className="block p-3.5 rounded-2xl bg-gradient-to-br from-blue-50 to-indigo-50/70 border border-blue-100 hover:border-blue-200 transition"
                >
                  <div className="flex items-center justify-between text-xs font-bold text-quizlet-blue">
                    <span>Mục tiêu mỗi ngày</span>
                    <Flame className="w-4 h-4 text-orange-500" />
                  </div>
                  <p className="mt-1 text-base font-extrabold text-quizlet-textDark">
                    {user.dailyGoalCards || 20} thẻ / ngày
                  </p>
                  <p className="mt-0.5 text-[11px] text-quizlet-textMuted">
                    Nhấn để tùy chỉnh mục tiêu & hồ sơ
                  </p>
                </Link>
              ) : (
                <div className="p-3.5 rounded-2xl bg-gradient-to-br from-quizlet-blue to-quizlet-darkBlue text-white">
                  <div className="flex items-center gap-1.5 text-xs font-bold text-quizlet-yellow">
                    <Sparkles className="w-4 h-4" />
                    <span>SmartFlash AI</span>
                  </div>
                  <p className="mt-1 text-sm font-bold leading-snug">
                    Ghi nhớ thông minh với FSRS-4.5 & DKT
                  </p>
                  <Link
                    to="/register"
                    className="mt-3 inline-flex w-full items-center justify-center py-2 rounded-xl bg-white text-quizlet-blue text-xs font-extrabold hover:bg-blue-50 transition"
                  >
                    Tạo tài khoản miễn phí
                  </Link>
                </div>
              )}
            </div>
          )}
        </aside>

        {/* Mobile Drawer Overlay */}
        {mobileDrawerOpen && (
          <div className="fixed inset-0 z-50 lg:hidden flex">
            <div
              className="fixed inset-0 bg-quizlet-textDark/40 backdrop-blur-[1px]"
              onClick={() => setMobileDrawerOpen(false)}
            />
            <aside className="relative w-72 max-w-[85vw] bg-white h-full shadow-2xl flex flex-col justify-between p-4 z-10">
              <div>
                <div className="flex items-center justify-between pb-4 mb-4 border-b border-gray-100">
                  <Link to="/" className="flex items-center gap-2.5">
                    <div className="w-9 h-9 rounded-xl bg-quizlet-blue text-white flex items-center justify-center font-black text-base">
                      SF
                    </div>
                    <span className="text-xl font-black text-quizlet-blue">SmartFlash</span>
                  </Link>
                  <button
                    type="button"
                    onClick={() => setMobileDrawerOpen(false)}
                    className="p-2 rounded-xl text-quizlet-textMuted hover:bg-gray-100"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>

                <nav className="space-y-1">
                  {primaryNavItems.map((item) => renderNavLink(item, false))}
                </nav>

                {workspaceNavItems.length > 0 && (
                  <div className="mt-6">
                    <p className="px-3.5 mb-2 text-[11px] font-extrabold uppercase tracking-wider text-quizlet-textMuted">
                      Không gian chuyên biệt
                    </p>
                    <nav className="space-y-1">
                      {workspaceNavItems.map((item) => renderNavLink(item, false))}
                    </nav>
                  </div>
                )}

                {isAuthenticated && user && (
                  <div className="mt-6">
                    <p className="px-3.5 mb-2 text-[11px] font-extrabold uppercase tracking-wider text-quizlet-textMuted">
                      Tài khoản
                    </p>
                    <nav className="space-y-1">
                      {renderNavLink(
                        { to: profilePath, label: 'Hồ sơ cá nhân', icon: UserIcon, end: true },
                        false
                      )}
                    </nav>
                  </div>
                )}
              </div>

              <div className="pt-4 border-t border-gray-100">
                {isAuthenticated ? (
                  <button
                    type="button"
                    onClick={handleLogout}
                    className="w-full flex items-center gap-2.5 px-3.5 py-2.5 rounded-xl text-sm font-semibold text-quizlet-red hover:bg-red-50 transition"
                  >
                    <LogOut className="w-5 h-5" />
                    <span>Đăng xuất</span>
                  </button>
                ) : (
                  <div className="grid grid-cols-2 gap-2">
                    <Link
                      to="/login"
                      className="text-center py-2.5 rounded-xl border border-gray-200 text-sm font-bold text-quizlet-textDark hover:bg-gray-50"
                    >
                      Đăng nhập
                    </Link>
                    <Link
                      to="/register"
                      className="text-center py-2.5 rounded-xl bg-quizlet-blue text-sm font-bold text-white hover:bg-blue-600"
                    >
                      Đăng ký
                    </Link>
                  </div>
                )}
              </div>
            </aside>
          </div>
        )}

        {/* Main Workspace */}
        <main className="flex-1 min-w-0">{children}</main>
      </div>
    </div>
  );
}

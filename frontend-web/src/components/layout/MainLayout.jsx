import React, { useEffect, useRef, useState } from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import {
  Menu,
  X,
  Search,
  Home,
  Folder,
  Users,
  Bell,
  GraduationCap,
  Layers,
  Gamepad2,
  Plus,
  Trophy,
  Settings,
  Moon,
  Sun,
  LogOut,
  Shield,
  ShieldCheck,
  User as UserIcon,
} from 'lucide-react';
import { useAuthStore } from '../../store/useAuthStore';
import { useThemeStore } from '../../store/useThemeStore';
import UserAvatar from '../common/UserAvatar';
import smartflashLogo from '../../assets/smartflash-logo.png';

export default function MainLayout({
  children,
  hideSidebar = false,
  stickyHeader = true,
}) {
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated, user, logout } = useAuthStore();
  const { theme, toggleTheme } = useThemeStore();
  const isDark = theme === 'dark';

  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [mobileDrawerOpen, setMobileDrawerOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [createMenuOpen, setCreateMenuOpen] = useState(false);
  const [accountMenuOpen, setAccountMenuOpen] = useState(false);
  const [notifOpen, setNotifOpen] = useState(false);

  const createMenuRef = useRef(null);
  const accountMenuRef = useRef(null);
  const searchInputRef = useRef(null);

  const role = isAuthenticated ? user?.role || 'USER' : 'GUEST';
  const profilePath = user?.username ? `/users/${user.username}` : '/login';

  useEffect(() => {
    setMobileDrawerOpen(false);
    setCreateMenuOpen(false);
    setAccountMenuOpen(false);
    setNotifOpen(false);
  }, [location.pathname, location.search]);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (createMenuRef.current && !createMenuRef.current.contains(event.target)) {
        setCreateMenuOpen(false);
      }
      if (accountMenuRef.current && !accountMenuRef.current.contains(event.target)) {
        setAccountMenuOpen(false);
      }
    };
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        setCreateMenuOpen(false);
        setAccountMenuOpen(false);
        setNotifOpen(false);
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
    {
      to: isAuthenticated ? '/library' : '/explore',
      label: 'Thư viện của bạn',
      icon: Folder,
    },
    ...(isAuthenticated
      ? [{ to: '/library?tab=groups', label: 'Nhóm học', icon: Users }]
      : []),
  ];

  const startHereItems = [
    { to: '/explore?mode=test', label: 'Bài thi', icon: GraduationCap },
    { to: '/explore?mode=flashcards', label: 'Thẻ ghi nhớ', icon: Layers },
    { to: '/explore?mode=games', label: 'Trò chơi', icon: Gamepad2 },
  ];

  const workspaceItems = [
    ...(role === 'TEACHER' || role === 'ADMIN'
      ? [{ to: '/teacher', label: 'Lớp học & Giao bài', icon: GraduationCap }]
      : []),
    ...(role === 'ADMIN'
      ? [{ to: '/admin', label: 'Cổng Quản trị', icon: ShieldCheck }]
      : []),
  ];

  // Slimmer vertical height (h-9 = 36px) & fixed left icon slot (w-10) so icons never shift when expanding
  const renderFixedRailItem = ({ to, label, icon: Icon, end }, collapsed) => (
    <NavLink
      key={label}
      to={to}
      end={end}
      title={collapsed ? label : undefined}
      className={({ isActive }) => {
        const active =
          isActive &&
          !(to.includes('?') && `${location.pathname}${location.search}` !== to);
        return `group flex items-center h-9 rounded-lg text-[14px] font-semibold transition-colors ${
          active
            ? 'bg-[#EDEFFF] text-[#4255FF] dark:bg-[#2E3856] dark:text-white'
            : 'text-[#586380] hover:bg-[#F6F7FB] hover:text-[#282E3E] dark:text-[#A8B1D1] dark:hover:bg-[#202140] dark:hover:text-white'
        }`;
      }}
    >
      <span className="w-10 h-9 shrink-0 flex items-center justify-center">
        <Icon className="w-[18px] h-[18px]" />
      </span>
      <span
        className={`whitespace-nowrap overflow-hidden transition-all duration-200 ${
          collapsed ? 'w-0 opacity-0' : 'pr-3 opacity-100'
        }`}
      >
        {label}
      </span>
    </NavLink>
  );

  return (
    <div className="min-h-screen bg-white dark:bg-[#0A082D] text-[#282E3E] dark:text-[#F6F7FB] flex flex-col">
      {/* Top Header (No Upgrade button; includes quick Light/Dark toggle) */}
      <header
        className={`${
          stickyHeader ? 'sticky top-0 z-40' : 'relative z-40'
        } h-16 bg-white dark:bg-[#0A082D] px-3 sm:px-4 flex items-center justify-between gap-3`}
      >
        {/* Left: Fixed Hamburger + Brand Mark */}
        <div className="flex items-center gap-2 shrink-0">
          <button
            type="button"
            onClick={() => {
              if (hideSidebar || window.innerWidth < 1024) {
                setMobileDrawerOpen((prev) => !prev);
              } else {
                setSidebarCollapsed((prev) => !prev);
              }
            }}
            aria-label="Mở hoặc thu gọn thanh điều hướng"
            className="w-10 h-10 rounded-full flex items-center justify-center text-[#586380] hover:bg-[#F6F7FB] hover:text-[#282E3E] dark:text-[#A8B1D1] dark:hover:bg-[#202140] dark:hover:text-white transition-colors"
          >
            <Menu className="w-5 h-5" />
          </button>

          <Link to="/" className="flex items-center gap-2 group" aria-label="ELearner Trang chủ">
            <img
              src={smartflashLogo}
              alt="ELearner"
              className="w-8 h-8 rounded-lg object-contain group-hover:scale-105 transition-transform"
            />
            <span className="text-[21px] leading-none font-semibold tracking-[-0.01em] text-[#282E3E] dark:text-white hidden sm:inline">
              ELearner
            </span>
          </Link>
        </div>

        {/* Center: Search Bar (~15% shorter) */}
        <form onSubmit={handleSearchSubmit} className="flex-1 max-w-[570px] mx-2 sm:mx-6 relative">
          <Search className="w-[18px] h-[18px] text-[#586380] dark:text-[#A8B1D1] absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
          <input
            ref={searchInputRef}
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Tìm bộ thẻ, câu hỏi hoặc @username"
            className="w-full h-10 pl-10 pr-10 rounded-lg bg-[#F6F7FB] dark:bg-[#2E3856] border border-transparent focus:bg-white dark:focus:bg-[#202140] focus:border-[#D9DDE8] dark:focus:border-[#4255FF] text-[14px] font-medium text-[#282E3E] dark:text-white placeholder:text-[#586380] dark:placeholder:text-[#A8B1D1] outline-none transition-colors"
          />
          {searchQuery && (
            <button
              type="button"
              onClick={() => setSearchQuery('')}
              aria-label="Xóa từ khóa"
              className="absolute right-2.5 top-1/2 -translate-y-1/2 p-1 rounded-full text-[#586380] hover:text-[#282E3E] dark:text-[#A8B1D1] dark:hover:text-white"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
        </form>

        {/* Right: Create (+), Avatar Dropdown */}
        <div className="flex items-center gap-2.5 shrink-0">
          {isAuthenticated && user ? (
            <>
              <div className="relative" ref={createMenuRef}>
                <button
                  type="button"
                  onClick={() => setCreateMenuOpen((prev) => !prev)}
                  title="Tạo mới"
                  aria-label="Tạo bộ thẻ hoặc thư mục mới"
                  aria-expanded={createMenuOpen}
                  className="w-10 h-10 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white flex items-center justify-center transition-colors"
                >
                  <Plus className="w-5 h-5 stroke-[2.5]" />
                </button>

                {createMenuOpen && (
                  <div
                    role="menu"
                    className="absolute right-0 mt-2 w-56 bg-white dark:bg-[#0A082D] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] shadow-xl py-1.5 z-50"
                  >
                    <Link
                      to="/sets/create"
                      role="menuitem"
                      className="flex items-center gap-3 px-4 py-2.5 text-[14px] font-bold text-[#282E3E] dark:text-[#F6F7FB] hover:bg-[#F6F7FB] dark:hover:bg-[#202140] transition-colors"
                    >
                      <Layers className="w-[18px] h-[18px] text-[#4255FF]" />
                      <span>Học phần (Bộ thẻ)</span>
                    </Link>
                    <Link
                      to="/library?tab=folders"
                      role="menuitem"
                      className="flex items-center gap-3 px-4 py-2.5 text-[14px] font-bold text-[#282E3E] dark:text-[#F6F7FB] hover:bg-[#F6F7FB] dark:hover:bg-[#202140] transition-colors"
                    >
                      <Folder className="w-[18px] h-[18px] text-[#586380] dark:text-[#A8B1D1]" />
                      <span>Thư mục</span>
                    </Link>
                    <Link
                      to="/library?tab=groups"
                      role="menuitem"
                      className="flex items-center gap-3 px-4 py-2.5 text-[14px] font-bold text-[#282E3E] dark:text-[#F6F7FB] hover:bg-[#F6F7FB] dark:hover:bg-[#202140] transition-colors"
                    >
                      <Users className="w-[18px] h-[18px] text-[#586380] dark:text-[#A8B1D1]" />
                      <span>Nhóm học</span>
                    </Link>
                  </div>
                )}
              </div>

              {/* Avatar & Dropdown */}
              <div className="relative" ref={accountMenuRef}>
                <button
                  type="button"
                  onClick={() => setAccountMenuOpen((prev) => !prev)}
                  aria-expanded={accountMenuOpen}
                  aria-haspopup="menu"
                  aria-label="Menu tài khoản"
                  className="rounded-full p-0.5 hover:ring-2 hover:ring-[#D9DDE8] dark:hover:ring-[#4255FF] transition"
                >
                  <UserAvatar
                    src={user.avatarUrl}
                    fullName={user.fullName}
                    username={user.username}
                    size="sm"
                  />
                </button>

                {accountMenuOpen && (
                  <div
                    role="menu"
                    className="absolute right-0 mt-2 w-64 bg-white dark:bg-[#0A082D] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] shadow-xl overflow-hidden z-50"
                  >
                    <Link
                      to={profilePath}
                      className="p-4 flex items-center gap-3 border-b border-[#EDEFF4] dark:border-[#2E3856] hover:bg-[#F6F7FB] dark:hover:bg-[#202140] transition-colors"
                    >
                      <UserAvatar
                        src={user.avatarUrl}
                        fullName={user.fullName}
                        username={user.username}
                        size="md"
                      />
                      <div className="min-w-0 flex-1">
                        <p className="text-[14px] font-bold text-[#282E3E] dark:text-white truncate">
                          {user.username || user.fullName}
                        </p>
                        <p className="text-[12px] text-[#586380] dark:text-[#A8B1D1] truncate">
                          {user.email || `@${user.username}`}
                        </p>
                      </div>
                    </Link>

                    <div className="py-1.5 border-b border-[#EDEFF4] dark:border-[#2E3856]">
                      <Link
                        to={`${profilePath}?tab=achievements`}
                        role="menuitem"
                        className="flex items-center gap-3 px-4 py-2.5 text-[14px] font-semibold text-[#282E3E] dark:text-[#F6F7FB] hover:bg-[#F6F7FB] dark:hover:bg-[#202140] transition-colors"
                      >
                        <Trophy className="w-[18px] h-[18px] text-[#586380] dark:text-[#A8B1D1]" />
                        <span>Thành tựu</span>
                      </Link>
                      <Link
                        to="/settings"
                        role="menuitem"
                        className="flex items-center gap-3 px-4 py-2.5 text-[14px] font-semibold text-[#282E3E] dark:text-[#F6F7FB] hover:bg-[#F6F7FB] dark:hover:bg-[#202140] transition-colors"
                      >
                        <Settings className="w-[18px] h-[18px] text-[#586380] dark:text-[#A8B1D1]" />
                        <span>Cài đặt</span>
                      </Link>
                      <button
                        type="button"
                        onClick={toggleTheme}
                        role="menuitemcheckbox"
                        aria-checked={isDark}
                        className="w-full flex items-center justify-between gap-3 px-4 py-2.5 text-[14px] font-semibold text-[#282E3E] dark:text-[#F6F7FB] hover:bg-[#F6F7FB] dark:hover:bg-[#202140] transition-colors"
                      >
                        <span className="flex items-center gap-3">
                          {isDark ? (
                            <Sun className="w-[18px] h-[18px] text-[#FFCD1F]" />
                          ) : (
                            <Moon className="w-[18px] h-[18px] text-[#586380]" />
                          )}
                          <span>Chế độ tối</span>
                        </span>
                        <span
                          className={`inline-flex items-center w-9 h-5 rounded-full p-0.5 transition-colors ${
                            isDark ? 'bg-[#4255FF]' : 'bg-[#D9DDE8]'
                          }`}
                        >
                          <span
                            className={`w-4 h-4 rounded-full bg-white shadow transition-transform ${
                              isDark ? 'translate-x-4' : 'translate-x-0'
                            }`}
                          />
                        </span>
                      </button>
                    </div>

                    {(role === 'TEACHER' || role === 'ADMIN') && (
                      <div className="py-1.5 border-b border-[#EDEFF4] dark:border-[#2E3856]">
                        <Link
                          to="/teacher"
                          role="menuitem"
                          className="flex items-center gap-3 px-4 py-2.5 text-[14px] font-semibold text-[#282E3E] dark:text-[#F6F7FB] hover:bg-[#F6F7FB] dark:hover:bg-[#202140] transition-colors"
                        >
                          <GraduationCap className="w-[18px] h-[18px] text-[#586380] dark:text-[#A8B1D1]" />
                          <span>Không gian Giáo viên</span>
                        </Link>
                        {role === 'ADMIN' && (
                          <Link
                            to="/admin"
                            role="menuitem"
                            className="flex items-center gap-3 px-4 py-2.5 text-[14px] font-semibold text-[#282E3E] dark:text-[#F6F7FB] hover:bg-[#F6F7FB] dark:hover:bg-[#202140] transition-colors"
                          >
                            <ShieldCheck className="w-[18px] h-[18px] text-[#586380] dark:text-[#A8B1D1]" />
                            <span>Cổng Quản trị Admin</span>
                          </Link>
                        )}
                      </div>
                    )}

                    <div className="py-1.5 border-b border-[#EDEFF4] dark:border-[#2E3856]">
                      <button
                        type="button"
                        onClick={handleLogout}
                        role="menuitem"
                        className="w-full flex items-center gap-3 px-4 py-2.5 text-[14px] font-semibold text-[#282E3E] dark:text-[#F6F7FB] hover:bg-[#F6F7FB] dark:hover:bg-[#202140] transition-colors"
                      >
                        <LogOut className="w-[18px] h-[18px] text-[#586380] dark:text-[#A8B1D1]" />
                        <span>Đăng xuất</span>
                      </button>
                    </div>

                    <div className="py-1.5">
                      <Link
                        to={profilePath}
                        role="menuitem"
                        className="flex items-center gap-3 px-4 py-2 text-[13px] font-semibold text-[#586380] dark:text-[#A8B1D1] hover:bg-[#F6F7FB] dark:hover:bg-[#202140] hover:text-[#282E3E] dark:hover:text-white transition-colors"
                      >
                        <UserIcon className="w-4 h-4" />
                        <span>Hồ sơ cá nhân</span>
                      </Link>
                      <Link
                        to={`${profilePath}?tab=settings`}
                        role="menuitem"
                        className="flex items-center gap-3 px-4 py-2 text-[13px] font-semibold text-[#586380] dark:text-[#A8B1D1] hover:bg-[#F6F7FB] dark:hover:bg-[#202140] hover:text-[#282E3E] dark:hover:text-white transition-colors"
                      >
                        <Shield className="w-4 h-4" />
                        <span>Quyền riêng tư</span>
                      </Link>
                    </div>
                  </div>
                )}
              </div>
            </>
          ) : (
            <div className="flex items-center gap-2">
              <Link
                to="/login"
                className="text-sm font-bold text-[#282E3E] dark:text-white hover:bg-[#F6F7FB] dark:hover:bg-[#202140] px-4 py-2 rounded-full transition-colors"
              >
                Đăng nhập
              </Link>
              <Link
                to="/register"
                className="text-sm font-bold bg-[#4255FF] hover:bg-[#3545E0] text-white px-4 py-2 rounded-full transition-colors"
              >
                Đăng ký
              </Link>
            </div>
          )}
        </div>
      </header>

      {/* Body: Expandable Fixed-Icon Left Rail (No visible scrollbar) + Main Content */}
      <div className="flex flex-1 min-h-0">
        {/* Desktop Sidebar */}
        {!hideSidebar && (
          <aside
            className={`hidden lg:flex flex-col bg-white dark:bg-[#0A082D] sticky top-16 h-[calc(100vh-4rem)] shrink-0 overflow-x-hidden overflow-y-auto [scrollbar-width:none] [&::-webkit-scrollbar]:hidden px-3 py-3 transition-[width] duration-200 ease-out ${
              sidebarCollapsed ? 'w-[64px]' : 'w-[244px]'
            }`}
          >
          {/* Primary Nav */}
          <nav className="space-y-1">
            {primaryNavItems.map((item) => renderFixedRailItem(item, sidebarCollapsed))}

            {isAuthenticated && (
              <div className="relative">
                <button
                  type="button"
                  onClick={() => setNotifOpen((prev) => !prev)}
                  title={sidebarCollapsed ? 'Thông báo' : undefined}
                  className="w-full flex items-center h-9 rounded-lg text-[14px] font-semibold text-[#586380] hover:bg-[#F6F7FB] hover:text-[#282E3E] dark:text-[#A8B1D1] dark:hover:bg-[#202140] dark:hover:text-white transition-colors"
                >
                  <span className="w-10 h-9 shrink-0 flex items-center justify-center">
                    <Bell className="w-[18px] h-[18px]" />
                  </span>
                  <span
                    className={`whitespace-nowrap overflow-hidden transition-all duration-200 ${
                      sidebarCollapsed ? 'w-0 opacity-0' : 'pr-3 opacity-100'
                    }`}
                  >
                    Thông báo
                  </span>
                </button>

                {notifOpen && (
                  <div className="mt-1 p-3 rounded-xl bg-[#F6F7FB] dark:bg-[#202140] border border-[#EDEFF4] dark:border-[#2E3856] text-xs text-[#586380] dark:text-[#A8B1D1]">
                    {sidebarCollapsed
                      ? 'Chưa có thông báo mới.'
                      : 'Chào mừng bạn trở lại! Hiện chưa có thông báo mới.'}
                  </div>
                )}
              </div>
            )}
          </nav>

          {/* Divider */}
          <div className="my-3.5 border-t border-[#EDEFF4] dark:border-[#2E3856]" />

          {/* "Bắt đầu tại đây" — expands downward & pushes items below */}
          <div>
            <div
              className={`overflow-hidden transition-all duration-200 ease-out ${
                sidebarCollapsed ? 'max-h-0 opacity-0 mb-0' : 'max-h-8 opacity-100 mb-2'
              }`}
            >
              <p className="px-2.5 text-[13px] font-bold text-[#282E3E] dark:text-[#F6F7FB] whitespace-nowrap">
                Bắt đầu tại đây
              </p>
            </div>

            <nav className="space-y-1">
              {startHereItems.map((item) => renderFixedRailItem(item, sidebarCollapsed))}
            </nav>
          </div>

          {/* Divider */}
          <div className="my-3.5 border-t border-[#EDEFF4] dark:border-[#2E3856]" />

          {/* "Thư mục của bạn" */}
          <div>
            <div
              className={`overflow-hidden transition-all duration-200 ease-out ${
                sidebarCollapsed ? 'max-h-0 opacity-0 mb-0' : 'max-h-8 opacity-100 mb-2'
              }`}
            >
              <p className="px-2.5 text-[13px] font-bold text-[#282E3E] dark:text-[#F6F7FB] whitespace-nowrap">
                Thư mục của bạn
              </p>
            </div>

            <Link
              to="/library"
              title={sidebarCollapsed ? 'Thư mục mới' : undefined}
              className="flex items-center h-9 rounded-lg text-[14px] font-semibold text-[#586380] hover:bg-[#F6F7FB] hover:text-[#282E3E] dark:text-[#A8B1D1] dark:hover:bg-[#202140] dark:hover:text-white transition-colors"
            >
              <span className="w-10 h-9 shrink-0 flex items-center justify-center">
                <Plus className="w-[18px] h-[18px]" />
              </span>
              <span
                className={`whitespace-nowrap overflow-hidden transition-all duration-200 ${
                  sidebarCollapsed ? 'w-0 opacity-0' : 'pr-3 opacity-100'
                }`}
              >
                Thư mục mới
              </span>
            </Link>
          </div>

          {/* Teacher / Admin shortcuts if applicable */}
          {workspaceItems.length > 0 && (
            <>
              <div className="my-3.5 border-t border-[#EDEFF4] dark:border-[#2E3856]" />
              <div>
                <div
                  className={`overflow-hidden transition-all duration-200 ease-out ${
                    sidebarCollapsed ? 'max-h-0 opacity-0 mb-0' : 'max-h-8 opacity-100 mb-2'
                  }`}
                >
                  <p className="px-2.5 text-[13px] font-bold text-[#282E3E] dark:text-[#F6F7FB] whitespace-nowrap">
                    Quản lý
                  </p>
                </div>
                <nav className="space-y-1">
                  {workspaceItems.map((item) => renderFixedRailItem(item, sidebarCollapsed))}
                </nav>
              </div>
            </>
          )}
          </aside>
        )}

        {/* Slide-over Drawer Overlay (Mobile, or Desktop when hideSidebar=true) */}
        <div
          aria-hidden={!mobileDrawerOpen}
          className={`fixed inset-0 z-50 flex transition-opacity duration-300 ${
            hideSidebar ? '' : 'lg:hidden'
          } ${mobileDrawerOpen ? 'pointer-events-auto opacity-100' : 'pointer-events-none opacity-0'}`}
        >
          <div
            className="fixed inset-0 bg-black/45 transition-opacity duration-300"
            onClick={() => setMobileDrawerOpen(false)}
          />
          <aside
            className={`relative w-64 max-w-[82vw] bg-white dark:bg-[#0A082D] h-full shadow-2xl flex flex-col justify-between p-3 z-10 overflow-y-auto [scrollbar-width:none] [&::-webkit-scrollbar]:hidden transform transition-transform duration-300 ease-[cubic-bezier(0.22,1,0.36,1)] ${
              mobileDrawerOpen ? 'translate-x-0' : '-translate-x-full'
            }`}
          >
              <div>
                <div className="flex items-center justify-between px-2 pb-3 mb-2 border-b border-[#EDEFF4] dark:border-[#2E3856]">
                  <Link to="/" className="flex items-center gap-2">
                    <img
                      src={smartflashLogo}
                      alt="ELearner"
                      className="w-7 h-7 rounded-lg object-contain"
                    />
                    <span className="text-[19px] leading-none font-semibold tracking-[-0.01em] text-[#282E3E] dark:text-white">
                      ELearner
                    </span>
                  </Link>
                  <button
                    type="button"
                    onClick={() => setMobileDrawerOpen(false)}
                    className="p-2 rounded-full text-[#586380] dark:text-[#A8B1D1] hover:bg-[#F6F7FB] dark:hover:bg-[#202140]"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>

                <nav className="space-y-1">
                  {primaryNavItems.map((item) => renderFixedRailItem(item, false))}
                </nav>

                <div className="my-3.5 border-t border-[#EDEFF4] dark:border-[#2E3856]" />
                <p className="px-2.5 mb-2 text-[13px] font-bold text-[#282E3E] dark:text-[#F6F7FB]">
                  Bắt đầu tại đây
                </p>
                <nav className="space-y-1">
                  {startHereItems.map((item) => renderFixedRailItem(item, false))}
                </nav>

                <div className="my-3.5 border-t border-[#EDEFF4] dark:border-[#2E3856]" />
                <p className="px-2.5 mb-2 text-[13px] font-bold text-[#282E3E] dark:text-[#F6F7FB]">
                  Thư mục của bạn
                </p>
                <Link
                  to="/library"
                  className="flex items-center h-9 rounded-lg text-[14px] font-semibold text-[#586380] dark:text-[#A8B1D1] hover:bg-[#F6F7FB] dark:hover:bg-[#202140]"
                >
                  <span className="w-10 h-9 shrink-0 flex items-center justify-center">
                    <Plus className="w-[18px] h-[18px]" />
                  </span>
                  <span>Thư mục mới</span>
                </Link>

                {workspaceItems.length > 0 && (
                  <>
                    <div className="my-3.5 border-t border-[#EDEFF4] dark:border-[#2E3856]" />
                    <nav className="space-y-1">
                      {workspaceItems.map((item) => renderFixedRailItem(item, false))}
                    </nav>
                  </>
                )}
              </div>

              <div className="pt-3 border-t border-[#EDEFF4] dark:border-[#2E3856]">
                {isAuthenticated ? (
                  <button
                    type="button"
                    onClick={handleLogout}
                    className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-semibold text-quizlet-red hover:bg-red-50 dark:hover:bg-red-950/30"
                  >
                    <LogOut className="w-4 h-4" />
                    <span>Đăng xuất</span>
                  </button>
                ) : (
                  <div className="grid grid-cols-2 gap-2">
                    <Link
                      to="/login"
                      className="text-center py-2.5 rounded-full border border-[#D9DDE8] dark:border-[#2E3856] text-sm font-bold text-[#282E3E] dark:text-white"
                    >
                      Đăng nhập
                    </Link>
                    <Link
                      to="/register"
                      className="text-center py-2.5 rounded-full bg-[#4255FF] text-sm font-bold text-white"
                    >
                      Đăng ký
                    </Link>
                  </div>
                )}
              </div>
          </aside>
        </div>

        {/* Main Workspace */}
        <main className="flex-1 min-w-0">{children}</main>
      </div>
    </div>
  );
}

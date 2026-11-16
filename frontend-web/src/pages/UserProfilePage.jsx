import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import {
  BadgeCheck,
  Calendar,
  Flame,
  Trophy,
  Zap,
  Layers,
  FolderOpen,
  GraduationCap,
  Award,
  Settings,
  Shield,
  Lock,
  EyeOff,
  Star,
  Eye,
  Users,
  Camera,
  Loader2,
  ArrowLeft,
  LogIn,
  UserX,
  Edit3,
} from 'lucide-react';
import MainLayout from '../components/layout/MainLayout';
import UserAvatar from '../components/common/UserAvatar';
import ProfilePrivacyModal from '../components/profile/ProfilePrivacyModal';
import ProfileSettingsPanel from '../components/profile/ProfileSettingsPanel';
import userApi from '../api/userApi';
import { useAuthStore } from '../store/useAuthStore';
import {
  extractApiErrorMessage,
  formatJoinedDate,
  formatShortDate,
  getRoleMeta,
  VISIBILITY_META,
} from '../utils/userHelpers';

const TAB_DEFINITIONS = [
  { id: 'sets', label: 'Bộ thẻ', configKey: 'showSets', icon: Layers },
  { id: 'folders', label: 'Thư mục', configKey: 'showFolders', icon: FolderOpen },
  { id: 'classes', label: 'Lớp học', configKey: 'showClasses', icon: GraduationCap },
  { id: 'achievements', label: 'Thành tựu', configKey: 'showAchievements', icon: Award },
];

export default function UserProfilePage() {
  const { username } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated, user: authUser, setUser } = useAuthStore();

  const headerFileInputRef = useRef(null);

  const [profile, setProfile] = useState(null);
  const [myProfile, setMyProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [errorState, setErrorState] = useState(null);
  const [privacyModalOpen, setPrivacyModalOpen] = useState(false);
  const [uploadingHeaderAvatar, setUploadingHeaderAvatar] = useState(false);
  const [headerBannerMsg, setHeaderBannerMsg] = useState('');

  const loadProfileData = useCallback(async () => {
    if (!username) return;
    setLoading(true);
    setErrorState(null);

    try {
      const publicRes = await userApi.getPublicProfile(username);
      const publicData = publicRes?.data;
      setProfile(publicData);

      if (publicData?.owner && isAuthenticated) {
        try {
          const meRes = await userApi.getMyProfile();
          if (meRes?.data) {
            setMyProfile(meRes.data);
            setUser(meRes.data);
          }
        } catch {
          // Keep publicData even if /me fails unexpectedly
        }
      } else {
        setMyProfile(null);
      }
    } catch (err) {
      setProfile(null);
      setMyProfile(null);
      setErrorState({
        code: err?.errorCode || 'UNKNOWN',
        message: extractApiErrorMessage(err, 'Không thể tải trang cá nhân.'),
      });
    } finally {
      setLoading(false);
    }
  }, [username, isAuthenticated, setUser]);

  useEffect(() => {
    loadProfileData();
  }, [loadProfileData]);

  const availableTabs = useMemo(() => {
    if (!profile) return [];
    const visibleTabs = profile.visibleTabs || {};
    const list = TAB_DEFINITIONS.filter((tab) => visibleTabs[tab.configKey]);
    if (profile.owner) {
      list.push({ id: 'settings', label: 'Cài đặt tài khoản', configKey: null, icon: Settings });
    }
    return list;
  }, [profile]);

  const requestedTab = searchParams.get('tab');
  const activeTab = useMemo(() => {
    if (requestedTab && availableTabs.some((t) => t.id === requestedTab)) {
      return requestedTab;
    }
    return availableTabs[0]?.id || null;
  }, [requestedTab, availableTabs]);

  const handleSelectTab = (tabId) => {
    const next = new URLSearchParams(searchParams);
    next.set('tab', tabId);
    setSearchParams(next, { replace: true });
  };

  const handleProfileUpdated = (updatedMe) => {
    setMyProfile(updatedMe);
    setUser(updatedMe);
    setProfile((prev) =>
      prev
        ? {
            ...prev,
            username: updatedMe.username,
            fullName: updatedMe.fullName,
            bio: updatedMe.bio,
            avatarUrl: updatedMe.avatarUrl,
            profileVisibility: updatedMe.profileVisibility,
          }
        : prev
    );

    if (
      updatedMe.username &&
      username &&
      updatedMe.username.toLowerCase() !== username.toLowerCase()
    ) {
      navigate(`/users/${encodeURIComponent(updatedMe.username)}?tab=settings`, { replace: true });
    }
  };

  const handleAvatarUpdated = (newAvatarUrl) => {
    setProfile((prev) => (prev ? { ...prev, avatarUrl: newAvatarUrl } : prev));
    setMyProfile((prev) => (prev ? { ...prev, avatarUrl: newAvatarUrl } : prev));
    if (authUser) {
      setUser({ ...authUser, avatarUrl: newAvatarUrl });
    }
  };

  const handleHeaderAvatarSelect = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setHeaderBannerMsg('');
    setUploadingHeaderAvatar(true);
    try {
      const res = await userApi.uploadAvatar(file);
      if (res?.data?.avatarUrl) {
        handleAvatarUpdated(res.data.avatarUrl);
      }
    } catch (err) {
      setHeaderBannerMsg(extractApiErrorMessage(err, 'Không thể tải ảnh đại diện mới.'));
    } finally {
      setUploadingHeaderAvatar(false);
      e.target.value = '';
    }
  };

  const handlePrivacySaved = (updatedMe) => {
    setMyProfile(updatedMe);
    setUser(updatedMe);
    setProfile((prev) =>
      prev
        ? {
            ...prev,
            profileVisibility: updatedMe.profileVisibility,
          }
        : prev
    );
  };

  const renderLoading = () => (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 animate-pulse space-y-6">
      <div className="bg-white dark:bg-[#202140] rounded-3xl border border-[#EDEFF4] dark:border-[#2E3856] p-6 sm:p-8 flex flex-col sm:flex-row items-center gap-6">
        <div className="w-24 h-24 rounded-full bg-gray-200 dark:bg-[#2E3856] shrink-0" />
        <div className="flex-1 w-full space-y-3">
          <div className="h-7 w-56 bg-gray-200 dark:bg-[#2E3856] rounded-lg" />
          <div className="h-4 w-36 bg-gray-200 dark:bg-[#2E3856] rounded-lg" />
          <div className="h-4 w-2/3 bg-gray-100 dark:bg-[#2E3856]/60 rounded-lg" />
        </div>
      </div>
      <div className="h-12 w-full bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856]" />
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        <div className="h-40 bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856]" />
        <div className="h-40 bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856]" />
        <div className="h-40 bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856]" />
      </div>
    </div>
  );

  const renderErrorState = () => {
    const isUnauthorized = errorState?.code === 'UNAUTHORIZED';
    const isForbidden = errorState?.code === 'FORBIDDEN';

    return (
      <div className="max-w-xl mx-auto px-4 py-16">
        <div className="bg-white dark:bg-[#202140] rounded-3xl border border-[#EDEFF4] dark:border-[#2E3856] shadow-sm p-8 text-center">
          <div
            className={`w-16 h-16 rounded-2xl mx-auto flex items-center justify-center mb-5 ${
              isUnauthorized
                ? 'bg-blue-50 dark:bg-[#2E3856] text-[#4255FF] dark:text-[#93A3FF]'
                : isForbidden
                ? 'bg-amber-50 dark:bg-amber-950/50 text-amber-600 dark:text-[#FFCD1F]'
                : 'bg-gray-100 dark:bg-[#2E3856] text-[#586380] dark:text-[#A8B1D1]'
            }`}
          >
            {isUnauthorized ? (
              <LogIn className="w-8 h-8" />
            ) : isForbidden ? (
              <Lock className="w-8 h-8" />
            ) : (
              <UserX className="w-8 h-8" />
            )}
          </div>

          <h1 className="text-2xl font-extrabold text-[#282E3E] dark:text-white">
            {isUnauthorized
              ? 'Yêu cầu đăng nhập'
              : isForbidden
              ? 'Trang cá nhân ở chế độ riêng tư'
              : 'Không tìm thấy trang cá nhân'}
          </h1>
          <p className="mt-2.5 text-sm text-[#586380] dark:text-[#A8B1D1] leading-relaxed">
            {errorState?.message}
          </p>

          <div className="mt-7 flex flex-wrap items-center justify-center gap-3">
            {isUnauthorized && (
              <Link
                to="/login"
                state={{ from: location }}
                className="inline-flex items-center gap-2 px-5 py-2.5 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white text-sm font-bold shadow-sm transition"
              >
                <LogIn className="w-4 h-4" />
                <span>Đăng nhập ngay</span>
              </Link>
            )}
            <Link
              to="/"
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-full border border-[#D9DDE8] dark:border-[#3A4369] hover:bg-[#F6F7FB] dark:hover:bg-[#2E3856] text-sm font-bold text-[#282E3E] dark:text-white transition"
            >
              <ArrowLeft className="w-4 h-4" />
              <span>Quay về Trang chủ</span>
            </Link>
          </div>
        </div>
      </div>
    );
  };

  const renderSetsTab = () => {
    const sets = profile?.sets || [];
    if (sets.length === 0) {
      return (
        <EmptyTabState
          icon={Layers}
          title="Chưa có bộ thẻ nào"
          description={
            profile?.owner
              ? 'Bạn chưa tạo bộ thẻ ghi nhớ nào. Hãy bắt đầu tạo bộ thẻ ở Phase 3 hoặc từ Thư viện.'
              : 'Người dùng này chưa có bộ thẻ công khai nào.'
          }
          actionLabel={profile?.owner ? 'Đi tới Thư viện' : null}
          actionTo={profile?.owner ? '/library' : null}
        />
      );
    }

    return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
        {sets.map((item) => (
          <div
            key={item.id}
            className="bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] p-5 hover:border-[#4255FF]/50 hover:shadow-md transition flex flex-col justify-between"
          >
            <div>
              <div className="flex items-center justify-between gap-2 mb-3">
                <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold bg-[#EDEFFF] dark:bg-[#2E3856] text-[#4255FF] dark:text-[#93A3FF]">
                  {item.cardCount ?? 0} thuật ngữ
                </span>
                {item.visibility && (
                  <span className="text-[11px] font-bold uppercase px-2 py-0.5 rounded-full bg-[#F6F7FB] dark:bg-[#0A082D] text-[#586380] dark:text-[#A8B1D1]">
                    {item.visibility}
                  </span>
                )}
              </div>
              <h3 className="text-base font-extrabold text-[#282E3E] dark:text-white line-clamp-2">
                {item.title}
              </h3>
              {item.description && (
                <p className="mt-1.5 text-xs text-[#586380] dark:text-[#A8B1D1] line-clamp-2">
                  {item.description}
                </p>
              )}
            </div>

            <div className="mt-4 pt-3 border-t border-[#EDEFF4] dark:border-[#2E3856] flex items-center justify-between text-xs text-[#586380] dark:text-[#A8B1D1]">
              <span className="inline-flex items-center gap-1">
                <Eye className="w-3.5 h-3.5" />
                {item.viewCount ?? 0}
              </span>
              <span className="inline-flex items-center gap-1 font-bold text-amber-600 dark:text-[#FFCD1F]">
                <Star className="w-3.5 h-3.5 fill-quizlet-yellow text-quizlet-yellow" />
                {(item.avgRating ?? 0).toFixed(1)}
              </span>
            </div>
          </div>
        ))}
      </div>
    );
  };

  const renderFoldersTab = () => {
    const folders = profile?.folders || [];
    if (folders.length === 0) {
      return (
        <EmptyTabState
          icon={FolderOpen}
          title="Chưa có thư mục nào"
          description={
            profile?.owner
              ? 'Sắp xếp các bộ thẻ theo môn học hoặc chủ đề bằng thư mục.'
              : 'Người dùng này chưa chia sẻ thư mục công khai nào.'
          }
        />
      );
    }

    return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
        {folders.map((folder) => (
          <div
            key={folder.id}
            className="bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] p-5 hover:border-[#4255FF]/50 hover:shadow-md transition"
          >
            <div className="flex items-center justify-between gap-2 mb-3">
              <div className="w-10 h-10 rounded-xl bg-amber-50 dark:bg-[#2E3856] text-amber-600 dark:text-[#FFCD1F] flex items-center justify-center">
                <FolderOpen className="w-5 h-5" />
              </div>
              <span className="text-xs font-bold text-[#586380] dark:text-[#A8B1D1] bg-[#F6F7FB] dark:bg-[#0A082D] px-2.5 py-1 rounded-full">
                {folder.setCount ?? 0} bộ thẻ
              </span>
            </div>
            <h3 className="text-base font-extrabold text-[#282E3E] dark:text-white truncate">
              {folder.name}
            </h3>
            {folder.description && (
              <p className="mt-1.5 text-xs text-[#586380] dark:text-[#A8B1D1] line-clamp-2">
                {folder.description}
              </p>
            )}
          </div>
        ))}
      </div>
    );
  };

  const renderClassesTab = () => {
    const classes = profile?.classes || [];
    if (classes.length === 0) {
      return (
        <EmptyTabState
          icon={GraduationCap}
          title="Chưa tham gia lớp học nào"
          description={
            profile?.owner
              ? 'Tham gia lớp học của giáo viên hoặc tạo lớp học mới để giao bài.'
              : 'Chưa có lớp học nào được hiển thị.'
          }
        />
      );
    }

    return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
        {classes.map((cls) => (
          <div
            key={cls.id}
            className="bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] p-5 hover:border-[#4255FF]/50 hover:shadow-md transition"
          >
            <div className="flex items-center justify-between gap-2 mb-3">
              <div className="w-10 h-10 rounded-xl bg-purple-50 dark:bg-[#2E3856] text-quizlet-purple dark:text-[#C084FC] flex items-center justify-center">
                <GraduationCap className="w-5 h-5" />
              </div>
              <span
                className={`text-[11px] font-bold uppercase px-2.5 py-0.5 rounded-full ${
                  cls.memberRole === 'TEACHER'
                    ? 'bg-purple-50 dark:bg-purple-950/50 text-quizlet-purple dark:text-[#C084FC]'
                    : 'bg-blue-50 dark:bg-[#2E3856] text-[#4255FF] dark:text-[#93A3FF]'
                }`}
              >
                {cls.memberRole === 'TEACHER' ? 'Giáo viên' : 'Học viên'}
              </span>
            </div>
            <h3 className="text-base font-extrabold text-[#282E3E] dark:text-white truncate">{cls.name}</h3>
            {cls.schoolName && (
              <p className="mt-1 text-xs text-[#586380] dark:text-[#A8B1D1] truncate">{cls.schoolName}</p>
            )}
            <div className="mt-4 pt-3 border-t border-[#EDEFF4] dark:border-[#2E3856] flex items-center gap-1.5 text-xs font-semibold text-[#586380] dark:text-[#A8B1D1]">
              <Users className="w-3.5 h-3.5" />
              <span>{cls.memberCount ?? 0} thành viên</span>
            </div>
          </div>
        ))}
      </div>
    );
  };

  const renderAchievementsTab = () => {
    const achievements = profile?.achievements || [];
    if (achievements.length === 0) {
      return (
        <EmptyTabState
          icon={Award}
          title="Chưa mở khóa thành tựu nào"
          description="Hoàn thành chuỗi ngày học, kiểm tra và minigames để mở khóa các huy hiệu đầu tiên."
        />
      );
    }

    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
        {achievements.map((ach) => (
          <div
            key={ach.id}
            className="bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] p-5 flex items-start gap-4 hover:shadow-md transition"
          >
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-quizlet-yellow to-amber-400 text-[#1A1D28] flex items-center justify-center shrink-0 shadow-sm">
              {ach.iconUrl ? (
                <img src={ach.iconUrl} alt={ach.name} className="w-7 h-7 object-contain" />
              ) : (
                <Award className="w-6 h-6" />
              )}
            </div>
            <div className="min-w-0 flex-1">
              <div className="flex items-center justify-between gap-2">
                <h3 className="text-sm font-extrabold text-[#282E3E] dark:text-white truncate">{ach.name}</h3>
                {ach.xpReward > 0 && (
                  <span className="text-[11px] font-extrabold text-emerald-700 dark:text-emerald-300 bg-emerald-50 dark:bg-emerald-950/50 px-2 py-0.5 rounded-full shrink-0">
                    +{ach.xpReward} XP
                  </span>
                )}
              </div>
              {ach.description && (
                <p className="mt-1 text-xs text-[#586380] dark:text-[#A8B1D1] line-clamp-2">
                  {ach.description}
                </p>
              )}
              {ach.unlockedAt && (
                <p className="mt-2 text-[11px] text-[#586380] dark:text-[#A8B1D1]">
                  Mở khóa ngày {formatShortDate(ach.unlockedAt)}
                </p>
              )}
            </div>
          </div>
        ))}
      </div>
    );
  };

  const roleMeta = getRoleMeta(profile?.role);
  const visibilityMeta =
    VISIBILITY_META[profile?.profileVisibility || 'PUBLIC'] || VISIBILITY_META.PUBLIC;
  const ownerTabPrivacy = myProfile?.profilePrivacyConfig || {
    showSets: true,
    showFolders: true,
    showClasses: true,
    showAchievements: true,
  };

  return (
    <MainLayout>
      {loading && renderLoading()}
      {!loading && errorState && renderErrorState()}
      {!loading && !errorState && profile && (
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
          {/* Profile Header Card */}
          <section className="bg-white dark:bg-[#202140] rounded-3xl border border-[#EDEFF4] dark:border-[#2E3856] shadow-sm overflow-hidden">
            <div className="h-24 sm:h-28 bg-gradient-to-r from-[#4255FF] via-[#3B4CE8] to-[#2E3856] relative">
              <div className="absolute -right-10 -top-10 w-44 h-44 rounded-full bg-white/10 blur-xl pointer-events-none" />
              <div className="absolute left-1/3 bottom-0 w-36 h-36 rounded-full bg-quizlet-yellow/15 blur-2xl pointer-events-none" />
            </div>

            <div className="px-6 sm:px-8 pb-6 sm:pb-8">
              <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 -mt-12 sm:-mt-14">
                <div className="flex flex-col sm:flex-row sm:items-end gap-4 sm:gap-5">
                  <div className="relative w-fit">
                    <UserAvatar
                      src={profile.avatarUrl}
                      fullName={profile.fullName}
                      username={profile.username}
                      size="xl"
                      className="ring-4 ring-white dark:ring-[#202140] shadow-md"
                    />
                    {profile.owner && (
                      <>
                        <input
                          ref={headerFileInputRef}
                          type="file"
                          accept="image/jpeg,image/png,image/webp,image/gif"
                          onChange={handleHeaderAvatarSelect}
                          className="hidden"
                        />
                        <button
                          type="button"
                          onClick={() => headerFileInputRef.current?.click()}
                          disabled={uploadingHeaderAvatar}
                          title="Đổi ảnh đại diện"
                          className="absolute bottom-1 right-1 w-8 h-8 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white flex items-center justify-center border-2 border-white dark:border-[#202140] shadow-md transition"
                        >
                          {uploadingHeaderAvatar ? (
                            <Loader2 className="w-4 h-4 animate-spin" />
                          ) : (
                            <Camera className="w-4 h-4" />
                          )}
                        </button>
                      </>
                    )}
                  </div>

                  <div className="pt-1 sm:pb-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <h1 className="text-2xl sm:text-3xl font-black text-[#282E3E] dark:text-white tracking-tight">
                        {profile.fullName || profile.username}
                      </h1>
                      {profile.verifiedTeacher && (
                        <span
                          title="Giáo viên đã xác minh"
                          className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-blue-50 dark:bg-[#2E3856] text-[#4255FF] dark:text-[#93A3FF] border border-blue-200 dark:border-[#3D4870] text-xs font-bold"
                        >
                          <BadgeCheck className="w-3.5 h-3.5" />
                          Verified
                        </span>
                      )}
                      <span
                        className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold border ${roleMeta.badgeClass}`}
                      >
                        {roleMeta.label}
                      </span>
                    </div>

                    <div className="mt-1 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-[#586380] dark:text-[#A8B1D1]">
                      <span className="font-semibold">@{profile.username}</span>
                      {profile.joinedAt && (
                        <span className="inline-flex items-center gap-1.5 text-xs">
                          <Calendar className="w-3.5 h-3.5" />
                          Tham gia {formatJoinedDate(profile.joinedAt)}
                        </span>
                      )}
                      {profile.owner && (
                        <span className="inline-flex items-center gap-1 text-xs font-semibold text-[#4255FF] dark:text-[#93A3FF]">
                          <Shield className="w-3.5 h-3.5" />
                          {visibilityMeta.label}
                        </span>
                      )}
                    </div>
                  </div>
                </div>

                {profile.owner && (
                  <div className="flex flex-wrap items-center gap-2.5">
                    <button
                      type="button"
                      onClick={() => handleSelectTab('settings')}
                      className="inline-flex items-center gap-2 px-4 py-2.5 rounded-full border border-[#D9DDE8] dark:border-[#3A4369] bg-white dark:bg-[#0A082D] hover:bg-[#F6F7FB] dark:hover:bg-[#2E3856] text-xs sm:text-sm font-bold text-[#282E3E] dark:text-white transition"
                    >
                      <Edit3 className="w-4 h-4" />
                      <span>Chỉnh sửa hồ sơ</span>
                    </button>
                    <button
                      type="button"
                      onClick={() => setPrivacyModalOpen(true)}
                      className="inline-flex items-center gap-2 px-4 py-2.5 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white text-xs sm:text-sm font-bold shadow-sm transition"
                    >
                      <Shield className="w-4 h-4" />
                      <span>Quyền riêng tư</span>
                    </button>
                  </div>
                )}
              </div>

              {headerBannerMsg && (
                <p className="mt-3 text-xs font-semibold text-quizlet-red">{headerBannerMsg}</p>
              )}

              {profile.bio && (
                <p className="mt-5 text-sm text-[#282E3E] dark:text-[#F6F7FB] leading-relaxed max-w-3xl">
                  {profile.bio}
                </p>
              )}

              {/* Stats Strip */}
              <div className="mt-6 grid grid-cols-3 gap-3 sm:gap-4 max-w-2xl">
                <div className="p-3.5 rounded-2xl bg-[#F6F7FB] dark:bg-[#0A082D] border border-[#EDEFF4] dark:border-[#2E3856] flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-orange-100 dark:bg-[#2E3856] text-orange-600 dark:text-orange-400 flex items-center justify-center shrink-0">
                    <Flame className="w-5 h-5" />
                  </div>
                  <div>
                    <p className="text-[11px] font-bold uppercase text-[#586380] dark:text-[#A8B1D1]">
                      Chuỗi hiện tại
                    </p>
                    <p className="text-lg font-black text-[#282E3E] dark:text-white">
                      {profile.currentStreak ?? 0} ngày
                    </p>
                  </div>
                </div>

                <div className="p-3.5 rounded-2xl bg-[#F6F7FB] dark:bg-[#0A082D] border border-[#EDEFF4] dark:border-[#2E3856] flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-amber-100 dark:bg-[#2E3856] text-amber-600 dark:text-[#FFCD1F] flex items-center justify-center shrink-0">
                    <Trophy className="w-5 h-5" />
                  </div>
                  <div>
                    <p className="text-[11px] font-bold uppercase text-[#586380] dark:text-[#A8B1D1]">
                      Kỷ lục chuỗi
                    </p>
                    <p className="text-lg font-black text-[#282E3E] dark:text-white">
                      {profile.longestStreak ?? 0} ngày
                    </p>
                  </div>
                </div>

                <div className="p-3.5 rounded-2xl bg-[#F6F7FB] dark:bg-[#0A082D] border border-[#EDEFF4] dark:border-[#2E3856] flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-blue-100 dark:bg-[#2E3856] text-[#4255FF] dark:text-[#93A3FF] flex items-center justify-center shrink-0">
                    <Zap className="w-5 h-5" />
                  </div>
                  <div>
                    <p className="text-[11px] font-bold uppercase text-[#586380] dark:text-[#A8B1D1]">
                      Tổng điểm XP
                    </p>
                    <p className="text-lg font-black text-[#282E3E] dark:text-white">
                      {(profile.totalXp ?? 0).toLocaleString('vi-VN')}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </section>

          {/* Navigation Tabs */}
          {availableTabs.length > 0 ? (
            <>
              <div className="bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] px-3 sm:px-5 flex items-center gap-1 overflow-x-auto">
                {availableTabs.map((tab) => {
                  const Icon = tab.icon;
                  const isActive = activeTab === tab.id;
                  const isHiddenFromOthers =
                    profile.owner && tab.configKey && ownerTabPrivacy[tab.configKey] === false;

                  return (
                    <button
                      key={tab.id}
                      type="button"
                      onClick={() => handleSelectTab(tab.id)}
                      className={`inline-flex items-center gap-2 px-4 py-3.5 text-sm font-bold border-b-2 whitespace-nowrap transition ${
                        isActive
                          ? 'border-[#4255FF] text-[#4255FF] dark:border-white dark:text-white'
                          : 'border-transparent text-[#586380] dark:text-[#A8B1D1] hover:text-[#282E3E] dark:hover:text-white'
                      }`}
                    >
                      <Icon className="w-4 h-4" />
                      <span>{tab.label}</span>
                      {isHiddenFromOthers && (
                        <EyeOff
                          className="w-3.5 h-3.5 text-amber-500"
                          title="Tab này đang ẩn với người khác"
                        />
                      )}
                    </button>
                  );
                })}
              </div>

              <div>
                {activeTab === 'sets' && renderSetsTab()}
                {activeTab === 'folders' && renderFoldersTab()}
                {activeTab === 'classes' && renderClassesTab()}
                {activeTab === 'achievements' && renderAchievementsTab()}
                {activeTab === 'settings' && profile.owner && (
                  <ProfileSettingsPanel
                    myProfile={myProfile || profile}
                    onProfileUpdated={handleProfileUpdated}
                    onAvatarUpdated={handleAvatarUpdated}
                    onOpenPrivacyModal={() => setPrivacyModalOpen(true)}
                  />
                )}
              </div>
            </>
          ) : (
            <div className="bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] p-10 text-center">
              <div className="w-12 h-12 rounded-2xl bg-[#F6F7FB] dark:bg-[#2E3856] text-[#586380] dark:text-[#A8B1D1] flex items-center justify-center mx-auto mb-3">
                <EyeOff className="w-6 h-6" />
              </div>
              <h2 className="text-lg font-extrabold text-[#282E3E] dark:text-white">
                Chưa có mục nào được hiển thị
              </h2>
              <p className="mt-1.5 text-sm text-[#586380] dark:text-[#A8B1D1]">
                Người dùng này đã ẩn toàn bộ các tab nội dung trên trang cá nhân.
              </p>
            </div>
          )}
        </div>
      )}

      {profile?.owner && (
        <ProfilePrivacyModal
          isOpen={privacyModalOpen}
          onClose={() => setPrivacyModalOpen(false)}
          initialVisibility={myProfile?.profileVisibility || profile.profileVisibility}
          initialConfig={ownerTabPrivacy}
          onSaved={handlePrivacySaved}
        />
      )}
    </MainLayout>
  );
}

function EmptyTabState({ icon: Icon, title, description, actionLabel, actionTo }) {
  return (
    <div className="bg-white dark:bg-[#202140] rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] p-10 text-center">
      <div className="w-12 h-12 rounded-2xl bg-blue-50 dark:bg-[#2E3856] text-[#4255FF] dark:text-[#93A3FF] flex items-center justify-center mx-auto mb-3">
        <Icon className="w-6 h-6" />
      </div>
      <h3 className="text-base font-extrabold text-[#282E3E] dark:text-white">{title}</h3>
      <p className="mt-1.5 text-sm text-[#586380] dark:text-[#A8B1D1] max-w-md mx-auto">{description}</p>
      {actionLabel && actionTo && (
        <Link
          to={actionTo}
          className="mt-5 inline-flex items-center gap-2 px-5 py-2.5 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white text-sm font-bold shadow-sm transition"
        >
          {actionLabel}
        </Link>
      )}
    </div>
  );
}

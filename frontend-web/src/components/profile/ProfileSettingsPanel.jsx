import React, { useEffect, useRef, useState } from 'react';
import {
  CheckCircle2,
  AlertCircle,
  Loader2,
  Eye,
  EyeOff,
  Plus,
  ChevronDown,
  ChevronUp,
  Mail,
  Bell,
  Shield,
} from 'lucide-react';
import userApi from '../../api/userApi';
import UserAvatar from '../common/UserAvatar';
import { useThemeStore } from '../../store/useThemeStore';
import { extractApiErrorMessage, getRoleMeta, VISIBILITY_META } from '../../utils/userHelpers';

const COMMON_TIMEZONES = [
  { value: 'Asia/Ho_Chi_Minh', label: '(GMT+07:00) Hồ Chí Minh' },
  { value: 'Asia/Bangkok', label: '(GMT+07:00) Bangkok' },
  { value: 'Asia/Singapore', label: '(GMT+08:00) Singapore' },
  { value: 'Asia/Tokyo', label: '(GMT+09:00) Tokyo' },
  { value: 'Asia/Seoul', label: '(GMT+09:00) Seoul' },
  { value: 'UTC', label: '(GMT+00:00) UTC' },
  { value: 'Europe/London', label: '(GMT+00:00) London' },
  { value: 'America/New_York', label: '(GMT-05:00) New York' },
  { value: 'America/Los_Angeles', label: '(GMT-08:00) Los Angeles' },
];

const DAILY_GOAL_PRESETS = [10, 20, 30, 50];
const ALLOWED_AVATAR_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp', 'image/gif'];
const MAX_AVATAR_BYTES = 5 * 1024 * 1024;

const PRESET_AVATARS = [
  { id: 'owl', bg: '#4255FF', fg: '#FFFFFF', emoji: '🦉' },
  { id: 'fox', bg: '#FF8A4C', fg: '#FFFFFF', emoji: '🦊' },
  { id: 'panda', bg: '#10B981', fg: '#FFFFFF', emoji: '🐼' },
  { id: 'penguin', bg: '#38BDF8', fg: '#FFFFFF', emoji: '🐧' },
  { id: 'cat', bg: '#A855F7', fg: '#FFFFFF', emoji: '🐱' },
  { id: 'koala', bg: '#6366F1', fg: '#FFFFFF', emoji: '🐨' },
  { id: 'rabbit', bg: '#EC4899', fg: '#FFFFFF', emoji: '🐰' },
  { id: 'bear', bg: '#F59E0B', fg: '#FFFFFF', emoji: '🐻' },
  { id: 'whale', bg: '#0EA5E9', fg: '#FFFFFF', emoji: '🐳' },
  { id: 'duck', bg: '#22C55E', fg: '#FFFFFF', emoji: '🦆' },
  { id: 'bee', bg: '#EAB308', fg: '#FFFFFF', emoji: '🐝' },
  { id: 'unicorn', bg: '#8B5CF6', fg: '#FFFFFF', emoji: '🦄' },
];

function renderPresetAvatarBlob(preset) {
  return new Promise((resolve) => {
    const canvas = document.createElement('canvas');
    canvas.width = 256;
    canvas.height = 256;
    const ctx = canvas.getContext('2d');
    if (!ctx) {
      resolve(null);
      return;
    }
    ctx.fillStyle = preset.bg;
    ctx.fillRect(0, 0, 256, 256);
    ctx.font = '136px "Segoe UI Emoji", "Apple Color Emoji", "Noto Color Emoji", sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(preset.emoji, 128, 136);
    canvas.toBlob((blob) => resolve(blob), 'image/png');
  });
}

export default function ProfileSettingsPanel({
  myProfile,
  onProfileUpdated,
  onAvatarUpdated,
  onOpenPrivacyModal,
}) {
  const fileInputRef = useRef(null);
  const { theme, setTheme } = useThemeStore();

  // Inline editing toggles
  const [editingField, setEditingField] = useState(null); // 'nameBio' | 'username' | 'password' | null
  const [notifOpen, setNotifOpen] = useState(true);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);

  // Profile form values
  const [fullName, setFullName] = useState('');
  const [username, setUsername] = useState('');
  const [bio, setBio] = useState('');
  const [dailyGoalCards, setDailyGoalCards] = useState(20);
  const [timezone, setTimezone] = useState('Asia/Ho_Chi_Minh');

  // Local UI preference toggles (notification visual prefs + language)
  const [language, setLanguage] = useState('vi');
  const [streakNotif, setStreakNotif] = useState(true);
  const [fsrsNotif, setFsrsNotif] = useState(true);

  // Feedback states
  const [savingProfile, setSavingProfile] = useState(false);
  const [uploadingAvatar, setUploadingAvatar] = useState(false);
  const [avatarPreview, setAvatarPreview] = useState('');
  const [bannerMsg, setBannerMsg] = useState({ type: '', text: '' });

  // Password form state
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showCurrentPwd, setShowCurrentPwd] = useState(false);
  const [showNewPwd, setShowNewPwd] = useState(false);
  const [savingPassword, setSavingPassword] = useState(false);

  useEffect(() => {
    if (!myProfile) return;
    setFullName(myProfile.fullName || '');
    setUsername(myProfile.username || '');
    setBio(myProfile.bio || '');
    setDailyGoalCards(myProfile.dailyGoalCards ?? 20);
    setTimezone(myProfile.timezone || 'Asia/Ho_Chi_Minh');
  }, [myProfile]);

  useEffect(() => {
    return () => {
      if (avatarPreview && avatarPreview.startsWith('blob:')) {
        URL.revokeObjectURL(avatarPreview);
      }
    };
  }, [avatarPreview]);

  const uploadAvatarFile = async (file) => {
    setBannerMsg({ type: '', text: '' });
    if (!ALLOWED_AVATAR_TYPES.includes(file.type?.toLowerCase())) {
      setBannerMsg({ type: 'error', text: 'Chỉ hỗ trợ ảnh JPG, PNG, WEBP hoặc GIF.' });
      return;
    }
    if (file.size > MAX_AVATAR_BYTES) {
      setBannerMsg({ type: 'error', text: 'Dung lượng ảnh tối đa là 5MB.' });
      return;
    }

    const previewUrl = URL.createObjectURL(file);
    setAvatarPreview(previewUrl);
    setUploadingAvatar(true);

    try {
      const res = await userApi.uploadAvatar(file);
      const newAvatarUrl = res?.data?.avatarUrl;
      if (newAvatarUrl) {
        onAvatarUpdated?.(newAvatarUrl);
      }
      setAvatarPreview('');
      setBannerMsg({
        type: 'success',
        text: res?.message || 'Cập nhật ảnh hồ sơ thành công!',
      });
    } catch (err) {
      setAvatarPreview('');
      setBannerMsg({
        type: 'error',
        text: extractApiErrorMessage(err, 'Tải ảnh đại diện thất bại.'),
      });
    } finally {
      setUploadingAvatar(false);
    }
  };

  const handleAvatarFileChange = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    await uploadAvatarFile(file);
  };

  const handleSelectPresetAvatar = async (preset) => {
    if (uploadingAvatar) return;
    const blob = await renderPresetAvatarBlob(preset);
    if (!blob) return;
    const file = new File([blob], `avatar-${preset.id}.png`, { type: 'image/png' });
    await uploadAvatarFile(file);
  };

  const persistProfilePatch = async (overrides = {}) => {
    setBannerMsg({ type: '', text: '' });
    const nextFullName = (overrides.fullName ?? fullName).trim();
    const nextUsername = (overrides.username ?? username).trim();
    const nextBio = overrides.bio !== undefined ? overrides.bio : bio;
    const nextGoal = Number(overrides.dailyGoalCards ?? dailyGoalCards);
    const nextTimezone = overrides.timezone ?? timezone;

    if (!nextFullName) {
      setBannerMsg({ type: 'error', text: 'Họ và tên không được để trống.' });
      return false;
    }
    if (!/^[a-zA-Z0-9._-]{3,50}$/.test(nextUsername)) {
      setBannerMsg({
        type: 'error',
        text: 'Tên người dùng phải từ 3–50 ký tự (chữ, số, dấu chấm, gạch dưới hoặc gạch ngang).',
      });
      return false;
    }

    setSavingProfile(true);
    try {
      const res = await userApi.updateMyProfile({
        username: nextUsername,
        fullName: nextFullName,
        bio: nextBio && nextBio.trim() ? nextBio.trim() : null,
        dailyGoalCards: Number.isNaN(nextGoal) ? 20 : nextGoal,
        timezone: nextTimezone,
      });
      if (res?.data) {
        onProfileUpdated?.(res.data);
      }
      setBannerMsg({
        type: 'success',
        text: res?.message || 'Đã lưu thay đổi thông tin cá nhân.',
      });
      return true;
    } catch (err) {
      setBannerMsg({
        type: 'error',
        text: extractApiErrorMessage(err, 'Không thể cập nhật thông tin cá nhân.'),
      });
      return false;
    } finally {
      setSavingProfile(false);
    }
  };

  const handleSaveNameBio = async (e) => {
    e.preventDefault();
    const ok = await persistProfilePatch();
    if (ok) setEditingField(null);
  };

  const handleSaveUsername = async (e) => {
    e.preventDefault();
    const ok = await persistProfilePatch();
    if (ok) setEditingField(null);
  };

  const handleGoalChange = async (e) => {
    const val = Number(e.target.value);
    setDailyGoalCards(val);
    await persistProfilePatch({ dailyGoalCards: val });
  };

  const handleTimezoneChange = async (e) => {
    const val = e.target.value;
    setTimezone(val);
    await persistProfilePatch({ timezone: val });
  };

  const handlePasswordSubmit = async (e) => {
    e.preventDefault();
    setBannerMsg({ type: '', text: '' });

    if (myProfile?.hasPassword && !currentPassword) {
      setBannerMsg({ type: 'error', text: 'Vui lòng nhập mật khẩu hiện tại.' });
      return;
    }
    if (newPassword.length < 6) {
      setBannerMsg({ type: 'error', text: 'Mật khẩu mới phải có ít nhất 6 ký tự.' });
      return;
    }
    if (newPassword !== confirmPassword) {
      setBannerMsg({ type: 'error', text: 'Xác nhận mật khẩu mới không khớp.' });
      return;
    }

    setSavingPassword(true);
    try {
      const res = await userApi.changePassword({
        currentPassword: myProfile?.hasPassword ? currentPassword : null,
        newPassword,
      });
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setEditingField(null);
      setBannerMsg({
        type: 'success',
        text: res?.message || 'Cập nhật mật khẩu thành công!',
      });
    } catch (err) {
      setBannerMsg({
        type: 'error',
        text: extractApiErrorMessage(err, 'Không thể đổi mật khẩu.'),
      });
    } finally {
      setSavingPassword(false);
    }
  };

  const roleMeta = getRoleMeta(myProfile?.role);
  const visibilityInfo =
    VISIBILITY_META[myProfile?.profileVisibility || 'PUBLIC'] || VISIBILITY_META.PUBLIC;
  const privacyConfig = myProfile?.profilePrivacyConfig || {
    showSets: true,
    showFolders: true,
    showClasses: true,
    showAchievements: true,
  };
  const visibleTabCount = [
    privacyConfig.showSets,
    privacyConfig.showFolders,
    privacyConfig.showClasses,
    privacyConfig.showAchievements,
  ].filter(Boolean).length;

  const cardShellClass =
    'rounded-2xl border-2 border-[#D9DDE8] dark:border-[#2E3856] bg-white dark:bg-[#202140] divide-y divide-[#EDEFF4] dark:divide-[#2E3856] overflow-hidden';
  const inputClass =
    'w-full h-11 px-3.5 rounded-xl bg-[#F6F7FB] dark:bg-[#0A082D] border-2 border-transparent focus:border-[#4255FF] focus:bg-white dark:focus:bg-[#0A082D] text-[14px] font-semibold text-[#282E3E] dark:text-white outline-none transition';
  const pillSelectClass =
    'h-10 pl-4 pr-8 rounded-full bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#2E3856] dark:hover:bg-[#3A476B] text-[13.5px] font-bold text-[#282E3E] dark:text-white border border-transparent focus:border-[#4255FF] outline-none cursor-pointer transition-colors';

  return (
    <div className="space-y-8">
      {bannerMsg.text && (
        <div
          className={`flex items-start gap-2.5 px-4 py-3 rounded-xl text-sm font-semibold ${
            bannerMsg.type === 'success'
              ? 'bg-emerald-50 dark:bg-emerald-950/50 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800'
              : 'bg-red-50 dark:bg-[#431F25] text-red-700 dark:text-red-300 border border-red-200 dark:border-red-900'
          }`}
        >
          {bannerMsg.type === 'success' ? (
            <CheckCircle2 className="w-4 h-4 shrink-0 mt-0.5" />
          ) : (
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
          )}
          <span>{bannerMsg.text}</span>
        </div>
      )}

      {/* Section 1: Thông tin cá nhân */}
      <section>
        <h2 className="text-[14px] font-bold text-[#586380] dark:text-[#A8B1D1] mb-3">
          Thông tin cá nhân
        </h2>

        <div className={cardShellClass}>
          {/* Ảnh hồ sơ */}
          <div className="p-6">
            <p className="text-[15px] font-bold text-[#282E3E] dark:text-white mb-4">
              Ảnh hồ sơ
            </p>

            <div className="flex flex-col sm:flex-row sm:items-center gap-6">
              <div className="relative w-fit shrink-0">
                <UserAvatar
                  src={avatarPreview || myProfile?.avatarUrl}
                  fullName={myProfile?.fullName}
                  username={myProfile?.username}
                  size="lg"
                  className="ring-2 ring-[#EDEFF4] dark:ring-[#2E3856]"
                />
                {uploadingAvatar && (
                  <div className="absolute inset-0 rounded-full bg-black/45 flex items-center justify-center">
                    <Loader2 className="w-5 h-5 text-white animate-spin" />
                  </div>
                )}
              </div>

              <div className="flex flex-wrap items-center gap-2.5">
                {PRESET_AVATARS.map((preset) => (
                  <button
                    key={preset.id}
                    type="button"
                    disabled={uploadingAvatar}
                    onClick={() => handleSelectPresetAvatar(preset)}
                    title={`Chọn avatar ${preset.id}`}
                    style={{ backgroundColor: preset.bg }}
                    className="w-10 h-10 rounded-full flex items-center justify-center text-lg shadow-sm hover:scale-110 transition-transform disabled:opacity-50"
                  >
                    <span aria-hidden="true">{preset.emoji}</span>
                  </button>
                ))}

                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/jpeg,image/png,image/webp,image/gif"
                  onChange={handleAvatarFileChange}
                  className="hidden"
                />
                <button
                  type="button"
                  disabled={uploadingAvatar}
                  onClick={() => fileInputRef.current?.click()}
                  title="Tải ảnh của bạn lên"
                  aria-label="Tải ảnh đại diện từ máy"
                  className="w-10 h-10 rounded-full bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#2E3856] dark:hover:bg-[#3A476B] text-[#586380] dark:text-white flex items-center justify-center transition-colors disabled:opacity-50"
                >
                  <Plus className="w-5 h-5" />
                </button>
              </div>
            </div>
          </div>

          {/* Họ và tên & Bio */}
          <div className="px-6 py-5">
            <div className="flex items-center justify-between gap-4">
              <div className="min-w-0">
                <p className="text-[14px] font-bold text-[#282E3E] dark:text-white">
                  Họ và tên & Giới thiệu
                </p>
                <p className="text-[14px] text-[#586380] dark:text-[#A8B1D1] mt-0.5 truncate">
                  {myProfile?.fullName || 'Chưa cập nhật'}
                  {myProfile?.bio ? ` • ${myProfile.bio}` : ''}
                </p>
              </div>
              <button
                type="button"
                onClick={() =>
                  setEditingField((prev) => (prev === 'nameBio' ? null : 'nameBio'))
                }
                className="text-[14px] font-bold text-[#4255FF] dark:text-[#7B8BFF] hover:underline shrink-0"
              >
                {editingField === 'nameBio' ? 'Đóng' : 'Sửa'}
              </button>
            </div>

            {editingField === 'nameBio' && (
              <form onSubmit={handleSaveNameBio} className="mt-4 space-y-3 max-w-lg">
                <div>
                  <label className="block text-xs font-bold text-[#586380] dark:text-[#A8B1D1] mb-1">
                    Họ và tên
                  </label>
                  <input
                    type="text"
                    required
                    maxLength={100}
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    className={inputClass}
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#586380] dark:text-[#A8B1D1] mb-1">
                    Giới thiệu bản thân (Bio)
                  </label>
                  <textarea
                    rows={2}
                    maxLength={500}
                    value={bio}
                    onChange={(e) => setBio(e.target.value)}
                    placeholder="Chia sẻ mục tiêu học tập..."
                    className="w-full p-3 rounded-xl bg-[#F6F7FB] dark:bg-[#0A082D] border-2 border-transparent focus:border-[#4255FF] focus:bg-white dark:focus:bg-[#0A082D] text-[14px] text-[#282E3E] dark:text-white outline-none resize-none"
                  />
                </div>
                <div className="flex items-center gap-2">
                  <button
                    type="submit"
                    disabled={savingProfile}
                    className="h-9 px-5 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white text-xs font-bold inline-flex items-center gap-1.5"
                  >
                    {savingProfile && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                    <span>Lưu</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setEditingField(null)}
                    className="h-9 px-4 rounded-full bg-[#F6F7FB] dark:bg-[#2E3856] text-xs font-bold text-[#586380] dark:text-white"
                  >
                    Hủy
                  </button>
                </div>
              </form>
            )}
          </div>

          {/* Tên người dùng */}
          <div className="px-6 py-5">
            <div className="flex items-center justify-between gap-4">
              <div className="min-w-0">
                <p className="text-[14px] font-bold text-[#282E3E] dark:text-white">
                  Tên người dùng
                </p>
                <p className="text-[14px] text-[#586380] dark:text-[#A8B1D1] mt-0.5 truncate">
                  {myProfile?.username}
                </p>
              </div>
              <button
                type="button"
                onClick={() =>
                  setEditingField((prev) => (prev === 'username' ? null : 'username'))
                }
                className="text-[14px] font-bold text-[#4255FF] dark:text-[#7B8BFF] hover:underline shrink-0"
              >
                {editingField === 'username' ? 'Đóng' : 'Sửa'}
              </button>
            </div>

            {editingField === 'username' && (
              <form onSubmit={handleSaveUsername} className="mt-4 flex flex-wrap items-center gap-2.5 max-w-md">
                <input
                  type="text"
                  required
                  minLength={3}
                  maxLength={50}
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className={`${inputClass} flex-1 min-w-[200px]`}
                />
                <button
                  type="submit"
                  disabled={savingProfile}
                  className="h-10 px-5 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white text-xs font-bold inline-flex items-center gap-1.5"
                >
                  {savingProfile && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                  <span>Lưu</span>
                </button>
              </form>
            )}
          </div>

          {/* Email */}
          <div className="px-6 py-5 flex items-center justify-between gap-4">
            <div className="min-w-0">
              <p className="text-[14px] font-bold text-[#282E3E] dark:text-white">Email</p>
              <p className="text-[14px] text-[#586380] dark:text-[#A8B1D1] mt-0.5 truncate">
                {myProfile?.email || ''}
              </p>
            </div>
            <span className="text-xs font-bold px-3 py-1 rounded-full bg-[#F6F7FB] dark:bg-[#2E3856] text-[#586380] dark:text-[#A8B1D1] shrink-0">
              Cố định
            </span>
          </div>

          {/* Mục tiêu mỗi ngày & Loại tài khoản */}
          <div className="px-6 py-4 flex items-center justify-between gap-4">
            <p className="text-[14px] font-bold text-[#282E3E] dark:text-white">
              Mục tiêu học mỗi ngày
            </p>
            <select
              value={dailyGoalCards}
              onChange={handleGoalChange}
              disabled={savingProfile}
              className={pillSelectClass}
            >
              {!DAILY_GOAL_PRESETS.includes(Number(dailyGoalCards)) && (
                <option value={dailyGoalCards}>{dailyGoalCards} thẻ / ngày</option>
              )}
              {DAILY_GOAL_PRESETS.map((preset) => (
                <option key={preset} value={preset}>
                  {preset} thẻ / ngày
                </option>
              ))}
            </select>
          </div>

          <div className="px-6 py-4 flex items-center justify-between gap-4">
            <p className="text-[14px] font-bold text-[#282E3E] dark:text-white">
              Loại tài khoản
            </p>
            <span className="h-9 px-4 rounded-full bg-[#F6F7FB] dark:bg-[#2E3856] text-[13.5px] font-bold text-[#282E3E] dark:text-white inline-flex items-center">
              {roleMeta.label}
            </span>
          </div>
        </div>
      </section>

      {/* Section 2: Giao diện */}
      <section>
        <h2 className="text-[14px] font-bold text-[#586380] dark:text-[#A8B1D1] mb-3">
          Giao diện
        </h2>

        <div className={cardShellClass}>
          <div className="px-6 py-4 flex items-center justify-between gap-4">
            <p className="text-[14px] font-bold text-[#282E3E] dark:text-white">Hình nền</p>
            <select
              value={theme}
              onChange={(e) => setTheme(e.target.value)}
              className={pillSelectClass}
            >
              <option value="light">Light</option>
              <option value="dark">Dark</option>
            </select>
          </div>

          <div className="px-6 py-4 flex items-center justify-between gap-4">
            <p className="text-[14px] font-bold text-[#282E3E] dark:text-white">Ngôn ngữ</p>
            <select
              value={language}
              onChange={(e) => setLanguage(e.target.value)}
              className={pillSelectClass}
            >
              <option value="vi">Tiếng Việt</option>
            </select>
          </div>
        </div>
      </section>

      {/* Section 3: Thông báo & Múi giờ */}
      <section>
        <h2 className="text-[14px] font-bold text-[#586380] dark:text-[#A8B1D1] mb-3">
          Thông báo
        </h2>

        <div className={cardShellClass}>
          <button
            type="button"
            onClick={() => setNotifOpen((prev) => !prev)}
            className="w-full px-6 py-4 flex items-center justify-between text-left hover:bg-[#F6F7FB]/60 dark:hover:bg-[#2E3856]/40 transition-colors"
          >
            <span className="text-[14px] font-bold text-[#282E3E] dark:text-white">
              Cập nhật cá nhân hóa cho việc học
            </span>
            {notifOpen ? (
              <ChevronUp className="w-5 h-5 text-[#586380] dark:text-[#A8B1D1]" />
            ) : (
              <ChevronDown className="w-5 h-5 text-[#586380] dark:text-[#A8B1D1]" />
            )}
          </button>

          {notifOpen && (
            <>
              <div className="px-6 py-4 flex items-center justify-between gap-4">
                <span className="text-[14px] text-[#586380] dark:text-[#A8B1D1]">
                  Chuỗi phiên học và huy hiệu
                </span>
                <button
                  type="button"
                  onClick={() => setStreakNotif((prev) => !prev)}
                  aria-label="Bật tắt thông báo chuỗi học"
                  className={`w-10 h-10 rounded-full border-2 flex items-center justify-center transition-colors ${
                    streakNotif
                      ? 'border-[#4255FF] bg-[#EDEFFF] text-[#4255FF] dark:bg-[#2E3856] dark:text-[#93A3FF]'
                      : 'border-[#D9DDE8] dark:border-[#3A4369] text-[#939BB4]'
                  }`}
                >
                  <Mail className="w-4 h-4" />
                </button>
              </div>

              <div className="px-6 py-4 flex items-center justify-between gap-4">
                <span className="text-[14px] text-[#586380] dark:text-[#A8B1D1]">
                  Lời nhắc ôn tập thẻ FSRS
                </span>
                <button
                  type="button"
                  onClick={() => setFsrsNotif((prev) => !prev)}
                  aria-label="Bật tắt lời nhắc ôn tập"
                  className={`w-10 h-10 rounded-full border-2 flex items-center justify-center transition-colors ${
                    fsrsNotif
                      ? 'border-[#4255FF] bg-[#EDEFFF] text-[#4255FF] dark:bg-[#2E3856] dark:text-[#93A3FF]'
                      : 'border-[#D9DDE8] dark:border-[#3A4369] text-[#939BB4]'
                  }`}
                >
                  <Bell className="w-4 h-4" />
                </button>
              </div>

              <div className="px-6 py-4 flex items-center justify-between gap-4">
                <span className="text-[14px] text-[#586380] dark:text-[#A8B1D1]">Múi giờ</span>
                <select
                  value={timezone}
                  onChange={handleTimezoneChange}
                  disabled={savingProfile}
                  className={pillSelectClass}
                >
                  {!COMMON_TIMEZONES.some((tz) => tz.value === timezone) && timezone && (
                    <option value={timezone}>{timezone}</option>
                  )}
                  {COMMON_TIMEZONES.map((tz) => (
                    <option key={tz.value} value={tz.value}>
                      {tz.label}
                    </option>
                  ))}
                </select>
              </div>
            </>
          )}
        </div>
      </section>

      {/* Section 4: Tài khoản và quyền riêng tư */}
      <section>
        <h2 className="text-[14px] font-bold text-[#586380] dark:text-[#A8B1D1] mb-3">
          Tài khoản và quyền riêng tư
        </h2>

        <div className={cardShellClass}>
          {/* Tạo / Đổi mật khẩu */}
          <div className="px-6 py-5">
            <div className="flex items-center justify-between gap-4">
              <div>
                <p className="text-[14px] font-bold text-[#282E3E] dark:text-white">
                  {myProfile?.hasPassword ? 'Đổi mật khẩu SmartFlash' : 'Tạo mật khẩu SmartFlash'}
                </p>
                {!myProfile?.hasPassword && (
                  <p className="text-xs text-[#586380] dark:text-[#A8B1D1] mt-0.5">
                    Đặt mật khẩu để đăng nhập cả bằng Email hoặc Tên người dùng
                  </p>
                )}
              </div>
              <button
                type="button"
                onClick={() =>
                  setEditingField((prev) => (prev === 'password' ? null : 'password'))
                }
                className="text-[14px] font-bold text-[#4255FF] dark:text-[#7B8BFF] hover:underline shrink-0"
              >
                {editingField === 'password'
                  ? 'Đóng'
                  : myProfile?.hasPassword
                  ? 'Đổi'
                  : 'Tạo'}
              </button>
            </div>

            {editingField === 'password' && (
              <form onSubmit={handlePasswordSubmit} className="mt-4 space-y-3 max-w-lg">
                {myProfile?.hasPassword && (
                  <div className="relative">
                    <input
                      type={showCurrentPwd ? 'text' : 'password'}
                      value={currentPassword}
                      onChange={(e) => setCurrentPassword(e.target.value)}
                      required
                      placeholder="Mật khẩu hiện tại"
                      className={`${inputClass} pr-11`}
                    />
                    <button
                      type="button"
                      onClick={() => setShowCurrentPwd((prev) => !prev)}
                      className="absolute right-3.5 top-1/2 -translate-y-1/2 text-[#586380] dark:text-[#A8B1D1]"
                    >
                      {showCurrentPwd ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                )}

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div className="relative">
                    <input
                      type={showNewPwd ? 'text' : 'password'}
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      minLength={6}
                      required
                      placeholder="Mật khẩu mới (≥ 6 ký tự)"
                      className={`${inputClass} pr-11`}
                    />
                    <button
                      type="button"
                      onClick={() => setShowNewPwd((prev) => !prev)}
                      className="absolute right-3.5 top-1/2 -translate-y-1/2 text-[#586380] dark:text-[#A8B1D1]"
                    >
                      {showNewPwd ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>

                  <input
                    type={showNewPwd ? 'text' : 'password'}
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    minLength={6}
                    required
                    placeholder="Xác nhận mật khẩu mới"
                    className={inputClass}
                  />
                </div>

                <button
                  type="submit"
                  disabled={savingPassword}
                  className="h-10 px-5 rounded-full bg-[#4255FF] hover:bg-[#3545E0] text-white text-xs font-bold inline-flex items-center gap-1.5"
                >
                  {savingPassword && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                  <span>{myProfile?.hasPassword ? 'Cập nhật mật khẩu' : 'Đặt mật khẩu'}</span>
                </button>
              </form>
            )}
          </div>

          {/* Quyền riêng tư */}
          <div className="px-6 py-5 flex items-center justify-between gap-4">
            <div>
              <p className="text-[14px] font-bold text-[#282E3E] dark:text-white">Quyền riêng tư</p>
              <p className="text-[13px] text-[#586380] dark:text-[#A8B1D1] mt-0.5">
                Hồ sơ: <span className="font-semibold text-[#282E3E] dark:text-white">{visibilityInfo.label}</span> • Đang bật{' '}
                <span className="font-semibold text-[#4255FF] dark:text-[#93A3FF]">{visibleTabCount}/4 tab</span>
              </p>
            </div>
            <button
              type="button"
              onClick={onOpenPrivacyModal}
              className="h-10 px-4 rounded-full bg-[#F6F7FB] hover:bg-[#EDEFF4] dark:bg-[#2E3856] dark:hover:bg-[#3A476B] text-[13.5px] font-bold text-[#282E3E] dark:text-white inline-flex items-center gap-2 shrink-0 transition-colors"
            >
              <Shield className="w-4 h-4 text-[#4255FF] dark:text-[#93A3FF]" />
              <span>Tùy chỉnh</span>
            </button>
          </div>

          {/* Xóa tài khoản */}
          <div className="px-6 py-5">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <p className="text-[14px] font-bold text-[#282E3E] dark:text-white">
                  Xóa tài khoản của bạn
                </p>
                <p className="text-[13px] text-[#586380] dark:text-[#A8B1D1] mt-0.5">
                  Thao tác này sẽ xóa toàn bộ dữ liệu học tập của bạn và không thể hoàn tác.
                </p>
              </div>
              <button
                type="button"
                onClick={() => setDeleteConfirmOpen((prev) => !prev)}
                className="h-10 px-5 rounded-full bg-[#B40F2D] hover:bg-[#920B23] text-white text-[13.5px] font-bold shrink-0 transition-colors"
              >
                Xóa tài khoản
              </button>
            </div>

            {deleteConfirmOpen && (
              <div className="mt-4 p-4 rounded-xl bg-red-50 dark:bg-[#431F25] border border-red-200 dark:border-red-900 flex flex-wrap items-center justify-between gap-3">
                <p className="text-xs font-semibold text-red-700 dark:text-red-300">
                  Tính năng tự xóa tài khoản vĩnh viễn sẽ được kích hoạt khi hoàn thiện cổng Quản trị (Phase 8).
                </p>
                <button
                  type="button"
                  onClick={() => setDeleteConfirmOpen(false)}
                  className="text-xs font-bold text-red-700 dark:text-red-300 underline"
                >
                  Đóng
                </button>
              </div>
            )}
          </div>
        </div>
      </section>
    </div>
  );
}

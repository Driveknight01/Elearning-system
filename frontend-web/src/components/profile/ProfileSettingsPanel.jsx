import React, { useEffect, useRef, useState } from 'react';
import {
  Camera,
  CheckCircle2,
  AlertCircle,
  Loader2,
  Lock,
  Eye,
  EyeOff,
  Shield,
  Save,
  Upload,
} from 'lucide-react';
import userApi from '../../api/userApi';
import UserAvatar from '../common/UserAvatar';
import { extractApiErrorMessage, VISIBILITY_META } from '../../utils/userHelpers';

const COMMON_TIMEZONES = [
  'Asia/Ho_Chi_Minh',
  'Asia/Bangkok',
  'Asia/Singapore',
  'Asia/Tokyo',
  'Asia/Seoul',
  'UTC',
  'Europe/London',
  'America/New_York',
  'America/Los_Angeles',
];

const DAILY_GOAL_PRESETS = [10, 20, 30, 50];
const ALLOWED_AVATAR_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp', 'image/gif'];
const MAX_AVATAR_BYTES = 5 * 1024 * 1024;

export default function ProfileSettingsPanel({
  myProfile,
  onProfileUpdated,
  onAvatarUpdated,
  onOpenPrivacyModal,
}) {
  const fileInputRef = useRef(null);

  // Profile info form state
  const [fullName, setFullName] = useState('');
  const [username, setUsername] = useState('');
  const [bio, setBio] = useState('');
  const [dailyGoalCards, setDailyGoalCards] = useState(20);
  const [timezone, setTimezone] = useState('Asia/Ho_Chi_Minh');
  const [savingProfile, setSavingProfile] = useState(false);
  const [profileMsg, setProfileMsg] = useState({ type: '', text: '' });

  // Avatar upload state
  const [uploadingAvatar, setUploadingAvatar] = useState(false);
  const [avatarPreview, setAvatarPreview] = useState('');
  const [avatarMsg, setAvatarMsg] = useState({ type: '', text: '' });

  // Change password form state
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showCurrentPwd, setShowCurrentPwd] = useState(false);
  const [showNewPwd, setShowNewPwd] = useState(false);
  const [savingPassword, setSavingPassword] = useState(false);
  const [passwordMsg, setPasswordMsg] = useState({ type: '', text: '' });

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

  const handleAvatarFileChange = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setAvatarMsg({ type: '', text: '' });

    if (!ALLOWED_AVATAR_TYPES.includes(file.type?.toLowerCase())) {
      setAvatarMsg({
        type: 'error',
        text: 'Chỉ hỗ trợ định dạng ảnh JPG, PNG, WEBP hoặc GIF.',
      });
      e.target.value = '';
      return;
    }

    if (file.size > MAX_AVATAR_BYTES) {
      setAvatarMsg({
        type: 'error',
        text: 'Dung lượng ảnh tối đa là 5MB.',
      });
      e.target.value = '';
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
      setAvatarMsg({
        type: 'success',
        text: res?.message || 'Cập nhật ảnh đại diện thành công!',
      });
    } catch (err) {
      setAvatarPreview('');
      setAvatarMsg({
        type: 'error',
        text: extractApiErrorMessage(err, 'Tải ảnh đại diện thất bại.'),
      });
    } finally {
      setUploadingAvatar(false);
      e.target.value = '';
    }
  };

  const handleProfileSubmit = async (e) => {
    e.preventDefault();
    setProfileMsg({ type: '', text: '' });

    const trimmedName = fullName.trim();
    const trimmedUsername = username.trim();
    const parsedGoal = Number(dailyGoalCards);

    if (!trimmedName) {
      setProfileMsg({ type: 'error', text: 'Họ và tên không được để trống.' });
      return;
    }
    if (!/^[a-zA-Z0-9._-]{3,50}$/.test(trimmedUsername)) {
      setProfileMsg({
        type: 'error',
        text: 'Tên đăng nhập phải từ 3–50 ký tự (chữ, số, dấu chấm, gạch dưới hoặc gạch ngang).',
      });
      return;
    }
    if (Number.isNaN(parsedGoal) || parsedGoal < 1 || parsedGoal > 500) {
      setProfileMsg({
        type: 'error',
        text: 'Mục tiêu thẻ mỗi ngày phải từ 1 đến 500.',
      });
      return;
    }

    setSavingProfile(true);
    try {
      const res = await userApi.updateMyProfile({
        username: trimmedUsername,
        fullName: trimmedName,
        bio: bio.trim() ? bio.trim() : null,
        dailyGoalCards: parsedGoal,
        timezone,
      });
      if (res?.data) {
        onProfileUpdated?.(res.data);
      }
      setProfileMsg({
        type: 'success',
        text: res?.message || 'Cập nhật hồ sơ thành công!',
      });
    } catch (err) {
      setProfileMsg({
        type: 'error',
        text: extractApiErrorMessage(err, 'Không thể cập nhật thông tin cá nhân.'),
      });
    } finally {
      setSavingProfile(false);
    }
  };

  const handlePasswordSubmit = async (e) => {
    e.preventDefault();
    setPasswordMsg({ type: '', text: '' });

    if (myProfile?.hasPassword && !currentPassword) {
      setPasswordMsg({ type: 'error', text: 'Vui lòng nhập mật khẩu hiện tại.' });
      return;
    }
    if (newPassword.length < 6) {
      setPasswordMsg({ type: 'error', text: 'Mật khẩu mới phải có ít nhất 6 ký tự.' });
      return;
    }
    if (newPassword !== confirmPassword) {
      setPasswordMsg({ type: 'error', text: 'Xác nhận mật khẩu mới không khớp.' });
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
      setPasswordMsg({
        type: 'success',
        text: res?.message || 'Đổi mật khẩu thành công!',
      });
    } catch (err) {
      setPasswordMsg({
        type: 'error',
        text: extractApiErrorMessage(err, 'Không thể đổi mật khẩu.'),
      });
    } finally {
      setSavingPassword(false);
    }
  };

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

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
      {/* Left Column: Avatar & Privacy Shortcut */}
      <div className="space-y-6">
        {/* Avatar Card */}
        <div className="bg-white rounded-2xl border border-gray-200 p-6 shadow-sm">
          <h3 className="text-base font-extrabold text-quizlet-textDark">Ảnh đại diện</h3>
          <p className="text-xs text-quizlet-textMuted mt-1">
            Hỗ trợ JPG, PNG, WEBP, GIF dung lượng tối đa 5MB.
          </p>

          <div className="mt-5 flex flex-col items-center text-center">
            <div className="relative">
              <UserAvatar
                src={avatarPreview || myProfile?.avatarUrl}
                fullName={myProfile?.fullName}
                username={myProfile?.username}
                size="lg"
              />
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                disabled={uploadingAvatar}
                aria-label="Chọn ảnh đại diện mới"
                className="absolute -bottom-1 -right-1 w-8 h-8 rounded-full bg-quizlet-blue hover:bg-blue-600 text-white flex items-center justify-center shadow-md border-2 border-white transition"
              >
                {uploadingAvatar ? (
                  <Loader2 className="w-4 h-4 animate-spin" />
                ) : (
                  <Camera className="w-4 h-4" />
                )}
              </button>
            </div>

            <input
              ref={fileInputRef}
              type="file"
              accept="image/jpeg,image/png,image/webp,image/gif"
              onChange={handleAvatarFileChange}
              className="hidden"
            />

            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              disabled={uploadingAvatar}
              className="mt-4 inline-flex items-center gap-2 px-4 py-2 rounded-xl border border-gray-200 hover:bg-gray-50 text-xs font-bold text-quizlet-textDark transition disabled:opacity-60"
            >
              <Upload className="w-3.5 h-3.5" />
              <span>{uploadingAvatar ? 'Đang tải lên...' : 'Tải ảnh mới lên'}</span>
            </button>

            {avatarMsg.text && (
              <div
                className={`mt-3 w-full flex items-center gap-2 p-2.5 rounded-xl text-xs text-left ${
                  avatarMsg.type === 'success'
                    ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                    : 'bg-red-50 text-red-700 border border-red-200'
                }`}
              >
                {avatarMsg.type === 'success' ? (
                  <CheckCircle2 className="w-4 h-4 shrink-0" />
                ) : (
                  <AlertCircle className="w-4 h-4 shrink-0" />
                )}
                <span>{avatarMsg.text}</span>
              </div>
            )}
          </div>
        </div>

        {/* Privacy Quick Card */}
        <div className="bg-white rounded-2xl border border-gray-200 p-6 shadow-sm">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-blue-50 text-quizlet-blue flex items-center justify-center">
              <Shield className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-base font-extrabold text-quizlet-textDark">Quyền riêng tư</h3>
              <p className="text-xs text-quizlet-textMuted">Kiểm soát hiển thị hồ sơ</p>
            </div>
          </div>

          <div className="mt-4 p-3.5 rounded-xl bg-quizlet-bg border border-gray-200/70 space-y-1.5">
            <div className="flex items-center justify-between text-xs">
              <span className="text-quizlet-textMuted font-medium">Chế độ hồ sơ</span>
              <span className="font-extrabold text-quizlet-textDark">{visibilityInfo.label}</span>
            </div>
            <div className="flex items-center justify-between text-xs">
              <span className="text-quizlet-textMuted font-medium">Tab đang hiển thị</span>
              <span className="font-extrabold text-quizlet-blue">{visibleTabCount} / 4 tab</span>
            </div>
          </div>

          <button
            type="button"
            onClick={onOpenPrivacyModal}
            className="mt-4 w-full py-2.5 px-4 rounded-xl bg-blue-50 hover:bg-blue-100 text-quizlet-blue text-xs font-extrabold transition"
          >
            Chỉnh quyền xem từng tab
          </button>
        </div>
      </div>

      {/* Right Column: Profile Info Form + Change Password Form */}
      <div className="lg:col-span-2 space-y-6">
        {/* Update Profile Information */}
        <form
          onSubmit={handleProfileSubmit}
          className="bg-white rounded-2xl border border-gray-200 p-6 shadow-sm space-y-5"
        >
          <div className="border-b border-gray-100 pb-4">
            <h3 className="text-lg font-extrabold text-quizlet-textDark">Thông tin cá nhân</h3>
            <p className="text-xs text-quizlet-textMuted mt-0.5">
              Cập nhật tên hiển thị, username, giới thiệu và mục tiêu học tập mỗi ngày.
            </p>
          </div>

          {profileMsg.text && (
            <div
              className={`flex items-start gap-2.5 p-3.5 rounded-xl text-sm ${
                profileMsg.type === 'success'
                  ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                  : 'bg-red-50 text-red-700 border border-red-200'
              }`}
            >
              {profileMsg.type === 'success' ? (
                <CheckCircle2 className="w-4 h-4 shrink-0 mt-0.5" />
              ) : (
                <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
              )}
              <span>{profileMsg.text}</span>
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-quizlet-textMuted mb-1.5">
                Họ và tên <span className="text-quizlet-red">*</span>
              </label>
              <input
                type="text"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                maxLength={100}
                required
                className="w-full h-11 px-3.5 rounded-xl bg-quizlet-bg border border-gray-200 focus:bg-white focus:border-quizlet-blue focus:ring-2 focus:ring-blue-100 text-sm text-quizlet-textDark outline-none transition"
              />
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-quizlet-textMuted mb-1.5">
                Tên đăng nhập (Username) <span className="text-quizlet-red">*</span>
              </label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                maxLength={50}
                required
                className="w-full h-11 px-3.5 rounded-xl bg-quizlet-bg border border-gray-200 focus:bg-white focus:border-quizlet-blue focus:ring-2 focus:ring-blue-100 text-sm text-quizlet-textDark outline-none transition"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-quizlet-textMuted mb-1.5">
              Email tài khoản
            </label>
            <input
              type="email"
              value={myProfile?.email || ''}
              disabled
              className="w-full h-11 px-3.5 rounded-xl bg-gray-100 border border-gray-200 text-sm text-quizlet-textMuted cursor-not-allowed"
            />
          </div>

          <div>
            <div className="flex items-center justify-between mb-1.5">
              <label className="text-xs font-bold uppercase tracking-wider text-quizlet-textMuted">
                Giới thiệu bản thân (Bio)
              </label>
              <span className="text-[11px] font-semibold text-quizlet-textMuted">
                {bio.length}/500
              </span>
            </div>
            <textarea
              value={bio}
              onChange={(e) => setBio(e.target.value)}
              maxLength={500}
              rows={3}
              placeholder="Chia sẻ mục tiêu học tập hoặc chuyên ngành của bạn..."
              className="w-full p-3.5 rounded-xl bg-quizlet-bg border border-gray-200 focus:bg-white focus:border-quizlet-blue focus:ring-2 focus:ring-blue-100 text-sm text-quizlet-textDark outline-none transition resize-none"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-quizlet-textMuted mb-1.5">
                Mục tiêu học mỗi ngày (thẻ)
              </label>
              <input
                type="number"
                min={1}
                max={500}
                value={dailyGoalCards}
                onChange={(e) => setDailyGoalCards(e.target.value)}
                className="w-full h-11 px-3.5 rounded-xl bg-quizlet-bg border border-gray-200 focus:bg-white focus:border-quizlet-blue focus:ring-2 focus:ring-blue-100 text-sm text-quizlet-textDark outline-none transition"
              />
              <div className="mt-2 flex flex-wrap gap-1.5">
                {DAILY_GOAL_PRESETS.map((preset) => (
                  <button
                    key={preset}
                    type="button"
                    onClick={() => setDailyGoalCards(preset)}
                    className={`px-2.5 py-1 rounded-lg text-xs font-bold border transition ${
                      Number(dailyGoalCards) === preset
                        ? 'bg-quizlet-blue text-white border-quizlet-blue'
                        : 'bg-white text-quizlet-textMuted border-gray-200 hover:border-gray-300'
                    }`}
                  >
                    {preset} thẻ
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-quizlet-textMuted mb-1.5">
                Múi giờ (Timezone)
              </label>
              <select
                value={timezone}
                onChange={(e) => setTimezone(e.target.value)}
                className="w-full h-11 px-3.5 rounded-xl bg-quizlet-bg border border-gray-200 focus:bg-white focus:border-quizlet-blue focus:ring-2 focus:ring-blue-100 text-sm text-quizlet-textDark outline-none transition"
              >
                {!COMMON_TIMEZONES.includes(timezone) && timezone && (
                  <option value={timezone}>{timezone}</option>
                )}
                {COMMON_TIMEZONES.map((tz) => (
                  <option key={tz} value={tz}>
                    {tz}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="pt-2 flex justify-end">
            <button
              type="submit"
              disabled={savingProfile}
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-quizlet-blue hover:bg-blue-600 disabled:opacity-60 text-white text-sm font-bold shadow-sm transition"
            >
              {savingProfile ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <Save className="w-4 h-4" />
              )}
              <span>Lưu thay đổi</span>
            </button>
          </div>
        </form>

        {/* Change Password Form */}
        <form
          onSubmit={handlePasswordSubmit}
          className="bg-white rounded-2xl border border-gray-200 p-6 shadow-sm space-y-5"
        >
          <div className="border-b border-gray-100 pb-4 flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-quizlet-bg text-quizlet-darkBlue flex items-center justify-center">
              <Lock className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-lg font-extrabold text-quizlet-textDark">
                {myProfile?.hasPassword ? 'Đổi mật khẩu' : 'Thiết lập mật khẩu'}
              </h3>
              <p className="text-xs text-quizlet-textMuted">
                {myProfile?.hasPassword
                  ? 'Nên sử dụng mật khẩu mạnh từ 6 ký tự trở lên.'
                  : 'Tài khoản Google của bạn chưa có mật khẩu. Bạn có thể đặt mật khẩu để đăng nhập bằng Email/Username.'}
              </p>
            </div>
          </div>

          {passwordMsg.text && (
            <div
              className={`flex items-start gap-2.5 p-3.5 rounded-xl text-sm ${
                passwordMsg.type === 'success'
                  ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                  : 'bg-red-50 text-red-700 border border-red-200'
              }`}
            >
              {passwordMsg.type === 'success' ? (
                <CheckCircle2 className="w-4 h-4 shrink-0 mt-0.5" />
              ) : (
                <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
              )}
              <span>{passwordMsg.text}</span>
            </div>
          )}

          {myProfile?.hasPassword && (
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-quizlet-textMuted mb-1.5">
                Mật khẩu hiện tại
              </label>
              <div className="relative">
                <input
                  type={showCurrentPwd ? 'text' : 'password'}
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  required
                  placeholder="Nhập mật khẩu hiện tại"
                  className="w-full h-11 pl-3.5 pr-11 rounded-xl bg-quizlet-bg border border-gray-200 focus:bg-white focus:border-quizlet-blue focus:ring-2 focus:ring-blue-100 text-sm text-quizlet-textDark outline-none transition"
                />
                <button
                  type="button"
                  onClick={() => setShowCurrentPwd((prev) => !prev)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-quizlet-textMuted hover:text-quizlet-textDark"
                >
                  {showCurrentPwd ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-quizlet-textMuted mb-1.5">
                Mật khẩu mới
              </label>
              <div className="relative">
                <input
                  type={showNewPwd ? 'text' : 'password'}
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  minLength={6}
                  maxLength={100}
                  required
                  placeholder="Tối thiểu 6 ký tự"
                  className="w-full h-11 pl-3.5 pr-11 rounded-xl bg-quizlet-bg border border-gray-200 focus:bg-white focus:border-quizlet-blue focus:ring-2 focus:ring-blue-100 text-sm text-quizlet-textDark outline-none transition"
                />
                <button
                  type="button"
                  onClick={() => setShowNewPwd((prev) => !prev)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-quizlet-textMuted hover:text-quizlet-textDark"
                >
                  {showNewPwd ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-quizlet-textMuted mb-1.5">
                Xác nhận mật khẩu mới
              </label>
              <input
                type={showNewPwd ? 'text' : 'password'}
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                minLength={6}
                maxLength={100}
                required
                placeholder="Nhập lại mật khẩu mới"
                className="w-full h-11 px-3.5 rounded-xl bg-quizlet-bg border border-gray-200 focus:bg-white focus:border-quizlet-blue focus:ring-2 focus:ring-blue-100 text-sm text-quizlet-textDark outline-none transition"
              />
            </div>
          </div>

          <div className="pt-2 flex justify-end">
            <button
              type="submit"
              disabled={savingPassword}
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-quizlet-darkBlue hover:bg-[#232b42] disabled:opacity-60 text-white text-sm font-bold shadow-sm transition"
            >
              {savingPassword && <Loader2 className="w-4 h-4 animate-spin" />}
              <span>{myProfile?.hasPassword ? 'Cập nhật mật khẩu' : 'Đặt mật khẩu'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

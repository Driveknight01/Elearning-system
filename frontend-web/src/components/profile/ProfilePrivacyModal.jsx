import React, { useEffect, useState } from 'react';
import {
  X,
  Globe,
  Users,
  Lock,
  Layers,
  FolderOpen,
  GraduationCap,
  Award,
  Loader2,
  ShieldCheck,
  AlertCircle,
} from 'lucide-react';
import userApi from '../../api/userApi';
import { extractApiErrorMessage } from '../../utils/userHelpers';

const VISIBILITY_OPTIONS = [
  {
    value: 'PUBLIC',
    title: 'Công khai (Public)',
    description: 'Mọi người, kể cả Khách chưa đăng nhập, đều có thể xem trang cá nhân.',
    icon: Globe,
  },
  {
    value: 'LOGGED_IN_ONLY',
    title: 'Chỉ thành viên đã đăng nhập',
    description: 'Chỉ người dùng có tài khoản SmartFlash đang đăng nhập mới xem được.',
    icon: Users,
  },
  {
    value: 'PRIVATE',
    title: 'Riêng tư (Private)',
    description: 'Ẩn hoàn toàn trang cá nhân khỏi người khác (chỉ bạn và Admin xem được).',
    icon: Lock,
  },
];

const TAB_TOGGLES = [
  {
    key: 'showSets',
    label: 'Tab Bộ thẻ (Sets)',
    description: 'Hiển thị các bộ thẻ công khai bạn đã tạo.',
    icon: Layers,
  },
  {
    key: 'showFolders',
    label: 'Tab Thư mục (Folders)',
    description: 'Hiển thị các thư mục công khai của bạn.',
    icon: FolderOpen,
  },
  {
    key: 'showClasses',
    label: 'Tab Lớp học (Classes)',
    description: 'Hiển thị danh sách lớp học bạn đang tham gia hoặc giảng dạy.',
    icon: GraduationCap,
  },
  {
    key: 'showAchievements',
    label: 'Tab Thành tựu (Achievements)',
    description: 'Hiển thị các huy hiệu và cột mốc XP bạn đã mở khóa.',
    icon: Award,
  },
];

export default function ProfilePrivacyModal({
  isOpen,
  onClose,
  initialVisibility = 'PUBLIC',
  initialConfig = {
    showSets: true,
    showFolders: true,
    showClasses: true,
    showAchievements: true,
  },
  onSaved,
}) {
  const [visibility, setVisibility] = useState(initialVisibility);
  const [tabConfig, setTabConfig] = useState({
    showSets: true,
    showFolders: true,
    showClasses: true,
    showAchievements: true,
  });
  const [saving, setSaving] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  useEffect(() => {
    if (isOpen) {
      setVisibility(initialVisibility || 'PUBLIC');
      setTabConfig({
        showSets: initialConfig?.showSets ?? true,
        showFolders: initialConfig?.showFolders ?? true,
        showClasses: initialConfig?.showClasses ?? true,
        showAchievements: initialConfig?.showAchievements ?? true,
      });
      setErrorMsg('');
    }
  }, [isOpen, initialVisibility, initialConfig]);

  useEffect(() => {
    if (!isOpen) return;
    const handleEsc = (e) => {
      if (e.key === 'Escape' && !saving) {
        onClose();
      }
    };
    document.addEventListener('keydown', handleEsc);
    return () => document.removeEventListener('keydown', handleEsc);
  }, [isOpen, saving, onClose]);

  if (!isOpen) return null;

  const handleToggleTab = (key) => {
    setTabConfig((prev) => ({
      ...prev,
      [key]: !prev[key],
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setSaving(true);
    try {
      const res = await userApi.updatePrivacy({
        profileVisibility: visibility,
        profilePrivacyConfig: tabConfig,
      });
      if (onSaved && res?.data) {
        onSaved(res.data);
      }
      onClose();
    } catch (err) {
      setErrorMsg(extractApiErrorMessage(err, 'Không thể lưu cài đặt quyền riêng tư.'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-[2px]"
      onClick={() => !saving && onClose()}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="privacy-modal-title"
        onClick={(e) => e.stopPropagation()}
        className="bg-white dark:bg-[#202140] rounded-3xl shadow-2xl border border-[#EDEFF4] dark:border-[#2E3856] w-full max-w-xl max-h-[90vh] flex flex-col overflow-hidden"
      >
        {/* Header */}
        <div className="px-6 py-5 border-b border-[#EDEFF4] dark:border-[#2E3856] flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-blue-50 dark:bg-[#2E3856] text-[#4255FF] dark:text-[#93A3FF] flex items-center justify-center">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <div>
              <h2 id="privacy-modal-title" className="text-lg font-extrabold text-[#282E3E] dark:text-white">
                Quyền riêng tư trang cá nhân
              </h2>
              <p className="text-xs text-[#586380] dark:text-[#A8B1D1]">
                Kiểm soát ai được phép xem hồ sơ và từng tab nội dung của bạn
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={saving}
            aria-label="Đóng hộp thoại"
            className="p-2 rounded-xl text-[#586380] dark:text-[#A8B1D1] hover:bg-[#F6F7FB] dark:hover:bg-[#2E3856] hover:text-[#282E3E] dark:hover:text-white transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <form onSubmit={handleSubmit} className="overflow-y-auto px-6 py-5 space-y-6">
          {errorMsg && (
            <div className="flex items-start gap-2.5 p-3.5 rounded-xl bg-red-50 dark:bg-[#431F25] border border-red-200 dark:border-red-900/50 text-sm text-red-700 dark:text-red-300">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
              <span>{errorMsg}</span>
            </div>
          )}

          {/* Section 1: Overall Visibility */}
          <div>
            <h3 className="text-sm font-extrabold text-[#282E3E] dark:text-white mb-3">
              1. Ai có thể truy cập trang cá nhân của bạn?
            </h3>
            <div className="space-y-2.5">
              {VISIBILITY_OPTIONS.map((option) => {
                const Icon = option.icon;
                const selected = visibility === option.value;
                return (
                  <button
                    key={option.value}
                    type="button"
                    onClick={() => setVisibility(option.value)}
                    className={`w-full text-left p-3.5 rounded-2xl border-2 transition flex items-start gap-3.5 ${
                      selected
                        ? 'border-[#4255FF] bg-blue-50/50 dark:bg-[#2E3856]/70'
                        : 'border-[#EDEFF4] dark:border-[#2E3856] hover:border-[#D9DDE8] dark:hover:border-[#3D4870] bg-white dark:bg-[#0A082D]/60'
                    }`}
                  >
                    <div
                      className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 mt-0.5 ${
                        selected
                          ? 'bg-[#4255FF] text-white'
                          : 'bg-[#F6F7FB] dark:bg-[#2E3856] text-[#586380] dark:text-[#A8B1D1]'
                      }`}
                    >
                      <Icon className="w-4 h-4" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-sm font-bold text-[#282E3E] dark:text-white">
                          {option.title}
                        </span>
                        <span
                          className={`w-4 h-4 rounded-full border-2 flex items-center justify-center shrink-0 ${
                            selected ? 'border-[#4255FF]' : 'border-gray-300 dark:border-[#586380]'
                          }`}
                        >
                          {selected && <span className="w-2 h-2 rounded-full bg-[#4255FF]" />}
                        </span>
                      </div>
                      <p className="mt-1 text-xs text-[#586380] dark:text-[#A8B1D1] leading-relaxed">
                        {option.description}
                      </p>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Section 2: Granular Tab Toggles */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-sm font-extrabold text-[#282E3E] dark:text-white">
                2. Bật / tắt quyền xem từng tab với người khác
              </h3>
              {visibility === 'PRIVATE' && (
                <span className="text-[11px] font-bold text-amber-700 dark:text-amber-300 bg-amber-50 dark:bg-amber-950/50 px-2.5 py-0.5 rounded-full border border-amber-200 dark:border-amber-800">
                  Đang ở chế độ Riêng tư
                </span>
              )}
            </div>

            <div
              className={`rounded-2xl border border-[#EDEFF4] dark:border-[#2E3856] divide-y divide-[#EDEFF4] dark:divide-[#2E3856] ${
                visibility === 'PRIVATE' ? 'opacity-60' : ''
              }`}
            >
              {TAB_TOGGLES.map((item) => {
                const Icon = item.icon;
                const checked = !!tabConfig[item.key];
                return (
                  <div
                    key={item.key}
                    className="p-3.5 flex items-center justify-between gap-3"
                  >
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="w-9 h-9 rounded-xl bg-[#F6F7FB] dark:bg-[#2E3856] text-[#586380] dark:text-[#A8B1D1] flex items-center justify-center shrink-0">
                        <Icon className="w-4 h-4" />
                      </div>
                      <div className="min-w-0">
                        <p className="text-sm font-bold text-[#282E3E] dark:text-white">{item.label}</p>
                        <p className="text-xs text-[#586380] dark:text-[#A8B1D1] truncate">
                          {item.description}
                        </p>
                      </div>
                    </div>

                    <button
                      type="button"
                      role="switch"
                      aria-checked={checked}
                      aria-label={item.label}
                      onClick={() => handleToggleTab(item.key)}
                      className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-[#4255FF] focus:ring-offset-2 ${
                        checked ? 'bg-[#4255FF]' : 'bg-gray-300 dark:bg-[#3A4369]'
                      }`}
                    >
                      <span
                        className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ${
                          checked ? 'translate-x-5' : 'translate-x-0'
                        }`}
                      />
                    </button>
                  </div>
                );
              })}
            </div>
            <p className="mt-2.5 text-xs text-[#586380] dark:text-[#A8B1D1]">
              Lưu ý: Khi chính bạn xem trang cá nhân của mình, tất cả các tab luôn hiển thị để bạn dễ quản lý.
            </p>
          </div>

          {/* Footer Buttons */}
          <div className="pt-2 border-t border-[#EDEFF4] dark:border-[#2E3856] flex items-center justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              disabled={saving}
              className="px-4 py-2.5 rounded-full border border-[#D9DDE8] dark:border-[#3A4369] text-sm font-bold text-[#586380] dark:text-[#A8B1D1] hover:bg-[#F6F7FB] dark:hover:bg-[#2E3856] hover:text-[#282E3E] dark:hover:text-white transition"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={saving}
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-full bg-[#4255FF] hover:bg-[#3545E0] disabled:opacity-60 text-white text-sm font-bold shadow-sm transition"
            >
              {saving && <Loader2 className="w-4 h-4 animate-spin" />}
              <span>Lưu quyền riêng tư</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

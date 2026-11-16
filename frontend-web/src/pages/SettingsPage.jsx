import React, { useCallback, useEffect, useState } from 'react';
import MainLayout from '../components/layout/MainLayout';
import ProfileSettingsPanel from '../components/profile/ProfileSettingsPanel';
import ProfilePrivacyModal from '../components/profile/ProfilePrivacyModal';
import userApi from '../api/userApi';
import { useAuthStore } from '../store/useAuthStore';
import { extractApiErrorMessage } from '../utils/userHelpers';

export default function SettingsPage() {
  const { user, setUser } = useAuthStore();
  const [myProfile, setMyProfile] = useState(user || null);
  const [loading, setLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState('');
  const [privacyModalOpen, setPrivacyModalOpen] = useState(false);

  const loadMyProfile = useCallback(async () => {
    setLoading(true);
    setErrorMsg('');
    try {
      const res = await userApi.getMyProfile();
      if (res?.data) {
        setMyProfile(res.data);
        setUser(res.data);
      }
    } catch (err) {
      setErrorMsg(extractApiErrorMessage(err, 'Không thể tải thông tin cài đặt tài khoản.'));
    } finally {
      setLoading(false);
    }
  }, [setUser]);

  useEffect(() => {
    loadMyProfile();
  }, [loadMyProfile]);

  const handleProfileUpdated = (updatedMe) => {
    setMyProfile(updatedMe);
    setUser(updatedMe);
  };

  const handleAvatarUpdated = (newAvatarUrl) => {
    setMyProfile((prev) => (prev ? { ...prev, avatarUrl: newAvatarUrl } : prev));
    if (user) {
      setUser({ ...user, avatarUrl: newAvatarUrl });
    }
  };

  const handlePrivacySaved = (updatedMe) => {
    setMyProfile(updatedMe);
    setUser(updatedMe);
  };

  const ownerTabPrivacy = myProfile?.profilePrivacyConfig || {
    showSets: true,
    showFolders: true,
    showClasses: true,
    showAchievements: true,
  };

  return (
    <MainLayout>
      <div className="max-w-[780px] mx-auto px-5 sm:px-8 pt-6 pb-20">
        <h1 className="text-[28px] sm:text-[32px] font-extrabold tracking-tight text-[#282E3E] dark:text-white mb-6">
          Cài đặt
        </h1>

        {loading ? (
          <div className="space-y-6 animate-pulse">
            <div className="h-64 rounded-2xl border-2 border-[#D9DDE8] dark:border-[#2E3856] bg-white dark:bg-[#202140]" />
            <div className="h-32 rounded-2xl border-2 border-[#D9DDE8] dark:border-[#2E3856] bg-white dark:bg-[#202140]" />
          </div>
        ) : errorMsg ? (
          <div className="rounded-2xl border-2 border-red-200 dark:border-red-900 bg-red-50 dark:bg-[#431F25] p-6 text-sm font-semibold text-red-700 dark:text-red-300">
            {errorMsg}
          </div>
        ) : (
          <ProfileSettingsPanel
            myProfile={myProfile}
            onProfileUpdated={handleProfileUpdated}
            onAvatarUpdated={handleAvatarUpdated}
            onOpenPrivacyModal={() => setPrivacyModalOpen(true)}
          />
        )}
      </div>

      <ProfilePrivacyModal
        isOpen={privacyModalOpen}
        onClose={() => setPrivacyModalOpen(false)}
        initialVisibility={myProfile?.profileVisibility || 'PUBLIC'}
        initialConfig={ownerTabPrivacy}
        onSaved={handlePrivacySaved}
      />
    </MainLayout>
  );
}

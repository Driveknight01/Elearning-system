package com.elearning.module.user.service;

import com.elearning.common.exception.AppException;
import com.elearning.common.exception.ErrorCode;
import com.elearning.common.service.LocalStorageService;
import com.elearning.module.user.dto.AvatarUploadResponse;
import com.elearning.module.user.dto.ChangePasswordRequest;
import com.elearning.module.user.dto.MyProfileResponse;
import com.elearning.module.user.dto.PublicProfileResponse;
import com.elearning.module.user.dto.UpdatePrivacyRequest;
import com.elearning.module.user.dto.UpdateProfileRequest;
import com.elearning.module.user.entity.ProfilePrivacyConfig;
import com.elearning.module.user.entity.ProfileVisibility;
import com.elearning.module.user.entity.Role;
import com.elearning.module.user.entity.User;
import com.elearning.module.user.entity.UserStreak;
import com.elearning.module.user.repository.UserRepository;
import com.elearning.module.user.repository.UserStreakRepository;
import com.elearning.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Manages user profile updates, avatar uploads, password changes,
 * and granular profile/tab privacy enforcement.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserStreakRepository userStreakRepository;
    private final PasswordEncoder passwordEncoder;
    private final LocalStorageService localStorageService;
    private final ProfileTabDataLoader profileTabDataLoader;

    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile(Long userId) {
        User user = getActiveUserById(userId);
        UserStreak streak = userStreakRepository.findById(user.getId()).orElse(null);
        return MyProfileResponse.from(user, streak);
    }

    @Transactional
    public MyProfileResponse updateMyProfile(Long userId, UpdateProfileRequest request) {
        User user = getActiveUserById(userId);

        if (StringUtils.hasText(request.getUsername())) {
            String newUsername = request.getUsername().trim();
            if (!newUsername.equalsIgnoreCase(user.getUsername())
                    && userRepository.existsByUsernameIgnoreCase(newUsername)) {
                throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
            }
            user.setUsername(newUsername);
        }

        user.setFullName(request.getFullName().trim());
        user.setBio(request.getBio() != null ? request.getBio().trim() : null);

        if (request.getDailyGoalCards() != null) {
            user.setDailyGoalCards(request.getDailyGoalCards());
        }
        if (StringUtils.hasText(request.getTimezone())) {
            user.setTimezone(request.getTimezone().trim());
        }

        User saved = userRepository.save(user);
        UserStreak streak = userStreakRepository.findById(saved.getId()).orElse(null);
        return MyProfileResponse.from(saved, streak);
    }

    @Transactional
    public AvatarUploadResponse uploadAvatar(Long userId, MultipartFile file) {
        User user = getActiveUserById(userId);
        String oldAvatarUrl = user.getAvatarUrl();

        String newAvatarUrl = localStorageService.storeImage(file, "avatars");
        user.setAvatarUrl(newAvatarUrl);
        userRepository.save(user);

        localStorageService.deleteByUrlIfLocal(oldAvatarUrl);
        return new AvatarUploadResponse(newAvatarUrl);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getActiveUserById(userId);

        if (StringUtils.hasText(user.getPasswordHash())) {
            if (!StringUtils.hasText(request.getCurrentPassword())
                    || !passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new AppException(ErrorCode.INVALID_CREDENTIALS, "Mật khẩu hiện tại không chính xác");
            }
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public MyProfileResponse updatePrivacy(Long userId, UpdatePrivacyRequest request) {
        User user = getActiveUserById(userId);
        user.setProfileVisibility(request.getProfileVisibility());

        if (request.getProfilePrivacyConfig() != null) {
            ProfilePrivacyConfig incoming = request.getProfilePrivacyConfig();
            user.setProfilePrivacyConfig(ProfilePrivacyConfig.builder()
                    .showSets(incoming.isShowSets())
                    .showFolders(incoming.isShowFolders())
                    .showClasses(incoming.isShowClasses())
                    .showAchievements(incoming.isShowAchievements())
                    .build());
        }

        User saved = userRepository.save(user);
        UserStreak streak = userStreakRepository.findById(saved.getId()).orElse(null);
        return MyProfileResponse.from(saved, streak);
    }

    @Transactional(readOnly = true)
    public PublicProfileResponse getPublicProfile(String username, UserPrincipal viewer) {
        if (!StringUtils.hasText(username)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        User target = userRepository.findByUsernameIgnoreCaseAndDeletedAtIsNull(username.trim())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        boolean isOwner = viewer != null && viewer.getId().equals(target.getId());
        boolean isAdmin = viewer != null && viewer.getRole() == Role.ADMIN;
        boolean canBypassPrivacy = isOwner || isAdmin;

        if (!canBypassPrivacy) {
            if (target.getProfileVisibility() == ProfileVisibility.PRIVATE) {
                throw new AppException(ErrorCode.FORBIDDEN, "Trang cá nhân này đang ở chế độ riêng tư");
            }
            if (target.getProfileVisibility() == ProfileVisibility.LOGGED_IN_ONLY && viewer == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập để xem trang cá nhân này");
            }
        }

        ProfilePrivacyConfig configured = target.getProfilePrivacyConfig() != null
                ? target.getProfilePrivacyConfig()
                : new ProfilePrivacyConfig();

        ProfilePrivacyConfig visibleTabs = canBypassPrivacy
                ? ProfilePrivacyConfig.builder()
                        .showSets(true)
                        .showFolders(true)
                        .showClasses(true)
                        .showAchievements(true)
                        .build()
                : ProfilePrivacyConfig.builder()
                        .showSets(configured.isShowSets())
                        .showFolders(configured.isShowFolders())
                        .showClasses(configured.isShowClasses())
                        .showAchievements(configured.isShowAchievements())
                        .build();

        List<PublicProfileResponse.ProfileSetItem> sets = visibleTabs.isShowSets()
                ? profileTabDataLoader.loadSets(target.getId(), canBypassPrivacy)
                : null;
        List<PublicProfileResponse.ProfileFolderItem> folders = visibleTabs.isShowFolders()
                ? profileTabDataLoader.loadFolders(target.getId(), canBypassPrivacy)
                : null;
        List<PublicProfileResponse.ProfileClassItem> classes = visibleTabs.isShowClasses()
                ? profileTabDataLoader.loadClasses(target.getId())
                : null;
        List<PublicProfileResponse.ProfileAchievementItem> achievements = visibleTabs.isShowAchievements()
                ? profileTabDataLoader.loadAchievements(target.getId())
                : null;

        UserStreak streak = userStreakRepository.findById(target.getId()).orElse(null);

        return PublicProfileResponse.builder()
                .id(target.getId())
                .username(target.getUsername())
                .fullName(target.getFullName())
                .avatarUrl(target.getAvatarUrl())
                .bio(target.getBio())
                .role(target.getRole())
                .verifiedTeacher(target.isVerifiedTeacher())
                .profileVisibility(target.getProfileVisibility())
                .visibleTabs(visibleTabs)
                .owner(isOwner)
                .currentStreak(streak != null ? streak.getCurrentStreak() : 0)
                .longestStreak(streak != null ? streak.getLongestStreak() : 0)
                .totalXp(streak != null ? streak.getTotalXp() : 0L)
                .joinedAt(target.getCreatedAt())
                .sets(sets)
                .folders(folders)
                .classes(classes)
                .achievements(achievements)
                .build();
    }

    private User getActiveUserById(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}

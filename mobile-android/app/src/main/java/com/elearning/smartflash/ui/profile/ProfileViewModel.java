package com.elearning.smartflash.ui.profile;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.elearning.smartflash.data.remote.dto.AvatarUploadResponseDto;
import com.elearning.smartflash.data.remote.dto.MyProfileDto;
import com.elearning.smartflash.data.remote.dto.ProfilePrivacyConfigDto;
import com.elearning.smartflash.data.remote.dto.PublicProfileDto;
import com.elearning.smartflash.data.remote.dto.UpdateProfileRequestDto;
import com.elearning.smartflash.data.repository.AuthRepository;
import com.elearning.smartflash.data.repository.UserRepository;
import com.elearning.smartflash.util.Resource;

/**
 * ViewModel for {@link ProfileFragment} and public profile lookups.
 */
public class ProfileViewModel extends AndroidViewModel {

    private final UserRepository userRepository;
    private final AuthRepository authRepository;
    private final MediatorLiveData<Resource<MyProfileDto>> profileState = new MediatorLiveData<>();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        this.userRepository = new UserRepository(application);
        this.authRepository = new AuthRepository(application);
    }

    public LiveData<Resource<MyProfileDto>> getProfileState() {
        return profileState;
    }

    public void loadMyProfile() {
        LiveData<Resource<MyProfileDto>> source = userRepository.getMyProfile();
        profileState.addSource(source, resource -> {
            profileState.setValue(resource);
            if (resource != null && resource.getStatus() != Resource.Status.LOADING) {
                profileState.removeSource(source);
            }
        });
    }

    public LiveData<Resource<MyProfileDto>> updateProfile(
            String username,
            String fullName,
            String bio,
            Integer dailyGoalCards,
            String timezone
    ) {
        UpdateProfileRequestDto request = new UpdateProfileRequestDto(
                username, fullName, bio, dailyGoalCards, timezone
        );
        MutableLiveData<Resource<MyProfileDto>> actionResult = new MutableLiveData<>(Resource.loading());
        LiveData<Resource<MyProfileDto>> source = userRepository.updateMyProfile(request);
        profileState.addSource(source, resource -> {
            actionResult.setValue(resource);
            if (resource != null && resource.getStatus() != Resource.Status.LOADING) {
                profileState.removeSource(source);
                if (resource.getStatus() == Resource.Status.SUCCESS) {
                    profileState.setValue(resource);
                }
            }
        });
        return actionResult;
    }

    public LiveData<Resource<AvatarUploadResponseDto>> uploadAvatar(Uri imageUri) {
        MutableLiveData<Resource<AvatarUploadResponseDto>> actionResult = new MutableLiveData<>(Resource.loading());
        LiveData<Resource<AvatarUploadResponseDto>> source = userRepository.uploadAvatar(imageUri);
        profileState.addSource(source, resource -> {
            actionResult.setValue(resource);
            if (resource != null && resource.getStatus() != Resource.Status.LOADING) {
                profileState.removeSource(source);
                if (resource.getStatus() == Resource.Status.SUCCESS) {
                    loadMyProfile();
                }
            }
        });
        return actionResult;
    }

    public LiveData<Resource<String>> changePassword(String currentPassword, String newPassword) {
        return userRepository.changePassword(currentPassword, newPassword);
    }

    public LiveData<Resource<MyProfileDto>> updatePrivacy(
            String profileVisibility,
            ProfilePrivacyConfigDto privacyConfig
    ) {
        MutableLiveData<Resource<MyProfileDto>> actionResult = new MutableLiveData<>(Resource.loading());
        LiveData<Resource<MyProfileDto>> source = userRepository.updatePrivacy(profileVisibility, privacyConfig);
        profileState.addSource(source, resource -> {
            actionResult.setValue(resource);
            if (resource != null && resource.getStatus() != Resource.Status.LOADING) {
                profileState.removeSource(source);
                if (resource.getStatus() == Resource.Status.SUCCESS) {
                    profileState.setValue(resource);
                }
            }
        });
        return actionResult;
    }

    public LiveData<Resource<PublicProfileDto>> getPublicProfile(String username) {
        return userRepository.getPublicProfile(username);
    }

    public void logout(Runnable onComplete) {
        authRepository.logout(onComplete);
    }
}

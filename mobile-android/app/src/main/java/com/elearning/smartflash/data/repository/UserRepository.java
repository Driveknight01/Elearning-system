package com.elearning.smartflash.data.repository;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.elearning.smartflash.data.local.prefs.TokenManager;
import com.elearning.smartflash.data.remote.api.RetrofitClient;
import com.elearning.smartflash.data.remote.api.UserApiService;
import com.elearning.smartflash.data.remote.dto.ApiResponse;
import com.elearning.smartflash.data.remote.dto.AvatarUploadResponseDto;
import com.elearning.smartflash.data.remote.dto.ChangePasswordRequestDto;
import com.elearning.smartflash.data.remote.dto.MyProfileDto;
import com.elearning.smartflash.data.remote.dto.ProfilePrivacyConfigDto;
import com.elearning.smartflash.data.remote.dto.PublicProfileDto;
import com.elearning.smartflash.data.remote.dto.UpdatePrivacyRequestDto;
import com.elearning.smartflash.data.remote.dto.UpdateProfileRequestDto;
import com.elearning.smartflash.util.ApiErrorParser;
import com.elearning.smartflash.util.Resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository handling user profile, avatar upload, password changes,
 * granular privacy settings, and public profile lookups.
 */
public class UserRepository {

    private static final long MAX_AVATAR_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_MIME_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    ));

    private final UserApiService userApi;
    private final TokenManager tokenManager;
    private final ContentResolver contentResolver;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    public UserRepository(Context context) {
        Context appContext = context.getApplicationContext();
        this.userApi = RetrofitClient.getInstance(appContext).getUserApi();
        this.tokenManager = TokenManager.getInstance(appContext);
        this.contentResolver = appContext.getContentResolver();
    }

    public LiveData<Resource<MyProfileDto>> getMyProfile() {
        MutableLiveData<Resource<MyProfileDto>> result = new MutableLiveData<>(Resource.loading());
        userApi.getMyProfile().enqueue(new Callback<ApiResponse<MyProfileDto>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<MyProfileDto>> call,
                    @NonNull Response<ApiResponse<MyProfileDto>> response
            ) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().isSuccess()
                        && response.body().getData() != null) {
                    MyProfileDto profile = response.body().getData();
                    tokenManager.syncUserFromProfile(profile);
                    result.postValue(Resource.success(profile));
                } else {
                    result.postValue(Resource.error(
                            ApiErrorParser.parseResponseError(response, "Không thể tải thông tin hồ sơ.")
                    ));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<MyProfileDto>> call, @NonNull Throwable t) {
                result.postValue(Resource.error(
                        ApiErrorParser.parseThrowable(t, "Không thể tải thông tin hồ sơ.")
                ));
            }
        });
        return result;
    }

    public LiveData<Resource<MyProfileDto>> updateMyProfile(UpdateProfileRequestDto request) {
        MutableLiveData<Resource<MyProfileDto>> result = new MutableLiveData<>(Resource.loading());
        userApi.updateMyProfile(request).enqueue(new Callback<ApiResponse<MyProfileDto>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<MyProfileDto>> call,
                    @NonNull Response<ApiResponse<MyProfileDto>> response
            ) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().isSuccess()
                        && response.body().getData() != null) {
                    MyProfileDto updated = response.body().getData();
                    tokenManager.syncUserFromProfile(updated);
                    String msg = response.body().getMessage() != null
                            ? response.body().getMessage()
                            : "Cập nhật hồ sơ thành công.";
                    result.postValue(Resource.success(updated, msg));
                } else {
                    result.postValue(Resource.error(
                            ApiErrorParser.parseResponseError(response, "Không thể cập nhật hồ sơ.")
                    ));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<MyProfileDto>> call, @NonNull Throwable t) {
                result.postValue(Resource.error(
                        ApiErrorParser.parseThrowable(t, "Không thể cập nhật hồ sơ.")
                ));
            }
        });
        return result;
    }

    public LiveData<Resource<AvatarUploadResponseDto>> uploadAvatar(Uri imageUri) {
        MutableLiveData<Resource<AvatarUploadResponseDto>> result = new MutableLiveData<>(Resource.loading());
        if (imageUri == null) {
            result.setValue(Resource.error("Vui lòng chọn một tệp ảnh."));
            return result;
        }

        ioExecutor.execute(() -> {
            try {
                String mimeType = contentResolver.getType(imageUri);
                if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType.toLowerCase())) {
                    mimeType = "image/jpeg";
                }

                byte[] bytes = readBytesWithLimit(imageUri);
                if (bytes.length == 0) {
                    result.postValue(Resource.error("Tệp ảnh trống hoặc không đọc được."));
                    return;
                }

                String fileName = queryFileName(imageUri, mimeType);
                RequestBody requestBody = RequestBody.create(bytes, MediaType.parse(mimeType));
                MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", fileName, requestBody);

                Response<ApiResponse<AvatarUploadResponseDto>> response = userApi.uploadAvatar(filePart).execute();
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().isSuccess()
                        && response.body().getData() != null) {
                    AvatarUploadResponseDto data = response.body().getData();
                    tokenManager.updateCachedAvatarUrl(data.getAvatarUrl());
                    String msg = response.body().getMessage() != null
                            ? response.body().getMessage()
                            : "Cập nhật ảnh đại diện thành công.";
                    result.postValue(Resource.success(data, msg));
                } else {
                    result.postValue(Resource.error(
                            ApiErrorParser.parseResponseError(response, "Không thể tải lên ảnh đại diện.")
                    ));
                }
            } catch (IllegalArgumentException ex) {
                result.postValue(Resource.error(ex.getMessage()));
            } catch (Exception ex) {
                result.postValue(Resource.error(
                        ApiErrorParser.parseThrowable(ex, "Không thể tải lên ảnh đại diện.")
                ));
            }
        });

        return result;
    }

    public LiveData<Resource<String>> changePassword(String currentPassword, String newPassword) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>(Resource.loading());
        ChangePasswordRequestDto request = new ChangePasswordRequestDto(currentPassword, newPassword);
        userApi.changePassword(request).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<Void>> call,
                    @NonNull Response<ApiResponse<Void>> response
            ) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    String msg = response.body().getMessage() != null
                            ? response.body().getMessage()
                            : "Đổi mật khẩu thành công.";
                    result.postValue(Resource.success(msg, msg));
                } else {
                    result.postValue(Resource.error(
                            ApiErrorParser.parseResponseError(response, "Không thể đổi mật khẩu.")
                    ));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                result.postValue(Resource.error(
                        ApiErrorParser.parseThrowable(t, "Không thể đổi mật khẩu.")
                ));
            }
        });
        return result;
    }

    public LiveData<Resource<MyProfileDto>> updatePrivacy(
            String profileVisibility,
            ProfilePrivacyConfigDto privacyConfig
    ) {
        MutableLiveData<Resource<MyProfileDto>> result = new MutableLiveData<>(Resource.loading());
        UpdatePrivacyRequestDto request = new UpdatePrivacyRequestDto(profileVisibility, privacyConfig);
        userApi.updatePrivacy(request).enqueue(new Callback<ApiResponse<MyProfileDto>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<MyProfileDto>> call,
                    @NonNull Response<ApiResponse<MyProfileDto>> response
            ) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().isSuccess()
                        && response.body().getData() != null) {
                    MyProfileDto updated = response.body().getData();
                    tokenManager.syncUserFromProfile(updated);
                    String msg = response.body().getMessage() != null
                            ? response.body().getMessage()
                            : "Cập nhật quyền riêng tư thành công.";
                    result.postValue(Resource.success(updated, msg));
                } else {
                    result.postValue(Resource.error(
                            ApiErrorParser.parseResponseError(response, "Không thể cập nhật quyền riêng tư.")
                    ));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<MyProfileDto>> call, @NonNull Throwable t) {
                result.postValue(Resource.error(
                        ApiErrorParser.parseThrowable(t, "Không thể cập nhật quyền riêng tư.")
                ));
            }
        });
        return result;
    }

    public LiveData<Resource<PublicProfileDto>> getPublicProfile(String username) {
        MutableLiveData<Resource<PublicProfileDto>> result = new MutableLiveData<>(Resource.loading());
        userApi.getPublicProfile(username).enqueue(new Callback<ApiResponse<PublicProfileDto>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<PublicProfileDto>> call,
                    @NonNull Response<ApiResponse<PublicProfileDto>> response
            ) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().isSuccess()
                        && response.body().getData() != null) {
                    result.postValue(Resource.success(response.body().getData()));
                } else {
                    result.postValue(Resource.error(
                            ApiErrorParser.parseResponseError(response, "Không thể xem trang cá nhân này.")
                    ));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<PublicProfileDto>> call, @NonNull Throwable t) {
                result.postValue(Resource.error(
                        ApiErrorParser.parseThrowable(t, "Không thể xem trang cá nhân này.")
                ));
            }
        });
        return result;
    }

    private byte[] readBytesWithLimit(Uri uri) throws IOException {
        try (InputStream is = contentResolver.openInputStream(uri)) {
            if (is == null) {
                throw new IOException("Không mở được tệp ảnh.");
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            long total = 0;
            int read;
            while ((read = is.read(chunk)) != -1) {
                total += read;
                if (total > MAX_AVATAR_BYTES) {
                    throw new IllegalArgumentException("Ảnh tải lên tối đa 5MB.");
                }
                buffer.write(chunk, 0, read);
            }
            return buffer.toByteArray();
        }
    }

    private String queryFileName(Uri uri, String mimeType) {
        String name = null;
        try (Cursor cursor = contentResolver.query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    name = cursor.getString(idx);
                }
            }
        } catch (Exception ignored) {
            // Fall back to generated name
        }
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }
        String ext = ".jpg";
        if ("image/png".equalsIgnoreCase(mimeType)) {
            ext = ".png";
        } else if ("image/webp".equalsIgnoreCase(mimeType)) {
            ext = ".webp";
        } else if ("image/gif".equalsIgnoreCase(mimeType)) {
            ext = ".gif";
        }
        return "avatar_" + System.currentTimeMillis() + ext;
    }
}

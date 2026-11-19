package com.elearning.smartflash.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.elearning.smartflash.data.local.prefs.TokenManager;
import com.elearning.smartflash.data.remote.api.AuthApiService;
import com.elearning.smartflash.data.remote.api.RetrofitClient;
import com.elearning.smartflash.data.remote.dto.ApiResponse;
import com.elearning.smartflash.data.remote.dto.AuthResponseDto;
import com.elearning.smartflash.data.remote.dto.ForgotPasswordRequestDto;
import com.elearning.smartflash.data.remote.dto.GoogleLoginRequestDto;
import com.elearning.smartflash.data.remote.dto.LoginRequestDto;
import com.elearning.smartflash.data.remote.dto.LogoutRequestDto;
import com.elearning.smartflash.data.remote.dto.RegisterRequestDto;
import com.elearning.smartflash.data.remote.dto.ResetPasswordRequestDto;
import com.elearning.smartflash.data.remote.dto.UserSummaryDto;
import com.elearning.smartflash.util.ApiErrorParser;
import com.elearning.smartflash.util.DeviceUtils;
import com.elearning.smartflash.util.Resource;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository mediating between {@link AuthApiService} and {@link TokenManager}.
 */
public class AuthRepository {

    private final AuthApiService authApi;
    private final TokenManager tokenManager;

    public AuthRepository(Context context) {
        this.authApi = RetrofitClient.getInstance(context).getAuthApi();
        this.tokenManager = TokenManager.getInstance(context);
    }

    public LiveData<Resource<Boolean>> checkEmailExists(String email) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>(Resource.loading());
        authApi.checkEmail(email).enqueue(new Callback<ApiResponse<Boolean>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Boolean>> call, @NonNull Response<ApiResponse<Boolean>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    result.postValue(Resource.success(Boolean.TRUE.equals(response.body().getData())));
                } else {
                    result.postValue(Resource.error("Không thể kiểm tra email."));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Boolean>> call, @NonNull Throwable t) {
                result.postValue(Resource.error("Không thể kiểm tra email."));
            }
        });
        return result;
    }

    public LiveData<Resource<AuthResponseDto>> login(String identifier, String password) {
        MutableLiveData<Resource<AuthResponseDto>> result = new MutableLiveData<>(Resource.loading());
        LoginRequestDto request = new LoginRequestDto(identifier, password, DeviceUtils.getDeviceInfo());
        enqueueAuthCall(authApi.login(request), result, "Đăng nhập thất bại.");
        return result;
    }

    public LiveData<Resource<AuthResponseDto>> register(
            String username,
            String email,
            String password,
            String fullName,
            String role
    ) {
        MutableLiveData<Resource<AuthResponseDto>> result = new MutableLiveData<>(Resource.loading());
        RegisterRequestDto request = new RegisterRequestDto(
                username,
                email,
                password,
                fullName,
                role,
                DeviceUtils.getDeviceInfo()
        );
        enqueueAuthCall(authApi.register(request), result, "Đăng ký tài khoản thất bại.");
        return result;
    }

    public LiveData<Resource<AuthResponseDto>> loginWithGoogle(String idToken) {
        MutableLiveData<Resource<AuthResponseDto>> result = new MutableLiveData<>(Resource.loading());
        GoogleLoginRequestDto request = new GoogleLoginRequestDto(idToken, DeviceUtils.getDeviceInfo());
        enqueueAuthCall(authApi.googleLogin(request), result, "Đăng nhập Google thất bại.");
        return result;
    }

    public LiveData<Resource<String>> forgotPassword(String email) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>(Resource.loading());
        authApi.forgotPassword(new ForgotPasswordRequestDto(email)).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    String msg = response.body().getMessage() != null
                            ? response.body().getMessage()
                            : "Mã OTP đã được gửi tới email của bạn.";
                    result.postValue(Resource.success(msg));
                } else {
                    result.postValue(Resource.error(
                            ApiErrorParser.parseResponseError(response, "Không thể gửi mã OTP.")
                    ));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                result.postValue(Resource.error(ApiErrorParser.parseThrowable(t, "Không thể gửi mã OTP.")));
            }
        });
        return result;
    }

    public LiveData<Resource<String>> resetPassword(String email, String otpCode, String newPassword) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>(Resource.loading());
        authApi.resetPassword(new ResetPasswordRequestDto(email, otpCode, newPassword))
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            String msg = response.body().getMessage() != null
                                    ? response.body().getMessage()
                                    : "Đặt lại mật khẩu thành công!";
                            result.postValue(Resource.success(msg));
                        } else {
                            result.postValue(Resource.error(
                                    ApiErrorParser.parseResponseError(response, "Không thể đặt lại mật khẩu.")
                            ));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                        result.postValue(Resource.error(ApiErrorParser.parseThrowable(t, "Không thể đặt lại mật khẩu.")));
                    }
                });
        return result;
    }

    /**
     * Checks whether the locally stored session is still valid on launch.
     * Note: If network is unreachable offline while cached tokens exist, allows offline entry to MainActivity.
     */
    public LiveData<Resource<Boolean>> verifySession() {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>(Resource.loading());
        if (!tokenManager.isLoggedIn()) {
            result.setValue(Resource.success(false));
            return result;
        }

        authApi.getMe().enqueue(new Callback<ApiResponse<UserSummaryDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<UserSummaryDto>> call, @NonNull Response<ApiResponse<UserSummaryDto>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    if (response.body().getData() != null) {
                        tokenManager.saveUser(response.body().getData());
                    }
                    result.postValue(Resource.success(true));
                } else if (response.code() == 401 || response.code() == 403) {
                    tokenManager.clearSession();
                    result.postValue(Resource.success(false));
                } else {
                    result.postValue(Resource.success(tokenManager.getUser() != null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<UserSummaryDto>> call, @NonNull Throwable t) {
                // Offline-first resilience: keep session when network is unavailable
                boolean hasCachedUser = tokenManager.getUser() != null;
                result.postValue(Resource.success(hasCachedUser));
            }
        });
        return result;
    }

    public void logout(Runnable onComplete) {
        String refreshToken = tokenManager.getRefreshToken();
        if (tokenManager.getAccessToken() == null) {
            tokenManager.clearSession();
            if (onComplete != null) onComplete.run();
            return;
        }

        authApi.logout(new LogoutRequestDto(refreshToken)).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                tokenManager.clearSession();
                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                tokenManager.clearSession();
                if (onComplete != null) onComplete.run();
            }
        });
    }

    private void enqueueAuthCall(
            Call<ApiResponse<AuthResponseDto>> call,
            MutableLiveData<Resource<AuthResponseDto>> result,
            String fallbackError
    ) {
        call.enqueue(new Callback<ApiResponse<AuthResponseDto>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<AuthResponseDto>> call,
                    @NonNull Response<ApiResponse<AuthResponseDto>> response
            ) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().isSuccess()
                        && response.body().getData() != null
                        && response.body().getData().getAccessToken() != null) {

                    AuthResponseDto authData = response.body().getData();
                    tokenManager.saveSession(authData);
                    result.postValue(Resource.success(authData, response.body().getMessage()));
                } else {
                    result.postValue(Resource.error(
                            ApiErrorParser.parseResponseError(response, fallbackError)
                    ));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<AuthResponseDto>> call, @NonNull Throwable t) {
                result.postValue(Resource.error(ApiErrorParser.parseThrowable(t, fallbackError)));
            }
        });
    }
}

package com.elearning.smartflash.data.remote.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.elearning.smartflash.data.local.prefs.TokenManager;
import com.elearning.smartflash.data.remote.dto.ApiResponse;
import com.elearning.smartflash.data.remote.dto.AuthResponseDto;
import com.elearning.smartflash.data.remote.dto.RefreshTokenRequestDto;
import com.elearning.smartflash.util.DeviceUtils;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Call;

/**
 * OkHttp authenticator that transparently refreshes expired JWT access tokens
 * using {@code POST /api/v1/auth/refresh} with token rotation.
 */
public class TokenAuthenticator implements Authenticator {

    private final TokenManager tokenManager;
    private final AuthApiService refreshApiService;

    public TokenAuthenticator(TokenManager tokenManager, AuthApiService refreshApiService) {
        this.tokenManager = tokenManager;
        this.refreshApiService = refreshApiService;
    }

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NonNull Response response) throws IOException {
        String path = response.request().url().encodedPath();
        if (AuthInterceptor.isPublicAuthPath(path)) {
            return null;
        }

        if (responseCount(response) >= 2) {
            return null;
        }

        synchronized (this) {
            String currentAccessToken = tokenManager.getAccessToken();
            String requestAuthHeader = response.request().header("Authorization");

            // Another concurrent request already refreshed the token; retry with the latest token.
            if (currentAccessToken != null
                    && requestAuthHeader != null
                    && !requestAuthHeader.equals("Bearer " + currentAccessToken)) {
                return response.request().newBuilder()
                        .header("Authorization", "Bearer " + currentAccessToken)
                        .build();
            }

            String refreshToken = tokenManager.getRefreshToken();
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                tokenManager.clearSession();
                return null;
            }

            Call<ApiResponse<AuthResponseDto>> call = refreshApiService.refresh(
                    new RefreshTokenRequestDto(refreshToken, DeviceUtils.getDeviceInfo())
            );
            retrofit2.Response<ApiResponse<AuthResponseDto>> refreshRes = call.execute();

            if (refreshRes.isSuccessful()
                    && refreshRes.body() != null
                    && refreshRes.body().isSuccess()
                    && refreshRes.body().getData() != null
                    && refreshRes.body().getData().getAccessToken() != null) {

                AuthResponseDto newSession = refreshRes.body().getData();
                tokenManager.saveSession(newSession);

                return response.request().newBuilder()
                        .header("Authorization", "Bearer " + newSession.getAccessToken())
                        .build();
            }

            tokenManager.clearSession();
            return null;
        }
    }

    private int responseCount(Response response) {
        int count = 1;
        Response prior = response.priorResponse();
        while (prior != null) {
            count++;
            prior = prior.priorResponse();
        }
        return count;
    }
}

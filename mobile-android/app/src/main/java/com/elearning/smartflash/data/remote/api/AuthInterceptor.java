package com.elearning.smartflash.data.remote.api;

import androidx.annotation.NonNull;

import com.elearning.smartflash.data.local.prefs.TokenManager;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OkHttp interceptor that attaches {@code Authorization: Bearer <accessToken>}
 * to outgoing API requests (excluding public auth endpoints).
 */
public class AuthInterceptor implements Interceptor {

    private static final List<String> PUBLIC_AUTH_PATHS = Arrays.asList(
            "/auth/login",
            "/auth/register",
            "/auth/google",
            "/auth/refresh",
            "/auth/forgot-password",
            "/auth/reset-password"
    );

    private final TokenManager tokenManager;

    public AuthInterceptor(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        String path = original.url().encodedPath();

        if (isPublicAuthPath(path)) {
            return chain.proceed(original);
        }

        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            return chain.proceed(original);
        }

        Request authorized = original.newBuilder()
                .header("Authorization", "Bearer " + accessToken)
                .build();
        return chain.proceed(authorized);
    }

    static boolean isPublicAuthPath(String encodedPath) {
        if (encodedPath == null) {
            return false;
        }
        for (String publicPath : PUBLIC_AUTH_PATHS) {
            if (encodedPath.endsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }
}

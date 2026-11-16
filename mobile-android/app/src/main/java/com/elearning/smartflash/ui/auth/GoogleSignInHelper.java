package com.elearning.smartflash.ui.auth;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

import com.elearning.smartflash.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.Task;

/**
 * Shared Google Sign-In helper for LoginActivity and RegisterActivity.
 */
public final class GoogleSignInHelper {

    private static final String PLACEHOLDER_CLIENT_ID = "your-google-client-id.apps.googleusercontent.com";

    private GoogleSignInHelper() {
    }

    public static boolean isConfigured(Context context) {
        String clientId = context.getString(R.string.google_web_client_id).trim();
        return !clientId.isEmpty() && !PLACEHOLDER_CLIENT_ID.equals(clientId);
    }

    public static GoogleSignInClient buildClient(Context context) {
        String webClientId = context.getString(R.string.google_web_client_id).trim();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        return GoogleSignIn.getClient(context, gso);
    }

    public static void signOutSilent(Context context) {
        if (!isConfigured(context)) {
            return;
        }
        buildClient(context).signOut();
    }

    public interface Callback {
        void onIdTokenReady(String idToken);
        void onError(String message);
    }

    public static void handleSignInResult(@Nullable Intent data, Callback callback) {
        if (data == null) {
            callback.onError("Đã hủy đăng nhập Google.");
            return;
        }
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            String idToken = account != null ? account.getIdToken() : null;
            if (idToken == null || idToken.trim().isEmpty()) {
                callback.onError("Không lấy được Google ID Token. Kiểm tra lại google_web_client_id.");
            } else {
                callback.onIdTokenReady(idToken);
            }
        } catch (ApiException ex) {
            if (ex.getStatusCode() == CommonStatusCodes.CANCELED) {
                callback.onError("Đã hủy đăng nhập Google.");
            } else if (ex.getStatusCode() == CommonStatusCodes.DEVELOPER_ERROR) {
                callback.onError("Lỗi cấu hình Google Sign-In (mã 10): kiểm tra Android OAuth Client ID, Package Name (com.elearning.smartflash) và SHA-1 trên Google Cloud Console.");
            } else {
                callback.onError("Đăng nhập Google thất bại (mã lỗi " + ex.getStatusCode() + ").");
            }
        }
    }
}

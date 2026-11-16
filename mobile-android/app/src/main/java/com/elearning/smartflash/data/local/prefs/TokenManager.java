package com.elearning.smartflash.data.local.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import com.elearning.smartflash.data.remote.dto.AuthResponseDto;
import com.elearning.smartflash.data.remote.dto.MyProfileDto;
import com.elearning.smartflash.data.remote.dto.UserSummaryDto;
import com.google.gson.Gson;

/**
 * Manages JWT access/refresh tokens and cached user profile summary in SharedPreferences.
 */
public class TokenManager {

    private static final String PREF_NAME = "smartflash_auth_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_EXPIRES_AT_MS = "expires_at_ms";
    private static final String KEY_USER_JSON = "user_json";
    private static final String KEY_GUEST_MODE = "guest_mode";

    private static volatile TokenManager instance;

    private final SharedPreferences prefs;
    private final Gson gson;

    private TokenManager(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public static TokenManager getInstance(Context context) {
        if (instance == null) {
            synchronized (TokenManager.class) {
                if (instance == null) {
                    instance = new TokenManager(context);
                }
            }
        }
        return instance;
    }

    public synchronized void saveSession(AuthResponseDto authResponse) {
        if (authResponse == null) {
            return;
        }
        long expiresAt = System.currentTimeMillis() + Math.max(0L, authResponse.getExpiresInMs());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_ACCESS_TOKEN, authResponse.getAccessToken());
        if (authResponse.getRefreshToken() != null) {
            editor.putString(KEY_REFRESH_TOKEN, authResponse.getRefreshToken());
        }
        editor.putLong(KEY_EXPIRES_AT_MS, expiresAt);
        if (authResponse.getUser() != null) {
            editor.putString(KEY_USER_JSON, gson.toJson(authResponse.getUser()));
        }
        editor.putBoolean(KEY_GUEST_MODE, false);
        editor.apply();
    }

    public synchronized void saveUser(UserSummaryDto user) {
        if (user == null) {
            prefs.edit().remove(KEY_USER_JSON).apply();
            return;
        }
        prefs.edit().putString(KEY_USER_JSON, gson.toJson(user)).apply();
    }

    public synchronized void syncUserFromProfile(MyProfileDto profile) {
        UserSummaryDto summary = UserSummaryDto.fromProfile(profile);
        if (summary != null) {
            saveUser(summary);
        }
    }

    public synchronized void updateCachedAvatarUrl(String avatarUrl) {
        UserSummaryDto current = getUser();
        if (current != null) {
            current.setAvatarUrl(avatarUrl);
            saveUser(current);
        }
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public long getExpiresAtMs() {
        return prefs.getLong(KEY_EXPIRES_AT_MS, 0L);
    }

    public UserSummaryDto getUser() {
        String raw = prefs.getString(KEY_USER_JSON, null);
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return gson.fromJson(raw, UserSummaryDto.class);
        } catch (Exception ex) {
            return null;
        }
    }

    public boolean isLoggedIn() {
        String access = getAccessToken();
        String refresh = getRefreshToken();
        return (access != null && !access.isEmpty()) || (refresh != null && !refresh.isEmpty());
    }

    public boolean isGuestMode() {
        return prefs.getBoolean(KEY_GUEST_MODE, false);
    }

    public void setGuestMode(boolean guestMode) {
        prefs.edit().putBoolean(KEY_GUEST_MODE, guestMode).apply();
    }

    public synchronized void clearSession() {
        prefs.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_EXPIRES_AT_MS)
                .remove(KEY_USER_JSON)
                .remove(KEY_GUEST_MODE)
                .apply();
    }
}

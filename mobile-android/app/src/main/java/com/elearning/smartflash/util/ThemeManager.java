package com.elearning.smartflash.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

/**
 * Persists and applies the user's selected Light / Dark / System theme mode.
 */
public final class ThemeManager {

    public static final int MODE_SYSTEM = 0;
    public static final int MODE_LIGHT = 1;
    public static final int MODE_DARK = 2;

    private static final String PREF_NAME = "smartflash_theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    private ThemeManager() {
    }

    public static void applySavedTheme(Context context) {
        applyNightMode(getThemeMode(context));
    }

    public static void setThemeMode(Context context, int mode) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
        applyNightMode(mode);
    }

    public static int getThemeMode(Context context) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME_MODE, MODE_SYSTEM);
    }

    public static String getThemeLabel(Context context) {
        int mode = getThemeMode(context);
        if (mode == MODE_LIGHT) {
            return "Sáng";
        }
        if (mode == MODE_DARK) {
            return "Tối";
        }
        return "Mặc định hệ thống";
    }

    public static boolean isNightMode(Context context) {
        int uiMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return uiMode == Configuration.UI_MODE_NIGHT_YES;
    }

    public static void applyStatusBarColor(Window window, Context context, int colorResId) {
        if (window == null) {
            return;
        }
        boolean nightMode = isNightMode(context);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(context, colorResId));
        window.setNavigationBarColor(ContextCompat.getColor(context, com.elearning.smartflash.R.color.sf_bg_screen));
        androidx.core.view.WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(!nightMode);
        controller.setAppearanceLightNavigationBars(!nightMode);
    }

    private static void applyNightMode(int mode) {
        if (mode == MODE_LIGHT) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (mode == MODE_DARK) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}

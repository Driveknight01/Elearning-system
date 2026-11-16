package com.elearning.smartflash;

import android.app.Application;

import com.elearning.smartflash.data.local.prefs.TokenManager;
import com.elearning.smartflash.data.remote.api.RetrofitClient;

/**
 * Application entry point for SmartFlash Android Native client.
 */
public class SmartFlashApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        TokenManager.getInstance(this);
        RetrofitClient.getInstance(this);
    }
}

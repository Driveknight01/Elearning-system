package com.elearning.smartflash.util;

import android.os.Build;

/**
 * Provides concise device info for backend refresh token audit logs.
 */
public final class DeviceUtils {

    private DeviceUtils() {
    }

    public static String getDeviceInfo() {
        String manufacturer = Build.MANUFACTURER != null ? Build.MANUFACTURER : "Android";
        String model = Build.MODEL != null ? Build.MODEL : "Device";
        String version = Build.VERSION.RELEASE != null ? Build.VERSION.RELEASE : "";
        String info = (manufacturer + " " + model + " (Android " + version + ")").trim();
        return info.length() > 120 ? info.substring(0, 120) : info;
    }
}

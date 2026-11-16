package com.elearning.smartflash.util;

import com.elearning.smartflash.BuildConfig;

import java.net.URI;

/**
 * Presentation and URL helpers for user profiles, avatars, roles, and visibility levels.
 */
public final class ProfileUtils {

    private ProfileUtils() {
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Resolves relative upload paths or localhost URLs returned by the backend
     * against the host derived from {@link BuildConfig#BASE_URL} (e.g. {@code 10.0.2.2:8080} on emulator).
     */
    public static String resolveImageUrl(String rawUrl) {
        if (isBlank(rawUrl)) {
            return null;
        }
        String trimmed = rawUrl.trim();
        String serverOrigin = extractServerOrigin();
        if (trimmed.startsWith("/")) {
            return serverOrigin + trimmed;
        }
        if (trimmed.startsWith("http://localhost:") || trimmed.startsWith("http://127.0.0.1:")) {
            try {
                URI uri = URI.create(trimmed);
                String path = uri.getRawPath() != null ? uri.getRawPath() : "";
                String query = uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "";
                return serverOrigin + path + query;
            } catch (Exception ignored) {
                return trimmed;
            }
        }
        return trimmed;
    }

    private static String extractServerOrigin() {
        String base = BuildConfig.BASE_URL;
        try {
            URI uri = URI.create(base);
            int port = uri.getPort();
            String portPart = port != -1 ? ":" + port : "";
            return uri.getScheme() + "://" + uri.getHost() + portPart;
        } catch (Exception ex) {
            return "http://10.0.2.2:8080";
        }
    }

    public static String getInitials(String fullName, String username) {
        String source = !isBlank(fullName) ? fullName.trim()
                : (!isBlank(username) ? username.trim() : "SF");
        String[] parts = source.split("\\s+");
        if (parts.length >= 2 && !parts[0].isEmpty() && !parts[parts.length - 1].isEmpty()) {
            return (String.valueOf(parts[0].charAt(0))
                    + parts[parts.length - 1].charAt(0)).toUpperCase();
        }
        return source.length() >= 2
                ? source.substring(0, 2).toUpperCase()
                : source.toUpperCase();
    }

    public static String getRoleLabel(String role) {
        if (role == null) {
            return "Học viên";
        }
        switch (role) {
            case "TEACHER":
                return "Giáo viên";
            case "ADMIN":
                return "Quản trị viên";
            case "GUEST":
                return "Khách";
            default:
                return "Học viên";
        }
    }

    public static String getVisibilityLabel(String visibility) {
        if (visibility == null) {
            return "Công khai";
        }
        switch (visibility) {
            case "LOGGED_IN_ONLY":
                return "Chỉ thành viên";
            case "PRIVATE":
                return "Riêng tư";
            default:
                return "Công khai";
        }
    }
}

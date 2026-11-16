package com.elearning.smartflash.util;

import com.elearning.smartflash.data.remote.dto.ApiResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Extracts readable error messages from Spring Boot ApiResponse payloads or network exceptions.
 */
public final class ApiErrorParser {

    private static final Gson GSON = new Gson();
    private static final Type API_VOID_TYPE = new TypeToken<ApiResponse<Object>>() {}.getType();

    private ApiErrorParser() {
    }

    public static String parseResponseError(Response<?> response, String fallback) {
        if (response == null) {
            return fallback;
        }
        try (ResponseBody errorBody = response.errorBody()) {
            if (errorBody == null) {
                return fallback;
            }
            String raw = errorBody.string();
            if (raw == null || raw.trim().isEmpty()) {
                return fallback;
            }
            ApiResponse<Object> parsed = GSON.fromJson(raw, API_VOID_TYPE);
            if (parsed != null) {
                if (parsed.getErrors() instanceof Map<?, ?> map && !map.isEmpty()) {
                    Object firstVal = map.values().iterator().next();
                    if (firstVal != null && !firstVal.toString().trim().isEmpty()) {
                        return firstVal.toString();
                    }
                }
                if (parsed.getMessage() != null && !parsed.getMessage().trim().isEmpty()) {
                    return parsed.getMessage();
                }
            }
        } catch (Exception ignored) {
            // Fall back to default message
        }
        return fallback;
    }

    public static String parseThrowable(Throwable t, String fallback) {
        if (t instanceof IOException) {
            return "Không thể kết nối tới máy chủ. Vui lòng kiểm tra mạng hoặc Backend.";
        }
        if (t != null && t.getMessage() != null && !t.getMessage().trim().isEmpty()) {
            return t.getMessage();
        }
        return fallback;
    }
}

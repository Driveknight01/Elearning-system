package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Standard wrapper matching Spring Boot {@code ApiResponse<T>}.
 */
public class ApiResponse<T> {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("errorCode")
    private String errorCode;

    @SerializedName("data")
    private T data;

    @SerializedName("errors")
    private Object errors;

    @SerializedName("timestamp")
    private String timestamp;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public T getData() {
        return data;
    }

    public Object getErrors() {
        return errors;
    }

    public String getTimestamp() {
        return timestamp;
    }
}

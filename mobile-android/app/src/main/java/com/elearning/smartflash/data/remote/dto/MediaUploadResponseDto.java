package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Response matching Spring Boot {@code MediaUploadResponse}.
 */
public class MediaUploadResponseDto {

    @SerializedName("url")
    private String url;

    @SerializedName("filename")
    private String filename;

    @SerializedName("contentType")
    private String contentType;

    @SerializedName("size")
    private long size;

    public String getUrl() {
        return url;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }
}

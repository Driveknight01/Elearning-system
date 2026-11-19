package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Generic pagination response matching Spring Boot {@code PageResponse<T>}.
 */
public class PageResponseDto<T> {

    @SerializedName("content")
    private List<T> content;

    @SerializedName("page")
    private int page;

    @SerializedName("size")
    private int size;

    @SerializedName("totalElements")
    private long totalElements;

    @SerializedName("totalPages")
    private int totalPages;

    @SerializedName("first")
    private boolean first;

    @SerializedName("last")
    private boolean last;

    public List<T> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isFirst() {
        return first;
    }

    public boolean isLast() {
        return last;
    }
}

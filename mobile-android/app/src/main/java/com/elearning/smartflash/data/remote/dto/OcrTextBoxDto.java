package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Normalized (0.0 - 1.0) bounding box for an OCR-detected text region on the uploaded image.
 */
public class OcrTextBoxDto {

    @SerializedName("id")
    private String id;

    @SerializedName("text")
    private String text;

    @SerializedName("x")
    private Double x;

    @SerializedName("y")
    private Double y;

    @SerializedName("width")
    private Double width;

    @SerializedName("height")
    private Double height;

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public Double getX() {
        return x;
    }

    public Double getY() {
        return y;
    }

    public Double getWidth() {
        return width;
    }

    public Double getHeight() {
        return height;
    }
}

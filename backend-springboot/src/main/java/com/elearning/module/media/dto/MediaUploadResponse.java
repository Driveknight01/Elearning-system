package com.elearning.module.media.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response returned after uploading a flashcard illustration image.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadResponse {

    private String url;
    private String filename;
    private String contentType;
    private long size;
    private Long savedToCardId;
}

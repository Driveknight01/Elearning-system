package com.elearning.module.media.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single stock image item returned from Pixabay/Unsplash or fallback provider.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockImageItemDto {

    private String id;
    private String previewUrl;
    private String imageUrl;
    private Integer width;
    private Integer height;
    private String provider;
    private String attribution;
}

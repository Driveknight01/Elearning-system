package com.elearning.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Strongly-typed configuration properties for stock image search and AI mnemonic image generation ({@code app.media.*}).
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.media")
public class MediaProperties {

    private String pixabayApiKey = "";
    private String unsplashAccessKey = "";
    private String pollinationsBaseUrl = "https://image.pollinations.ai/prompt";
    private int timeoutMs = 8000;
}

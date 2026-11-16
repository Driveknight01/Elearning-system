package com.elearning.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Strongly-typed configuration properties for the Python FastAPI AI/ML microservice ({@code app.ai-service.*}).
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.ai-service")
public class AiServiceProperties {

    private String baseUrl = "http://localhost:8000";
    private int dktTimeoutMs = 1500;
    private int ocrTimeoutMs = 15000;
    private int chatTimeoutMs = 20000;
}

package com.elearning.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web MVC configuration for serving local uploaded files at {@code /uploads/**} and CORS defaults.
 */
@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.storage.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000,http://10.0.2.2:8080}")
    private String[] allowedOrigins;

    @PostConstruct
    public void initUploadDirectories() {
        Path rootPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootPath);
            Files.createDirectories(rootPath.resolve("avatars"));
            Files.createDirectories(rootPath.resolve("cards"));
            Files.createDirectories(rootPath.resolve("ocr"));
            log.info("Initialized local upload storage at: {}", rootPath);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not initialize local upload directory: " + rootPath, ex);
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path rootPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String location = rootPath.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

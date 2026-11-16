package com.elearning.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Strongly-typed Google OAuth2 configuration properties bound to {@code app.oauth2.*}.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.oauth2")
public class GoogleOAuth2Properties {

    public static final String PLACEHOLDER_CLIENT_ID = "your-google-client-id.apps.googleusercontent.com";

    /**
     * One or more comma-separated Google OAuth2 Client IDs accepted in the {@code aud} claim
     * (shared across Web React and Android Native).
     */
    private String googleClientId;

    private String jwksUri = "https://www.googleapis.com/oauth2/v3/certs";

    private long jwksCacheSeconds = 3600L;

    public Set<String> getAllowedClientIds() {
        if (!StringUtils.hasText(googleClientId)) {
            return Set.of();
        }
        return Arrays.stream(googleClientId.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isConfigured() {
        Set<String> clientIds = getAllowedClientIds();
        return !clientIds.isEmpty()
                && clientIds.stream().anyMatch(id -> !PLACEHOLDER_CLIENT_ID.equals(id));
    }
}

package com.elearning.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Strongly-typed JWT configuration properties bound to {@code app.jwt.*}.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private String secret;
    private long accessTokenExpirationMs = 86_400_000L;   // 24 hours
    private long refreshTokenExpirationMs = 604_800_000L; // 7 days
}

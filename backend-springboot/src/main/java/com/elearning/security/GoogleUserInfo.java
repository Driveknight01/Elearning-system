package com.elearning.security;

/**
 * Verified Google identity claims extracted from a Google ID Token.
 */
public record GoogleUserInfo(
        String subject,
        String email,
        boolean emailVerified,
        String fullName,
        String avatarUrl
) {
}

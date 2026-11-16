package com.elearning.security;

import com.elearning.common.exception.AppException;
import com.elearning.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleOAuth2VerifierTest {

    private static final String TEST_CLIENT_ID = "1234567890-abc.apps.googleusercontent.com";

    private KeyPair keyPair1;
    private KeyPair keyPair2;
    private GoogleOAuth2Properties properties;
    private AtomicReference<String> jwksPayload;
    private GoogleOAuth2Verifier verifier;

    @BeforeEach
    void setUp() {
        keyPair1 = Jwts.SIG.RS256.keyPair().build();
        keyPair2 = Jwts.SIG.RS256.keyPair().build();

        properties = new GoogleOAuth2Properties();
        properties.setGoogleClientId(TEST_CLIENT_ID);
        properties.setJwksCacheSeconds(3600L);

        jwksPayload = new AtomicReference<>(buildJwksJson("kid-1", (RSAPublicKey) keyPair1.getPublic()));
        verifier = new GoogleOAuth2Verifier(properties, new ObjectMapper(), jwksPayload::get);
    }

    @Test
    void verify_shouldReturnUserInfoWhenTokenIsValid() {
        String idToken = signGoogleToken(
                keyPair1,
                "kid-1",
                "https://accounts.google.com",
                TEST_CLIENT_ID,
                "google-sub-123",
                "Student@SmartFlash.vn",
                true,
                "Nguyen Van A",
                "https://lh3.googleusercontent.com/avatar.png",
                Instant.now().plus(10, ChronoUnit.MINUTES)
        );

        GoogleUserInfo userInfo = verifier.verify(idToken);

        assertThat(userInfo.subject()).isEqualTo("google-sub-123");
        assertThat(userInfo.email()).isEqualTo("student@smartflash.vn");
        assertThat(userInfo.emailVerified()).isTrue();
        assertThat(userInfo.fullName()).isEqualTo("Nguyen Van A");
        assertThat(userInfo.avatarUrl()).isEqualTo("https://lh3.googleusercontent.com/avatar.png");
    }

    @Test
    void verify_shouldRefreshJwksWhenGoogleRotatesKeyId() {
        // Warm cache with kid-1
        String firstToken = signGoogleToken(
                keyPair1, "kid-1", "accounts.google.com", TEST_CLIENT_ID,
                "sub-1", "a@smartflash.vn", true, "User A", null,
                Instant.now().plus(10, ChronoUnit.MINUTES)
        );
        assertThat(verifier.verify(firstToken).subject()).isEqualTo("sub-1");

        // Rotate Google JWKS to kid-2 and verify token signed by keyPair2
        jwksPayload.set(buildJwksJson("kid-2", (RSAPublicKey) keyPair2.getPublic()));
        String rotatedToken = signGoogleToken(
                keyPair2, "kid-2", "https://accounts.google.com", TEST_CLIENT_ID,
                "sub-2", "b@smartflash.vn", true, "User B", null,
                Instant.now().plus(10, ChronoUnit.MINUTES)
        );

        GoogleUserInfo userInfo = verifier.verify(rotatedToken);
        assertThat(userInfo.subject()).isEqualTo("sub-2");
        assertThat(userInfo.email()).isEqualTo("b@smartflash.vn");
    }

    @Test
    void verify_shouldThrowWhenAudienceDoesNotMatchClientId() {
        String idToken = signGoogleToken(
                keyPair1, "kid-1", "https://accounts.google.com",
                "other-app-client-id.apps.googleusercontent.com",
                "sub-1", "a@smartflash.vn", true, "User A", null,
                Instant.now().plus(10, ChronoUnit.MINUTES)
        );

        assertThatThrownBy(() -> verifier.verify(idToken))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    void verify_shouldThrowWhenIssuerIsInvalid() {
        String idToken = signGoogleToken(
                keyPair1, "kid-1", "https://evil.example.com", TEST_CLIENT_ID,
                "sub-1", "a@smartflash.vn", true, "User A", null,
                Instant.now().plus(10, ChronoUnit.MINUTES)
        );

        assertThatThrownBy(() -> verifier.verify(idToken))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    void verify_shouldThrowWhenEmailNotVerified() {
        String idToken = signGoogleToken(
                keyPair1, "kid-1", "https://accounts.google.com", TEST_CLIENT_ID,
                "sub-1", "a@smartflash.vn", false, "User A", null,
                Instant.now().plus(10, ChronoUnit.MINUTES)
        );

        assertThatThrownBy(() -> verifier.verify(idToken))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    void verify_shouldThrowWhenTokenIsExpired() {
        String idToken = signGoogleToken(
                keyPair1, "kid-1", "https://accounts.google.com", TEST_CLIENT_ID,
                "sub-1", "a@smartflash.vn", true, "User A", null,
                Instant.now().minus(5, ChronoUnit.MINUTES)
        );

        assertThatThrownBy(() -> verifier.verify(idToken))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    void verify_shouldThrowWhenSignatureDoesNotMatchKey() {
        // Signed with keyPair2 but claims kid-1 (which belongs to keyPair1)
        String tamperedToken = signGoogleToken(
                keyPair2, "kid-1", "https://accounts.google.com", TEST_CLIENT_ID,
                "sub-1", "a@smartflash.vn", true, "User A", null,
                Instant.now().plus(10, ChronoUnit.MINUTES)
        );

        assertThatThrownBy(() -> verifier.verify(tamperedToken))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    void verify_shouldThrowWhenGoogleClientIdNotConfigured() {
        properties.setGoogleClientId(GoogleOAuth2Properties.PLACEHOLDER_CLIENT_ID);

        assertThatThrownBy(() -> verifier.verify("dummy.jwt.token"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    private String signGoogleToken(
            KeyPair keyPair,
            String kid,
            String issuer,
            String audience,
            String subject,
            String email,
            boolean emailVerified,
            String name,
            String picture,
            Instant expiration
    ) {
        var builder = Jwts.builder()
                .header().keyId(kid).and()
                .issuer(issuer)
                .audience().add(audience).and()
                .subject(subject)
                .claim("email", email)
                .claim("email_verified", emailVerified)
                .claim("name", name)
                .issuedAt(Date.from(Instant.now().minus(1, ChronoUnit.MINUTES)))
                .expiration(Date.from(expiration));

        if (picture != null) {
            builder.claim("picture", picture);
        }

        return builder.signWith(keyPair.getPrivate(), Jwts.SIG.RS256).compact();
    }

    private String buildJwksJson(String kid, RSAPublicKey publicKey) {
        String n = encodeUnsignedBigInt(publicKey.getModulus());
        String e = encodeUnsignedBigInt(publicKey.getPublicExponent());
        return """
                {
                  "keys": [
                    {
                      "kty": "RSA",
                      "alg": "RS256",
                      "use": "sig",
                      "kid": "%s",
                      "n": "%s",
                      "e": "%s"
                    }
                  ]
                }
                """.formatted(kid, n, e);
    }

    private String encodeUnsignedBigInt(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] stripped = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, stripped, 0, stripped.length);
            bytes = stripped;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

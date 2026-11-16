package com.elearning.module.auth.controller;

import com.elearning.module.auth.dto.GoogleLoginRequest;
import com.elearning.module.auth.dto.RefreshTokenRequest;
import com.elearning.module.auth.dto.RegisterRequest;
import com.elearning.module.auth.entity.OAuthAccount;
import com.elearning.module.auth.entity.OAuthProvider;
import com.elearning.module.auth.entity.PlatformType;
import com.elearning.module.auth.repository.OAuthAccountRepository;
import com.elearning.module.auth.repository.RefreshTokenRepository;
import com.elearning.module.user.entity.Role;
import com.elearning.module.user.entity.User;
import com.elearning.module.user.entity.UserStreak;
import com.elearning.module.user.repository.UserRepository;
import com.elearning.module.user.repository.UserStreakRepository;
import com.elearning.security.GoogleOAuth2Properties;
import com.elearning.security.GoogleOAuth2Verifier;
import com.elearning.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.oauth2.google-client-id=" + AuthGoogleIntegrationTest.TEST_GOOGLE_CLIENT_ID)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthGoogleIntegrationTest {

    static final String TEST_GOOGLE_CLIENT_ID = "smartflash-test-client.apps.googleusercontent.com";
    private static final String TEST_KID = "test-google-kid-1";
    private static final KeyPair GOOGLE_KEY_PAIR = Jwts.SIG.RS256.keyPair().build();

    @TestConfiguration
    static class GoogleJwksTestConfig {

        @Bean
        @Primary
        GoogleOAuth2Verifier testGoogleOAuth2Verifier(
                GoogleOAuth2Properties properties,
                ObjectMapper objectMapper
        ) {
            String jwksJson = buildJwksJson(TEST_KID, (RSAPublicKey) GOOGLE_KEY_PAIR.getPublic());
            return GoogleOAuth2Verifier.withJwksSupplier(properties, objectMapper, () -> jwksJson);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OAuthAccountRepository oAuthAccountRepository;

    @Autowired
    private UserStreakRepository userStreakRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        oAuthAccountRepository.deleteAll();
        userStreakRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void googleLogin_shouldPersistUserOAuthAccountStreakAndTokensOnFirstLoginAndReuseOnSecondLogin() throws Exception {
        String idToken = createSignedGoogleIdToken(
                "google-sub-alice",
                "Alice@SmartFlash.vn",
                "Alice Nguyen",
                "https://lh3.googleusercontent.com/alice.png"
        );

        GoogleLoginRequest androidRequest = GoogleLoginRequest.builder()
                .idToken(idToken)
                .platform(PlatformType.ANDROID)
                .deviceInfo("Pixel 8 Pro")
                .fcmToken("fcm-token-alice")
                .build();

        MvcResult firstResult = mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(androidRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("alice@smartflash.vn"))
                .andExpect(jsonPath("$.data.user.username").value("alice"))
                .andExpect(jsonPath("$.data.user.role").value("USER"))
                .andReturn();

        JsonNode firstData = objectMapper.readTree(firstResult.getResponse().getContentAsString()).get("data");
        String accessToken = firstData.get("accessToken").asText();
        String refreshToken = firstData.get("refreshToken").asText();
        assertThat(jwtTokenProvider.validateToken(accessToken)).isTrue();

        User savedUser = userRepository.findByEmailAndDeletedAtIsNull("alice@smartflash.vn").orElseThrow();
        assertThat(savedUser.getPasswordHash()).isNull();
        assertThat(savedUser.getFullName()).isEqualTo("Alice Nguyen");
        assertThat(savedUser.getAvatarUrl()).isEqualTo("https://lh3.googleusercontent.com/alice.png");

        OAuthAccount oauthAccount = oAuthAccountRepository
                .findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "google-sub-alice")
                .orElseThrow();
        assertThat(oauthAccount.getUser().getId()).isEqualTo(savedUser.getId());

        UserStreak streak = userStreakRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(streak.getCurrentStreak()).isZero();
        assertThat(streak.getLongestStreak()).isZero();
        assertThat(streak.getFreezeCount()).isZero();
        assertThat(streak.getTotalXp()).isZero();
        assertThat(streak.getLastActiveDate()).isNull();

        // Second login from Web should reuse existing User, OAuthAccount, and UserStreak
        GoogleLoginRequest webRequest = GoogleLoginRequest.builder()
                .idToken(idToken)
                .platform(PlatformType.WEB)
                .deviceInfo("Chrome Desktop")
                .build();

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.id").value(savedUser.getId()));

        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(oAuthAccountRepository.count()).isEqualTo(1);
        assertThat(userStreakRepository.count()).isEqualTo(1);
        assertThat(refreshTokenRepository.count()).isEqualTo(2);

        // Issued refresh token must rotate cleanly via /api/v1/auth/refresh
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RefreshTokenRequest(refreshToken, null, null, null)
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    void googleLogin_shouldLinkExistingEmailAccountAndHandleUsernameCollisions() throws Exception {
        // 1. Register an existing TEACHER account via Email/Password
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("shared_name")
                .email("teacher@smartflash.vn")
                .password("Password123")
                .fullName("Teacher Bob")
                .role(Role.TEACHER)
                .platform(PlatformType.WEB)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // 2. Same email signs in via Google -> links OAuthAccount without overwriting role/user
        String teacherGoogleToken = createSignedGoogleIdToken(
                "google-sub-teacher",
                "teacher@smartflash.vn",
                "Teacher Bob",
                "https://lh3.googleusercontent.com/bob.png"
        );

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                GoogleLoginRequest.builder().idToken(teacherGoogleToken).build()
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.username").value("shared_name"))
                .andExpect(jsonPath("$.data.user.role").value("TEACHER"));

        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(oAuthAccountRepository.count()).isEqualTo(1);
        assertThat(userStreakRepository.count()).isEqualTo(1);

        // 3. New Google user whose email local-part collides with existing username "shared_name"
        String collidingGoogleToken = createSignedGoogleIdToken(
                "google-sub-colliding",
                "shared_name@gmail.com",
                "Another User",
                null
        );

        MvcResult collisionResult = mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                GoogleLoginRequest.builder().idToken(collidingGoogleToken).build()
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email").value("shared_name@gmail.com"))
                .andReturn();

        String generatedUsername = objectMapper.readTree(collisionResult.getResponse().getContentAsString())
                .path("data").path("user").path("username").asText();
        assertThat(generatedUsername).startsWith("shared_name_");
        assertThat(userRepository.count()).isEqualTo(2);
        assertThat(userStreakRepository.count()).isEqualTo(2);
    }

    @Test
    void googleLogin_shouldReturnStandardErrorResponsesForBlankOrInvalidToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("REQ_400_VALIDATION"));

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"not-a-valid-google-jwt\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AUTH_401_TOKEN"));
    }

    private String createSignedGoogleIdToken(String subject, String email, String name, String picture) {
        var builder = Jwts.builder()
                .header().keyId(TEST_KID).and()
                .issuer("https://accounts.google.com")
                .audience().add(TEST_GOOGLE_CLIENT_ID).and()
                .subject(subject)
                .claim("email", email)
                .claim("email_verified", true)
                .claim("name", name)
                .issuedAt(Date.from(Instant.now().minus(1, ChronoUnit.MINUTES)))
                .expiration(Date.from(Instant.now().plus(15, ChronoUnit.MINUTES)));

        if (picture != null) {
            builder.claim("picture", picture);
        }

        return builder.signWith(GOOGLE_KEY_PAIR.getPrivate(), Jwts.SIG.RS256).compact();
    }

    private static String buildJwksJson(String kid, RSAPublicKey publicKey) {
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

    private static String encodeUnsignedBigInt(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] stripped = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, stripped, 0, stripped.length);
            bytes = stripped;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

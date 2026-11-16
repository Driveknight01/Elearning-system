package com.elearning.module.user.controller;

import com.elearning.module.auth.repository.RefreshTokenRepository;
import com.elearning.module.user.dto.ChangePasswordRequest;
import com.elearning.module.user.dto.UpdatePrivacyRequest;
import com.elearning.module.user.dto.UpdateProfileRequest;
import com.elearning.module.user.entity.ProfilePrivacyConfig;
import com.elearning.module.user.entity.ProfileVisibility;
import com.elearning.module.user.entity.Role;
import com.elearning.module.user.entity.User;
import com.elearning.module.user.entity.UserStatus;
import com.elearning.module.user.entity.UserStreak;
import com.elearning.module.user.repository.UserRepository;
import com.elearning.module.user.repository.UserStreakRepository;
import com.elearning.security.JwtTokenProvider;
import com.elearning.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserProfileIntegrationTest {

    @TempDir
    static Path tempUploadDir;

    @DynamicPropertySource
    static void registerStorageProperties(DynamicPropertyRegistry registry) {
        registry.add("app.storage.upload-dir", () -> tempUploadDir.toAbsolutePath().toString());
        registry.add("app.storage.base-url", () -> "http://localhost:8080/uploads");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserStreakRepository userStreakRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User alice;
    private User bob;
    private User admin;
    private String aliceJwt;
    private String bobJwt;
    private String adminJwt;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userStreakRepository.deleteAll();
        userRepository.deleteAll();

        alice = userRepository.save(User.builder()
                .username("alice_learner")
                .email("alice@smartflash.vn")
                .passwordHash(passwordEncoder.encode("Secret123"))
                .fullName("Alice Nguyen")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build());
        userStreakRepository.save(UserStreak.builder()
                .user(alice)
                .currentStreak(5)
                .longestStreak(10)
                .totalXp(250L)
                .build());

        bob = userRepository.save(User.builder()
                .username("bob_google")
                .email("bob@smartflash.vn")
                .passwordHash(null) // Google-only account
                .fullName("Bob Tran")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build());

        admin = userRepository.save(User.builder()
                .username("sys_admin")
                .email("admin@smartflash.vn")
                .passwordHash(passwordEncoder.encode("Admin123"))
                .fullName("System Admin")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build());

        aliceJwt = jwtTokenProvider.generateAccessToken(UserPrincipal.from(alice));
        bobJwt = jwtTokenProvider.generateAccessToken(UserPrincipal.from(bob));
        adminJwt = jwtTokenProvider.generateAccessToken(UserPrincipal.from(admin));
    }

    @Test
    void getAndUpdateMyProfile_shouldPersistChangesAndRejectDuplicateUsername() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("alice_learner"))
                .andExpect(jsonPath("$.data.hasPassword").value(true))
                .andExpect(jsonPath("$.data.currentStreak").value(5))
                .andExpect(jsonPath("$.data.totalXp").value(250));

        UpdateProfileRequest updateRequest = UpdateProfileRequest.builder()
                .username("alice_updated")
                .fullName("Alice Updated")
                .bio("Learning IELTS 8.0")
                .dailyGoalCards(35)
                .timezone("Asia/Ho_Chi_Minh")
                .build();

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("alice_updated"))
                .andExpect(jsonPath("$.data.fullName").value("Alice Updated"))
                .andExpect(jsonPath("$.data.bio").value("Learning IELTS 8.0"))
                .andExpect(jsonPath("$.data.dailyGoalCards").value(35));

        // Duplicate username owned by Bob must fail with 409
        UpdateProfileRequest duplicateRequest = UpdateProfileRequest.builder()
                .username("bob_google")
                .fullName("Alice")
                .build();

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USER_409_USERNAME"));
    }

    @Test
    void uploadAvatar_shouldStoreImageAndRejectUnsupportedFileType() throws Exception {
        MockMultipartFile validImage = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[]{1, 2, 3, 4}
        );

        mockMvc.perform(multipart("/api/v1/users/me/avatar")
                        .file(validImage)
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avatarUrl").isNotEmpty());

        User reloaded = userRepository.findById(alice.getId()).orElseThrow();
        assertThat(reloaded.getAvatarUrl()).startsWith("http://localhost:8080/uploads/avatars/");

        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "malicious.txt",
                "text/plain",
                "hello".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/users/me/avatar")
                        .file(invalidFile)
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("REQ_400"));
    }

    @Test
    void changePassword_shouldVerifyCurrentPasswordAndAllowGoogleUserToSetFirstPassword() throws Exception {
        // Wrong current password
        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("WrongPass", "NewSecret123")
                        )))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_401_CREDENTIALS"));

        // Correct current password
        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("Secret123", "NewSecret123")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        User updatedAlice = userRepository.findById(alice.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("NewSecret123", updatedAlice.getPasswordHash())).isTrue();

        // Google-only user (Bob) can set initial password without currentPassword
        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + bobJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest(null, "GoogleUserPass123")
                        )))
                .andExpect(status().isOk());

        User updatedBob = userRepository.findById(bob.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("GoogleUserPass123", updatedBob.getPasswordHash())).isTrue();
    }

    @Test
    void updatePrivacyAndGetPublicProfile_shouldEnforceVisibilityAndTabFilters() throws Exception {
        // 1. PUBLIC with folders & classes tabs hidden
        UpdatePrivacyRequest partialPrivacy = UpdatePrivacyRequest.builder()
                .profileVisibility(ProfileVisibility.PUBLIC)
                .profilePrivacyConfig(ProfilePrivacyConfig.builder()
                        .showSets(true)
                        .showFolders(false)
                        .showClasses(false)
                        .showAchievements(true)
                        .build())
                .build();

        mockMvc.perform(put("/api/v1/users/me/privacy")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialPrivacy)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profilePrivacyConfig.showFolders").value(false));

        // Guest sees sets & achievements, but folders & classes are null
        mockMvc.perform(get("/api/v1/users/profile/alice_learner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.owner").value(false))
                .andExpect(jsonPath("$.data.visibleTabs.showSets").value(true))
                .andExpect(jsonPath("$.data.visibleTabs.showFolders").value(false))
                .andExpect(jsonPath("$.data.sets").isArray())
                .andExpect(jsonPath("$.data.achievements").isArray())
                .andExpect(jsonPath("$.data.folders").value(nullValue()))
                .andExpect(jsonPath("$.data.classes").value(nullValue()));

        // Owner still sees all 4 tabs
        mockMvc.perform(get("/api/v1/users/profile/alice_learner")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.owner").value(true))
                .andExpect(jsonPath("$.data.folders").isArray())
                .andExpect(jsonPath("$.data.classes").isArray());

        // 2. LOGGED_IN_ONLY blocks Guest (401) but allows Bob (200)
        UpdatePrivacyRequest loggedInOnly = UpdatePrivacyRequest.builder()
                .profileVisibility(ProfileVisibility.LOGGED_IN_ONLY)
                .build();

        mockMvc.perform(put("/api/v1/users/me/privacy")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loggedInOnly)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/profile/alice_learner"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_401"));

        mockMvc.perform(get("/api/v1/users/profile/alice_learner")
                        .header("Authorization", "Bearer " + bobJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("alice_learner"));

        // 3. PRIVATE blocks Guest & Bob (403) but allows Owner & ADMIN (200)
        UpdatePrivacyRequest privatePrivacy = UpdatePrivacyRequest.builder()
                .profileVisibility(ProfileVisibility.PRIVATE)
                .build();

        mockMvc.perform(put("/api/v1/users/me/privacy")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(privatePrivacy)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/profile/alice_learner")
                        .header("Authorization", "Bearer " + bobJwt))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AUTH_403"));

        mockMvc.perform(get("/api/v1/users/profile/alice_learner")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("alice_learner"));
    }
}

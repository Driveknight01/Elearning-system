# CLAUDE.md / PROJECT_STATE.md — Trạng thái Dự án & Nhật ký Phát triển

> **LƯU Ý CHO AI AGENT**: Hãy đọc [AGENTS.md](file:///D:/Elearning-system/AGENTS.md) trước khi viết bất kỳ dòng code nào. Sau mỗi task hoàn thành trong [FEATURE.md](file:///D:/Elearning-system/FEATURE.md), bạn **BẮT BUỘC** phải cập nhật mục **Trạng thái hiện tại**, **CHANGELOG** trong file này (đồng bộ với [PROJECT_STATE.md](file:///D:/Elearning-system/PROJECT_STATE.md)), **hướng dẫn chi tiết cách chạy test/kiểm chứng trong IDE** (IntelliJ IDEA / Android Studio / VS Code) cho người dùng, và nhắc người dùng chuyển đổi mô hình AI (`🟢 Flash` / `🟡 Medium` / `🔴 Pro`) cho task kế tiếp. **ĐẶC BIỆT**: Mọi quyết định thay đổi kiến trúc/công nghệ/CSDL đều **BẮT BUỘC** phải giải thích, phân tích ưu-nhược điểm và hỏi ý kiến người dùng, tuyệt đối không tự ý quyết định.

---

## 1. Bản đồ Tài liệu Dự án (Project Documentation Map)
- 📐 **Thiết kế Hệ thống & Kiến trúc tổng thể**: [system_design.md](file:///D:/Elearning-system/system_design.md)
- 🗄️ **Thiết kế CSDL PostgreSQL chi tiết (41 bảng / 8 phân hệ)**: [docs/DATABASE.md](file:///D:/Elearning-system/docs/DATABASE.md)
- 📋 **Danh sách Task chi tiết (8 Phases — 66 Tasks — 1 Task = 1 Git Commit)**: [FEATURE.md](file:///D:/Elearning-system/FEATURE.md)
- ⚖️ **Luật Code, Quy ước Đặt tên & Quy tắc Git/Test**: [AGENTS.md](file:///D:/Elearning-system/AGENTS.md)

---

## 2. Trạng thái Hiện tại (Current Project State)

### ✅ Đã hoàn thành (Completed)
- **Phân tích & Thiết kế Hệ thống ([system_design.md](file:///D:/Elearning-system/system_design.md))**:
  - Chốt kiến trúc **Hybrid Modular Monolith (Spring Boot 3.x) + AI/ML Microservice (Python FastAPI)** chạy trực tiếp trên môi trường máy thật (**PostgreSQL Native + IntelliJ IDEA + Android Studio**, không cần chạy Docker).
  - Phân tích đầy đủ 4 vai trò (`Guest`, `User/Learner`, `Teacher`, `Admin`) và 8 Module chức năng (FSRS-4.5, DKT, OCR, AI Chatbot forward bộ thẻ sang CRUD, Minigames chuẩn Quizlet: *Ghép thẻ, Blast, Đá quý, Khối hộp, Live Arena*, công cụ Giáo viên *Chỉ định hoạt động không cần đăng nhập*, và **Admin Suite 7 nhóm chức năng chuẩn Quizlet**).
- **Thiết kế & Rà soát CSDL Khép kín ([docs/DATABASE.md](file:///D:/Elearning-system/docs/DATABASE.md))**:
  - Hoàn thiện đặc tả **41 bảng PostgreSQL** chia theo 8 phân hệ, đảm bảo không phải phát sinh thêm bảng về sau (bao gồm cả `verification_tokens` cho OTP/Quên mật khẩu, `user_set_interactions` cho *Bộ thẻ Đã lưu + Học gần đây Recents + Đánh giá 1–5★ + Đồng bộ vị trí học dở giữa Web & Android*, `dkt_knowledge_states` & `ai_model_versions` cho mạng LSTM DKT, `class_folders`, và 8 bảng Admin Suite).
- **Thiết lập Quy chuẩn & Phân rã Task nhỏ theo chuẩn Git Commit ([AGENTS.md](file:///D:/Elearning-system/AGENTS.md), [FEATURE.md](file:///D:/Elearning-system/FEATURE.md))**:
  - Chia nhỏ toàn bộ dự án thành **66 Task nguyên tử (`T1.1` → `T8.9`)**, mỗi task kèm gợi ý câu lệnh `git commit` và nhãn độ khó (`🟢 [DỄ — Flash]`, `🟡 [VỪA — Medium]`, `🔴 [KHÓ — ⚠️ Đề xuất Model Mạnh/Pro]`).
- **T1.1 & T1.2 — Khởi tạo Monorepo & Khung `backend-springboot`**:
  - Tạo cấu trúc thư mục monorepo (`backend-springboot`, `ai-ml-service`, `frontend-web`, `mobile-android`, `uploads`), [`.gitignore`](file:///D:/Elearning-system/.gitignore) đa nền tảng và script tạo CSDL PostgreSQL [scripts/init_database.sql](file:///D:/Elearning-system/scripts/init_database.sql).
  - Khởi tạo khung Spring Boot 3.3.5 ([backend-springboot/pom.xml](file:///D:/Elearning-system/backend-springboot/pom.xml), [application.yml](file:///D:/Elearning-system/backend-springboot/src/main/resources/application.yml), [SmartFlashApplication.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/SmartFlashApplication.java)) và các package theo module nghiệp vụ.
- **T1.3, T1.4, T1.5 — Hoàn thành 3 File Flyway Migration PostgreSQL (`V1`, `V2`, `V3`)**:
  - Tạo [V1__init_core_users_and_cards.sql](file:///D:/Elearning-system/backend-springboot/src/main/resources/db/migration/V1__init_core_users_and_cards.sql) (11 bảng: Danh mục, Người dùng, Auth, Bộ thẻ, Thư mục).
  - Tạo [V2__init_study_and_activities.sql](file:///D:/Elearning-system/backend-springboot/src/main/resources/db/migration/V2__init_study_and_activities.sql) (14 bảng: FSRS/DKT, Kiểm tra, Minigames, Live Rooms, Lớp học & Hoạt động Giáo viên).
  - Tạo [V3__init_gamification_chat_admin_and_indexes.sql](file:///D:/Elearning-system/backend-springboot/src/main/resources/db/migration/V3__init_gamification_chat_admin_and_indexes.sql) (17 bảng: Nhóm học, Streak/Thành tựu, AI Chat, 8 bảng Admin Suite & toàn bộ Performance Indexes).
- **T1.6 — Hoàn thành gói `common` & Cấu hình Local Storage `/uploads/**` (`backend-springboot`)**:
  - Tạo [BaseEntity.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/common/entity/BaseEntity.java), [ApiResponse.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/common/dto/ApiResponse.java), [PageResponse.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/common/dto/PageResponse.java), [ErrorCode.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/common/exception/ErrorCode.java), [AppException.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/common/exception/AppException.java), [GlobalExceptionHandler.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/common/exception/GlobalExceptionHandler.java) và [WebMvcConfig.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/config/WebMvcConfig.java).
- **T1.7 — Hoàn thành Khung Microservice `ai-ml-service` (Python FastAPI)**:
  - Tạo [requirements.txt](file:///D:/Elearning-system/ai-ml-service/requirements.txt), [.env.example](file:///D:/Elearning-system/ai-ml-service/.env.example), [main.py](file:///D:/Elearning-system/ai-ml-service/main.py), [app/core/config.py](file:///D:/Elearning-system/ai-ml-service/app/core/config.py), các module router [app/dkt/router.py](file:///D:/Elearning-system/ai-ml-service/app/dkt/router.py), [app/ocr/router.py](file:///D:/Elearning-system/ai-ml-service/app/ocr/router.py), [app/chatbot/router.py](file:///D:/Elearning-system/ai-ml-service/app/chatbot/router.py) và bộ test [test_main.py](file:///D:/Elearning-system/ai-ml-service/test_main.py).
- **T1.8 — Hoàn thành Khung `frontend-web` (React + Vite + TailwindCSS)**:
  - Cài đặt đầy đủ 163 packages (`npm install`) và kiểm chứng `npm run build` thành công (`built in 3.75s`).
- **T1.9 — Hoàn thành Khung `mobile-android` (Android Native Java + MVVM)**:
  - Tạo [settings.gradle](file:///D:/Elearning-system/mobile-android/settings.gradle), [build.gradle](file:///D:/Elearning-system/mobile-android/build.gradle), [app/build.gradle](file:///D:/Elearning-system/mobile-android/app/build.gradle) (Material 3, Retrofit2, Gson, Glide, RoomDB, WorkManager, CameraX, ML Kit Text Recognition), [AndroidManifest.xml](file:///D:/Elearning-system/mobile-android/app/src/main/AndroidManifest.xml), [SmartFlashApp.java](file:///D:/Elearning-system/mobile-android/app/src/main/java/com/elearning/smartflash/SmartFlashApp.java), [MainActivity.java](file:///D:/Elearning-system/mobile-android/app/src/main/java/com/elearning/smartflash/ui/main/MainActivity.java), [activity_main.xml](file:///D:/Elearning-system/mobile-android/app/src/main/res/layout/activity_main.xml) và cấu trúc package MVVM (`data/`, `domain/`, `ui/`, `worker/`, `util/`).
- **T2.1 — Hoàn thành Auth/User JPA Entities, Enums & Repositories (`backend-springboot`)**:
  - Tạo Enums [Role.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/module/user/entity/Role.java), [UserStatus.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/module/user/entity/UserStatus.java), [ProfileVisibility.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/module/user/entity/ProfileVisibility.java), [OAuthProvider.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/module/auth/entity/OAuthProvider.java), [PlatformType.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/module/auth/entity/PlatformType.java), [TokenType.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/module/auth/entity/TokenType.java).
  - Tạo Entities [User.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/module/user/entity/User.java), [ProfilePrivacyConfig.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/module/user/entity/ProfilePrivacyConfig.java), [OAuthAccount.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/module/auth/entity/OAuthAccount.java), [RefreshToken.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/module/auth/entity/RefreshToken.java), [VerificationToken.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/module/auth/entity/VerificationToken.java) và 4 Repositories (`UserRepository`, `OAuthAccountRepository`, `RefreshTokenRepository`, `VerificationTokenRepository`).
- **T2.2 — Hoàn thành Spring Security 6, JWT Provider & RBAC Filter (`backend-springboot`)**:
  - Tạo [JwtProperties.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/security/JwtProperties.java), [UserPrincipal.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/security/UserPrincipal.java), [CustomUserDetailsService.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/security/CustomUserDetailsService.java), [JwtTokenProvider.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/security/JwtTokenProvider.java), [JwtAuthenticationFilter.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/security/JwtAuthenticationFilter.java), [JwtAuthenticationEntryPoint.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/security/JwtAuthenticationEntryPoint.java), [CustomAccessDeniedHandler.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/security/CustomAccessDeniedHandler.java), [SecurityConfig.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/security/SecurityConfig.java) và unit test [JwtTokenProviderTest.java](file:///D:/Elearning-system/backend-springboot/src/test/java/com/elearning/security/JwtTokenProviderTest.java).
- **T2.3 — Hoàn thành API Auth: Đăng ký, Đăng nhập, OTP Quên mật khẩu, Refresh Token & Đăng xuất (`backend-springboot`)**:
  - Tạo 8 DTOs trong `com.elearning.module.auth.dto` (`RegisterRequest`, `LoginRequest`, `ForgotPasswordRequest`, `ResetPasswordRequest`, `RefreshTokenRequest`, `LogoutRequest`, `AuthResponse`, `UserSummaryResponse`).
  - Tạo [OtpMailService.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/module/auth/service/OtpMailService.java), [AuthService.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/module/auth/service/AuthService.java), [AuthController.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/module/auth/controller/AuthController.java) và [AuthServiceTest.java](file:///D:/Elearning-system/backend-springboot/src/test/java/com/elearning/module/auth/service/AuthServiceTest.java).
- **T2.4 — Hoàn thành Xác thực Google OAuth2 ID Token dùng chung cho Web & Android (`backend-springboot`)**:
  - Tạo [GoogleOAuth2Properties.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/security/GoogleOAuth2Properties.java), [GoogleUserInfo.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/security/GoogleUserInfo.java), [GoogleOAuth2Verifier.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/security/GoogleOAuth2Verifier.java) (xác thực RS256 qua Google JWKS bằng JJWT 0.12.6 + `RestClient`, không phát sinh thêm dependency trong `pom.xml`).
  - Tạo [UserStreak.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/module/user/entity/UserStreak.java), [UserStreakRepository.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/module/user/repository/UserStreakRepository.java) và [GoogleLoginRequest.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/module/auth/dto/GoogleLoginRequest.java).
  - Cập nhật [AuthService.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/module/auth/service/AuthService.java) & [AuthController.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/module/auth/controller/AuthController.java) với API `POST /api/v1/auth/google` (tự động tạo `users` + `oauth_accounts` + khởi tạo `user_streaks` ở lần đầu đăng nhập, hoặc liên kết tài khoản trùng email) cùng bộ test [GoogleOAuth2VerifierTest.java](file:///D:/Elearning-system/backend-springboot/src/test/java/com/elearning/security/GoogleOAuth2VerifierTest.java), [AuthServiceTest.java](file:///D:/Elearning-system/backend-springboot/src/test/java/com/elearning/module/auth/service/AuthServiceTest.java) và [AuthGoogleIntegrationTest.java](file:///D:/Elearning-system/backend-springboot/src/test/java/com/elearning/module/auth/controller/AuthGoogleIntegrationTest.java).
- **T2.5 — Hoàn thành API Hồ sơ cá nhân & Quyền xem Trang cá nhân (`backend-springboot`)**:
  - Tạo [LocalStorageService.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/common/service/LocalStorageService.java) lưu ảnh cục bộ `./uploads/avatars/` (validate loại ảnh & dung lượng <= 5MB).
  - Tạo 6 DTOs trong `com.elearning.module.user.dto` (`MyProfileResponse`, `UpdateProfileRequest`, `ChangePasswordRequest`, `UpdatePrivacyRequest`, `AvatarUploadResponse`, `PublicProfileResponse`), [ProfileTabDataLoader.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/module/user/service/ProfileTabDataLoader.java), [UserService.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/module/user/service/UserService.java), [UserController.java](file:///D:/Elearning-system/backend-springboot/src/main/java/com/elearning/module/user/controller/UserController.java) và [UserProfileIntegrationTest.java](file:///D:/Elearning-system/backend-springboot/src/test/java/com/elearning/module/user/controller/UserProfileIntegrationTest.java).
  - Hỗ trợ `GET/PUT /api/v1/users/me`, `POST /api/v1/users/me/avatar`, `PUT /api/v1/users/me/password`, `PUT /api/v1/users/me/privacy` (`profile_visibility` + `profile_privacy_config` JSONB) và `GET /api/v1/users/profile/{username}` lọc dữ liệu 4 tab (*Bộ thẻ, Thư mục, Lớp học, Thành tựu*) theo đúng quyền riêng tư.

### 🔄 Đang thực hiện (In Progress)
- Đang triển khai **Phase 2: Xác thực (JWT + Google OAuth2), RBAC 4 Vai trò & Quyền riêng tư Trang cá nhân** trên nhánh `feat/phase-2-auth` (đã xong phần Backend `T2.1` → `T2.5`).

### ⏭️ Kế tiếp (Next Up)
- [ ] **T2.6** 🟡 `[VỪA — Medium]`: [Web] Xây dựng `LoginPage`, `RegisterPage` (tích hợp nút *Continue with Google*), `ProtectedRoute` theo 4 vai trò và lưu trạng thái đăng nhập vào `useAuthStore`.

---

## 3. CHANGELOG (Nhật ký theo từng buổi làm việc)

### [2026-11-16] — Buổi 4 (Cập nhật lần 4): Hoàn thành T2.5 (User Profile & Granular Tab Privacy APIs)
- Hoàn thành **T2.5** trên nhánh `feat/phase-2-auth`: Triển khai `UserController` (`/api/v1/users/me`, `/me/avatar`, `/me/password`, `/me/privacy`, `/profile/{username}`), `UserService`, `LocalStorageService`, `ProfileTabDataLoader`, `UserStreak` (`Persistable<Long>` tránh lỗi detached `User` khi lưu) và `UserProfileIntegrationTest`. Đã kiểm tra biên dịch `mvn test-compile` thành công.

### [2026-11-16] — Buổi 4 (Cập nhật lần 3): Hoàn thành T2.4 (Unified Google OAuth2 ID Token Verification)
- Hoàn thành **T2.4** trên nhánh `feat/phase-2-auth`: Triển khai `GoogleOAuth2Verifier` xác thực Google `idToken` (RS256 qua `https://www.googleapis.com/oauth2/v3/certs` với cache JWKS & auto-refresh khi xoay khóa), `UserStreak` entity/repository, và endpoint `POST /api/v1/auth/google` dùng chung cho Web React & Android Native (tự động tạo `users`, `oauth_accounts`, `user_streaks` ở lần đầu đăng nhập hoặc liên kết email đã có). Đã kiểm tra biên dịch `mvn test-compile` thành công.

### [2026-11-16] — Buổi 4 (Cập nhật lần 2): Hoàn thành T2.3 (Auth APIs: Register, Login, OTP Reset, Refresh, Logout)
- Hoàn thành **T2.3** trên nhánh `feat/phase-2-auth`: Triển khai `AuthController` (`/api/v1/auth/register`, `/login`, `/forgot-password`, `/reset-password`, `/refresh`, `/logout`), `AuthService` (BCrypt, SHA-256 hash refresh token + token rotation, OTP 6 số 15 phút), `OtpMailService` (có fallback log console khi chưa cấu hình SMTP) và `AuthServiceTest`.

### [2026-11-16] — Buổi 4 (Cập nhật): Hoàn thành T2.2 (Spring Security 6 & JWT Authentication)
- Hoàn thành **T2.2** trên nhánh `feat/phase-2-auth`: Cấu hình `SecurityConfig` (Stateless JWT, CORS, BCrypt, public Guest endpoints, `/api/v1/admin/**` chỉ cho `ADMIN`), `JwtTokenProvider` (JJWT 0.12.6), `JwtAuthenticationFilter`, `CustomUserDetailsService`, handler 401/403 chuẩn `ApiResponse` và `JwtTokenProviderTest` (pass 7/7 tests).

### [2026-11-16] — Buổi 4: Hoàn thành T2.1 (Auth & User Entities, Enums & Repositories)
- Hoàn thành **T2.1** trên nhánh `feat/phase-2-auth`: Khởi tạo đầy đủ JPA Entities (`User`, `ProfilePrivacyConfig` JSONB, `OAuthAccount`, `RefreshToken`, `VerificationToken`), Enums (`Role`, `UserStatus`, `ProfileVisibility`, `OAuthProvider`, `PlatformType`, `TokenType`) và 4 Spring Data JPA Repositories khớp schema PostgreSQL `V1__init_core_users_and_cards.sql`.

### [2026-11-16] — Buổi 3 (Cập nhật): Hoàn thành T1.9 & Kết thúc Phase 1
- Hoàn thành **T1.9**: Khởi tạo khung project Android Native Java (`mobile-android`) với Gradle 8.7 / AGP 8.5.2, Material 3, Retrofit2, Gson, Glide, RoomDB, WorkManager, CameraX + ML Kit OCR, `MainActivity` (ViewBinding) và đầy đủ package MVVM.

### [2026-11-16] — Buổi 2 (Cập nhật): Hoàn thành T1.8 (Khởi tạo & Build thành công `frontend-web`)
- Cài đặt dependencies `frontend-web` (`npm install`) và verify `vite build` thành công (`dist/` built in 3.75s).

### [2026-11-16] — Buổi 3: Hoàn thành T1.6 & T1.7 (Gói `common` Spring Boot & Khung FastAPI Microservice)
- Hoàn thành **T1.6**: Triển khai `BaseEntity`, `ApiResponse<T>`, `PageResponse<T>`, `ErrorCode`, `AppException`, `GlobalExceptionHandler`, `WebMvcConfig` phục vụ file tĩnh `/uploads/**` và unit test `GlobalExceptionHandlerTest`.
- Hoàn thành **T1.7**: Khởi tạo khung microservice `ai-ml-service` với Python FastAPI, `requirements.txt`, cấu trúc module `app/dkt/`, `app/ocr/`, `app/chatbot/`, `app/core/config.py`, endpoint `/health`, CORS cho Spring Boot/Web và `test_main.py`.

### [2026-11-16] — Buổi 2 (Cập nhật): Hoàn thành T1.3, T1.4, T1.5 (Flyway Migration 42 Bảng PostgreSQL)
- Tạo 3 file Flyway SQL (`V1`, `V2`, `V3`) trong `backend-springboot/src/main/resources/db/migration/` khởi tạo đầy đủ 42 bảng, khóa ngoại (`fk_*`), ràng buộc duy nhất và toàn bộ `CREATE INDEX` (`GIN`, B-Tree kép cho FSRS/DKT, Leaderboard, Moderation).

### [2026-11-16] — Buổi 2: Hoàn thành T1.1 & T1.2 (Khởi tạo Monorepo, Script PostgreSQL & Khung Spring Boot 3.x)
- Hoàn thành **T1.1**: Khởi tạo Git repository, cấu trúc 4 module chính, file [`.gitignore`](file:///D:/Elearning-system/.gitignore) đa nền tảng và script [scripts/init_database.sql](file:///D:/Elearning-system/scripts/init_database.sql) tạo `smartflash_db` kèm extension `pgcrypto`, `pg_trgm`, `unaccent`.
- Hoàn thành **T1.2**: Tạo [backend-springboot/pom.xml](file:///D:/Elearning-system/backend-springboot/pom.xml) (Spring Boot 3.3.5, Web, Data JPA, Security, Validation, WebSocket STOMP, Caffeine Cache, Flyway PostgreSQL, JJWT, SpringDoc OpenAPI, Lombok), [application.yml](file:///D:/Elearning-system/backend-springboot/src/main/resources/application.yml) kết nối PostgreSQL local và cấu trúc package `com.elearning.*`.

### [2026-11-16] — Buổi 1 (Cập nhật lần 4): Rà soát Khép kín Logic CSDL (41 Bảng Chuẩn Quizlet & AI/ML)
- Rà soát toàn diện các luồng nghiệp vụ dễ phát sinh bảng khi code FE/BE và bổ sung khép kín vào [docs/DATABASE.md](file:///D:/Elearning-system/docs/DATABASE.md):
  1. `verification_tokens` (mã OTP xác thực email & quên mật khẩu) + cột `fcm_token`, `platform` trong `refresh_tokens` (cho Android Push Notification).
  2. `user_set_interactions` (hợp nhất 4 tính năng Quizlet: *Bộ thẻ Đã lưu*, *Lịch sử Học gần đây Recents*, *Đánh giá 1–5★*, và *Resume State JSONB* giúp học dở trên Web mở Android học tiếp đúng thẻ đó).
  3. `dkt_knowledge_states` & `ai_model_versions` (lưu cache xác suất nắm vững kiến thức của DKT và lịch sử huấn luyện AUC/Loss của model PyTorch).
  4. `class_folders` (gắn cả thư mục vào lớp học) & `submission_details JSONB` trong `activity_submissions` (xem chi tiết bài làm của học sinh).

### [2026-11-16] — Buổi 1 (Cập nhật lần 3): Mở rộng Phân hệ Quản trị (Admin Suite) & Bổ sung 8 Bảng Admin vào PostgreSQL
- Phân tích chuyên sâu Module Quản trị viên tham chiếu Quizlet & đặc thù AI/FSRS/DKT trong [system_design.md](file:///D:/Elearning-system/system_design.md) và bổ sung 8 bảng Admin vào [docs/DATABASE.md](file:///D:/Elearning-system/docs/DATABASE.md).

### [2026-11-16] — Buổi 1 (Cập nhật lần 2): Chia nhỏ Task theo Git Commit, Nhãn Đề xuất Model & Bỏ phụ thuộc Docker
- Chia nhỏ [FEATURE.md](file:///D:/Elearning-system/FEATURE.md) theo chuẩn 1 task = 1 git commit kèm nhãn độ khó `🟢 Flash` / `🟡 Medium` / `🔴 Model Mạnh/Pro` và tối ưu môi trường chạy Native Local trong [AGENTS.md](file:///D:/Elearning-system/AGENTS.md).

### [2026-11-16] — Buổi 1 (Khởi tạo): Phân tích Nghiệp vụ, Thiết kế Kiến trúc & CSDL PostgreSQL
- Khởi tạo bản thiết kế tổng thể [system_design.md](file:///D:/Elearning-system/system_design.md), thiết kế CSDL chi tiết [docs/DATABASE.md](file:///D:/Elearning-system/docs/DATABASE.md) và bộ file quản lý trạng thái dự án.

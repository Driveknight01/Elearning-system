# FEATURE.md — Danh sách Task Chi tiết (Chuẩn 1 Task = 1 Git Commit) & Khuyến nghị Mô hình AI

Tài liệu này chia nhỏ toàn bộ dự án thành **88 Task nguyên tử (Atomic Tasks)**. Mỗi task tương ứng với **1 lần `git commit` độc lập**, vừa vặn trong 1 lượt làm việc để dễ kiểm thử và không bị quá tải ngữ cảnh.

---

## 🧭 Chú giải Đánh dấu Độ khó & Đề xuất Chuyển đổi Mô hình AI

Để tối ưu hóa chất lượng code và tiết kiệm hạn mức (quota), mỗi task được gắn nhãn khuyến nghị mô hình rõ ràng:

- 🟢 **`[DỄ — Khuyên dùng: Model Nhanh / Flash]`**:
  - *Đặc điểm*: Tạo khung project, khai báo Entity, DTO, Repository, cấu hình cơ bản, màn hình UI tĩnh, CRUD đơn giản.
  - *Hành động*: **Chuyển về model nhẹ/nhanh (Flash)** để chạy tốc độ cao và tiết kiệm tài nguyên.
- 🟡 **`[VỪA — Khuyên dùng: Model Tiêu chuẩn / Medium]`**:
  - *Đặc điểm*: Viết Service nghiệp vụ có điều kiện, ghép nối API vào React/Android, xử lý phân quyền RBAC, form động, biểu đồ thống kê.
  - *Hành động*: Giữ ở **model tầm trung (Medium)** hoặc Flash (Thinking).
- 🔴 **`[KHÓ — ⚠️ ĐỀ XUẤT CHUYỂN SANG MODEL MẠNH / PRO / HIGH]`**:
  - *Đặc điểm*: Thuật toán toán học **FSRS-4.5**, mô hình học sâu **PyTorch LSTM (DKT)**, **WebSocket STOMP Real-time Multiplayer** đa nền tảng, **AI Chatbot Structured JSON Forward**, **Game Engine (Blocks/Gems/Blast)** và **Android Offline Sync (RoomDB + WorkManager)**.
  - *Hành động*: **Nên chuyển sang model mạnh nhất (Pro / High Reasoning)** trước khi giao các task có nhãn 🔴 này để đảm bảo thuật toán và kiến trúc chính xác tuyệt đối ngay từ lần đầu.

---

## Phase 1: Khởi tạo Khung Dự án & Cơ sở dữ liệu PostgreSQL (Local Native)
> *Lưu ý*: Chạy trực tiếp trên **PostgreSQL Native**, **IntelliJ IDEA** và **Android Studio** sẵn có trên máy (không bắt buộc bật Docker). Sử dụng `Spring Cache / ConcurrentHashMap` thay cho Redis container và lưu trữ ảnh tại thư mục `./uploads/` (hoặc Cloudinary) để tiết kiệm RAM.

- [x] **T1.1** 🟢 `[DỄ — Flash]`: Khởi tạo cấu trúc thư mục gốc, file `.gitignore` đa nền tảng (Java, Python, Node, Android) và script tạo database `smartflash_db` trên PostgreSQL local.
  - *Commit*: `chore(init): setup monorepo structure and gitignore`
- [x] **T1.2** 🟢 `[DỄ — Flash]`: Khởi tạo khung project `backend-springboot` (`pom.xml` với Spring Boot 3.x, Web, Data JPA, Security, Validation, WebSocket, Flyway, PostgreSQL Driver, Lombok) và `application.yml` kết nối PostgreSQL local.
  - *Commit*: `chore(be): initialize spring boot 3 project and postgres config`
- [x] **T1.3** 🟡 `[VỪA — Medium]`: Viết migration Flyway `V1__init_core_users_and_cards.sql` cho nhóm bảng Danh mục (`categories`), Người dùng (`users`, `oauth_accounts`, `refresh_tokens`, `verification_tokens`) và Bộ thẻ (`flashcard_sets`, `flashcards`, `user_set_interactions`, `starred_cards`, `folders`, `folder_sets`).
  - *Commit*: `feat(db): add V1 flyway migration for categories, users and flashcard tables`
- [x] **T1.4** 🟡 `[VỪA — Medium]`: Viết migration Flyway `V2__init_study_and_activities.sql` cho nhóm bảng Học tập FSRS/DKT (`user_card_progress`, `learning_logs`, `ai_model_versions`, `dkt_knowledge_states`), Kiểm tra & Game (`test_attempts`, `game_scores`, `live_game_rooms`, `live_game_participants`) và Lớp học/Giáo viên (`classes`, `class_members`, `class_sets`, `class_folders`, `assigned_activities`, `activity_submissions`).
  - *Commit*: `feat(db): add V2 flyway migration for fsrs, dkt, games and teacher activities`
- [x] **T1.5** 🟡 `[VỪA — Medium]`: Viết migration Flyway `V3__init_gamification_chat_admin_and_indexes.sql` cho nhóm bảng Nhóm học, Streak, Thành tựu, AI Chat (`ai_chat_sessions`, `ai_chat_messages`), 8 bảng Quản trị & Kiểm duyệt (`teacher_verification_requests`, `content_reports`, `sensitive_keywords`, `ai_usage_logs`, `system_settings`, `system_announcements`, `user_notifications`, `admin_audit_logs`) và toàn bộ các `CREATE INDEX`.
  - *Commit*: `feat(db): add V3 flyway migration for gamification, ai chat, admin suite and indexes`
- [x] **T1.6** 🟢 `[DỄ — Flash]`: Xây dựng gói `common` trong Spring Boot: `BaseEntity`, chuẩn hóa wrapper `ApiResponse<T>`, `PageResponse<T>`, `ErrorCode`, `AppException` và `GlobalExceptionHandler` + cấu hình phục vụ file ảnh tĩnh `/uploads/**`.
  - *Commit*: `feat(be-common): implement standard ApiResponse, exception handler and local storage config`
- [x] **T1.7** 🟢 `[DỄ — Flash]`: Khởi tạo khung project `ai-ml-service` (Python FastAPI, `requirements.txt`, cấu trúc module `dkt/`, `ocr/`, `chatbot/`, endpoint `/health` và CORS).
  - *Commit*: `chore(ai): initialize fastapi microservice skeleton`
- [x] **T1.8** 🟢 `[DỄ — Flash]`: Khởi tạo khung project `frontend-web` (React + Vite + TailwindCSS, cấu hình `axiosClient.js`, React Router, Zustand store cơ bản và bộ màu/font chuẩn Quizlet).
  - *Commit*: `chore(web): initialize react vite tailwindcss project and core routing`
- [x] **T1.9** 🟢 `[DỄ — Flash]`: Khởi tạo khung project `mobile-android` (Android Native Java, Empty Views Activity, `build.gradle` với Material 3, Retrofit2, Gson, Glide, RoomDB, CameraX và cấu trúc package MVVM).
  - *Commit*: `chore(android): initialize android native java project with mvvm packages`

---

## Phase 2: Xác thực (JWT + Google OAuth2), RBAC 4 Vai trò & Quyền riêng tư Trang cá nhân
> 🌿 **Quy tắc Nhánh Git**: Bắt đầu Phase 2, hãy tạo và chuyển sang nhánh: `git checkout -b feat/phase-2-auth`. Sau khi hoàn thành và test xong toàn bộ từ `T2.1` đến `T2.9`, merge về `main`: `git checkout main && git merge feat/phase-2-auth`.
- [x] **T2.1** 🟢 `[DỄ — Flash]`: Tạo JPA Entities & Repositories cho `User`, `OAuthAccount`, `RefreshToken`, `VerificationToken` và Enum `Role` (`USER`, `TEACHER`, `ADMIN`), `UserStatus`, `ProfileVisibility`.
  - *Commit*: `feat(auth): create user, oauth, refresh and verification token entities and repositories`
- [x] **T2.2** 🟡 `[VỪA — Medium]`: Cấu hình Spring Security 6, `JwtTokenProvider`, `JwtAuthenticationFilter` và `CustomUserDetailsService` hỗ trợ phân quyền theo Role và cho phép Guest truy cập các public endpoints.
  - *Commit*: `feat(security): configure spring security 6 and jwt authentication filter`
- [x] **T2.3** 🟡 `[VỪA — Medium]`: Xây dựng API Đăng ký, Đăng nhập bằng Email/Password, Quên mật khẩu (OTP), Refresh Token và Đăng xuất (`POST /api/v1/auth/register`, `/login`, `/forgot-password`, `/refresh`, `/logout`).
  - *Commit*: `feat(auth): implement register, login, forgot password otp, refresh token and logout apis`
- [x] **T2.4** 🔴 `[KHÓ — ⚠️ Đề xuất Model Mạnh/Pro]`: Xây dựng `GoogleOAuth2Verifier` và API `POST /api/v1/auth/google` xác thực Google `idToken` dùng chung cho cả Web React và Android Native (tự động tạo user + `oauth_accounts` + khởi tạo bảng `user_streaks` ở lần đầu đăng nhập).
  - *Commit*: `feat(auth): implement unified google id-token authentication for web and android`
- [x] **T2.5** 🟡 `[VỪA — Medium]`: Xây dựng API Hồ sơ cá nhân & **Quyền xem Trang cá nhân**: cập nhật thông tin, upload avatar, đổi mật khẩu, chỉnh `profile_visibility` và `profile_privacy_config` (`JSONB` bật/tắt hiển thị từng tab *Bộ thẻ, Thư mục, Lớp học, Thành tựu*) + API xem trang cá nhân `/api/v1/users/profile/{username}` lọc dữ liệu theo đúng quyền riêng tư.
  - *Commit*: `feat(user): implement profile management and granular tab privacy visibility`
- [x] **T2.6** 🟡 `[VỪA — Medium]`: [Web] Xây dựng `LoginPage`, `RegisterPage` (tích hợp nút *Continue with Google*), `ProtectedRoute` theo 4 vai trò và lưu trạng thái đăng nhập vào `useAuthStore`.
  - *Commit*: `feat(web-auth): build login, register pages with google sign-in and role guards`
- [x] **T2.7** 🟡 `[VỪA — Medium]`: [Web] Xây dựng `MainLayout` (Sidebar trái, Topbar tìm kiếm, menu tài khoản) và trang `UserProfilePage` (`/users/:username`) kèm `ProfilePrivacyModal` bật/tắt quyền xem từng tab.
  - *Commit*: `feat(web-profile): build main layout, user profile page and privacy settings modal`
- [x] **T2.8** 🟡 `[VỪA — Medium]`: [Android] Xây dựng `TokenManager` (`SharedPreferences`), `RetrofitClient` (kèm `AuthInterceptor` tự gắn Bearer JWT) và màn hình `SplashActivity`, `LoginActivity`, `RegisterActivity` (có Google Sign-In).
  - *Commit*: `feat(android-auth): implement retrofit jwt interceptor and login/register activities`
- [x] **T2.9** 🟡 `[VỪA — Medium]`: [Android] Xây dựng `MainActivity` với `BottomNavigationView` (4 tabs: *Trang chủ, Tìm kiếm, Thư viện, Cá nhân*) và `ProfileFragment` kèm Dialog chỉnh quyền riêng tư trang cá nhân.
  - *Commit*: `feat(android-main): implement MainActivity bottom navigation and ProfileFragment`

---

## Phase 3: CRUD Bộ thẻ Flashcard, Phân quyền Xem, OCR & Ảnh Minh họa AI
- [x] **T3.1** 🟢 `[DỄ — Flash]`: Tạo JPA Entities & Repositories cho `FlashcardSet`, `Flashcard`, `UserSetInteraction`, `StarredCard`, `Folder`, `StudyGroup`, `StudyGroupMember`.
  - *Commit*: `feat(flashcard): create entities and repositories for sets, cards, interactions, folders and groups`
- [ ] **T3.2** 🟡 `[VỪA — Medium]`: Xây dựng API CRUD Bộ thẻ (`POST/PUT/DELETE /api/v1/flashcard-sets`) xử lý lưu đồng bộ danh sách thẻ con, tự sinh `share_token` (NanoID) và kiểm tra chặt chẽ 3 mức quyền xem (`PUBLIC`, `PRIVATE`, `LINK_ONLY`).
  - *Commit*: `feat(flashcard): implement flashcard set CRUD and visibility access control`
- [ ] **T3.3** 🟡 `[VỪA — Medium]`: Xây dựng API Tìm kiếm & Khám phá bộ thẻ `PUBLIC`, API **Clone (Nhân bản)** bộ thẻ, API **Tương tác Bộ thẻ (`user_set_interactions`: Lưu bộ thẻ yêu thích, Học gần đây Recents, Đánh giá 1–5★, Lưu vị trí học dở `resume_state_json`)** và API gắn dấu sao thẻ khó (`starred_cards`).
  - *Commit*: `feat(flashcard): implement search, clone set, saved/recents/rating interactions and starred card apis`
- [ ] **T3.4** 🟢 `[DỄ — Flash]`: Xây dựng API CRUD Thư mục (`folders` — thêm/xóa bộ thẻ vào thư mục) và Nhóm học (`study_groups` — tạo nhóm, tham gia bằng `invite_code`, chia sẻ bộ thẻ vào nhóm).
  - *Commit*: `feat(organization): implement folder and study group management apis`
- [ ] **T3.5** 🔴 `[KHÓ — ⚠️ Đề xuất Model Mạnh/Pro]`: [AI Service + BE] Xây dựng module **OCR & Card Parser** trong `ai-ml-service/app/ocr/` (nhận ảnh chụp tài liệu -> trích xuất văn bản -> phân tích cú pháp/LLM tách thành mảng `[{term, definition}]`) và kết nối với Spring Boot qua `POST /api/v1/flashcard-sets/ocr-extract`.
  - *Commit*: `feat(ocr): implement ocr image-to-flashcards extraction pipeline in fastapi and springboot`
- [ ] **T3.6** 🟡 `[VỪA — Medium]`: [BE] Xây dựng `MediaController`: upload ảnh thẻ, tìm ảnh minh họa miễn phí theo từ khóa `Term` (Pixabay/Unsplash API) và tạo ảnh minh họa tự động bằng AI (`Pollinations/Imagen`) lưu URL vào thẻ.
  - *Commit*: `feat(media): implement stock image search and AI mnemonic image generation for cards`
- [ ] **T3.7** 🟡 `[VỪA — Medium]`: [Web] Xây dựng `LandingExplorePage` (cho cả Guest và User) và `LibraryPage` (quản lý Bộ thẻ của tôi, Thư mục, Nhóm học).
  - *Commit*: `feat(web-library): build explore landing page, personal library, folders and study groups ui`
- [ ] **T3.8** 🔴 `[KHÓ — ⚠️ Đề xuất Model Mạnh/Pro]`: [Web] Xây dựng `SetEditorPage` (Tạo/Sửa bộ thẻ): thêm/xóa/kéo-thả thứ tự thẻ, chọn quyền xem (`Public/Private/Link-only`), **Import nhanh bằng Copy-Paste (Tab/CSV)**, **Modal Quét ảnh OCR**, chọn ảnh AI cho từng thẻ, và nhận dữ liệu `prefillData` từ AI Chatbot.
  - *Commit*: `feat(web-editor): build comprehensive SetEditorPage with OCR modal, bulk import and AI image picker`
- [ ] **T3.9** 🟡 `[VỪA — Medium]`: [Web] Xây dựng `SetDetailPage`: hiển thị thẻ lật xem trước, phát âm tự động **Text-to-Speech (`window.speechSynthesis`)**, gắn sao `★` thẻ khó, nút Clone bộ thẻ, Copy Share Link và bảng chọn các chế độ học/chơi game.
  - *Commit*: `feat(web-set-detail): build SetDetailPage with TTS pronunciation, star cards and share link`
- [ ] **T3.10** 🟡 `[VỪA — Medium]`: [Android] Xây dựng `HomeFragment`, `SearchFragment` và `LibraryFragment` (hiển thị danh sách bộ thẻ, thư mục, nhóm học từ API).
  - *Commit*: `feat(android-library): implement Home, Search and Library fragments with RecyclerView adapters`
- [ ] **T3.11** 🔴 `[KHÓ — ⚠️ Đề xuất Model Mạnh/Pro]`: [Android] Xây dựng `CreateEditSetActivity` và `OcrScannerActivity` (tích hợp **CameraX** chụp ảnh tài liệu/sách, gửi ảnh lên API OCR và tự động đổ danh sách cặp `Term - Definition` vào `RecyclerView` để chỉnh sửa & lưu).
  - *Commit*: `feat(android-editor): implement CreateEditSetActivity and CameraX OcrScannerActivity`
- [ ] **T3.12** 🟡 `[VỪA — Medium]`: [Android] Xây dựng `SetDetailActivity` tích hợp `android.speech.tts.TextToSpeech` phát âm thuật ngữ/định nghĩa, gắn sao thẻ khó, chia sẻ link và điều hướng sang các chế độ học.
  - *Commit*: `feat(android-set-detail): implement SetDetailActivity with Android TTS and share options`

---

## Phase 4: AI Chatbot Tùy biến Ngữ cảnh & Tạo Bộ thẻ Forward sang CRUD (Q-Chat)
- [ ] **T4.1** 🔴 `[KHÓ — ⚠️ Đề xuất Model Mạnh/Pro]`: [AI Service] Xây dựng `ai-ml-service/app/chatbot/`: thiết kế System Prompt thông minh nhận ngữ cảnh (danh sách thẻ của bộ đang học + các thẻ yếu từ DKT), tích hợp Gemini/OpenAI API với **Structured JSON Schema** để tự động phân loại `intent` (`GENERAL_CHAT` vs `GENERATE_FLASHCARD_SET`) và sinh cấu trúc `suggestedSet: { title, description, tags, cards: [{term, definition}] }`.
  - *Commit*: `feat(ai-chatbot): implement context-aware tutor and structured flashcard set generator in fastapi`
- [ ] **T4.2** 🟡 `[VỪA — Medium]`: [BE] Tạo Entities `AiChatSession`, `AiChatMessage` và xây dựng `AiChatController` (`POST /api/v1/ai-chat/messages`, `GET /api/v1/ai-chat/sessions`) tự động truy vấn ngữ cảnh bộ thẻ + tiến độ học của user từ PostgreSQL gửi sang `ai-ml-service`.
  - *Commit*: `feat(be-chatbot): implement chat session persistence and context injection service`
- [ ] **T4.3** 🟡 `[VỪA — Medium]`: [Web] Xây dựng `AiChatDrawer` nổi góc phải màn hình: tự nhận biết người dùng đang ở bộ thẻ nào để gắn ngữ cảnh, hiển thị `GeneratedSetPreviewCard` ngay trong khung chat và nút **"Chỉnh sửa & Lưu bộ thẻ"** forward dữ liệu (`navigate('/sets/create', { state: { prefillData } })`) điền sẵn vào `SetEditorPage`.
  - *Commit*: `feat(web-chatbot): build context-aware AiChatDrawer with one-click forward to SetEditorPage`
- [ ] **T4.4** 🟡 `[VỪA — Medium]`: [Android] Xây dựng `AiChatBottomSheet` / `AiChatActivity`: giao diện chat đa `ViewType`, hiển thị thẻ xem trước bộ thẻ AI vừa tạo và nút **"Điền vào trang tạo thẻ"** truyền `SuggestedSetDto` qua `Intent` sang `CreateEditSetActivity`.
  - *Commit*: `feat(android-chatbot): implement AiChatBottomSheet and Intent prefill forward to CreateEditSetActivity`

---

## Phase 5: Lõi Học tập Cá nhân hóa – Thuật toán FSRS-4.5 & Deep Knowledge Tracing (DKT)
- [ ] **T5.1** 🔴 `[KHÓ — ⚠️ Đề xuất Model Mạnh/Pro]`: [BE] Cài đặt thuật toán **FSRS-4.5 Scheduler** thuần Java (`FsrsScheduler.java`, `FsrsParameters.java`, `FsrsCardState.java`) tính toán chính xác *Difficulty ($D$)*, *Stability ($S$)*, *Retrievability ($R$)* và khoảng cách thời gian ôn tập cho cả 4 mức đánh giá (`1=Again`, `2=Hard`, `3=Good`, `4=Easy`) kèm Unit Test kiểm chứng công thức.
  - *Commit*: `feat(study-fsrs): implement FSRS-4.5 mathematical scheduler engine in Java with unit tests`
- [ ] **T5.2** 🔴 `[KHÓ — ⚠️ Đề xuất Model Mạnh/Pro]`: [AI Service] Xây dựng mạng nơ-ron **PyTorch LSTM cho Deep Knowledge Tracing (DKT)** trong `ai-ml-service/app/dkt/model.py`, kịch bản khởi tạo/huấn luyện trọng số (`train.py`) và endpoint `POST /dkt/predict` nhận chuỗi tương tác $(q_t, a_t)$ trả về xác suất nắm vững $\hat{y} \in [0, 1]^N$ cho từng thẻ.
  - *Commit*: `feat(ai-dkt): implement PyTorch LSTM Deep Knowledge Tracing model and inference endpoint`
- [ ] **T5.3** 🔴 `[KHÓ — ⚠️ Đề xuất Model Mạnh/Pro]`: [BE] Xây dựng `StudyService` & `StudyController`:
  - API lấy danh sách thẻ cần ôn (`GET /api/v1/study/due-cards` & `/smart-session` kết hợp thẻ đến hạn FSRS + thẻ có xác suất DKT thấp).
  - API ghi nhận kết quả vuốt thẻ (`POST /api/v1/study/review` & `/sync-batch`), cập nhật `user_card_progress`, lưu `learning_logs` và gọi sang Python DKT bất đồng bộ (`@Async`) kèm cơ chế **Fallback** nếu AI Service không phản hồi.
  - *Commit*: `feat(be-study): integrate FSRS review transaction with async DKT mastery prediction and fallback`
- [ ] **T5.4** 🟡 `[VỪA — Medium]`: [Web] Xây dựng `FlashcardStudyPage`: hiệu ứng lật thẻ 3D mượt mà, hỗ trợ phím tắt (`Space` lật thẻ, `1-2-3-4` chọn mức nhớ), thanh hiển thị thời gian lặp lại dự kiến trên 4 nút (*Again, Hard, Good, Easy*) và màn hình tổng kết phiên học kèm biểu đồ tiến độ DKT Mastery.
  - *Commit*: `feat(web-study): build 3D FlashcardStudyPage with FSRS interval preview and DKT mastery summary`
- [ ] **T5.5** 🔴 `[KHÓ — ⚠️ Đề xuất Model Mạnh/Pro]`: [Android] Xây dựng tầng **Offline-first bằng RoomDB** (`AppDatabase`, `FlashcardDao`, `OfflineReviewLogEntity`) + bản cài đặt `LocalFsrsCalculator` trên Android và `WorkManagerSync` tự động đẩy lịch sử vuốt thẻ offline lên Spring Boot khi có mạng.
  - *Commit*: `feat(android-offline): implement RoomDB offline flashcard storage and WorkManager FSRS sync`
- [ ] **T5.6** 🟡 `[VỪA — Medium]`: [Android] Xây dựng `FlashcardStudyActivity` với hiệu ứng lật thẻ 3D (`ObjectAnimator`), thao tác vuốt thẻ (Swipe Gesture) hoặc bấm 4 nút đánh giá FSRS.
  - *Commit*: `feat(android-study): build FlashcardStudyActivity with 3D card flip and swipe rating controls`

---

## Phase 6: Học qua Kiểm tra, Bộ Minigames Chuẩn Quizlet & Đấu trường Live (WebSocket)
- [ ] **T6.1** 🟡 `[VỪA — Medium]`: [BE] Xây dựng `AssessmentService` (`GET /api/v1/assessments/generate-test`, `POST /api/v1/assessments/submit-test`): thuật toán `DistractorGenerator` chọn 3 đáp án nhiễu thông minh cùng bộ thẻ cho câu hỏi trắc nghiệm 4 đáp án, kết hợp chế độ **Adaptive Learn** (chuyển từ trắc nghiệm sang tự luận gõ từ khi chỉ số DKT Mastery $\ge 0.75$).
  - *Commit*: `feat(assessment): implement smart 4-option distractor generator and DKT-based adaptive learn api`
- [ ] **T6.2** 🟢 `[DỄ — Flash]`: [BE] Xây dựng API lưu kỷ lục điểm/thời gian và truy vấn **Bảng xếp hạng (Leaderboard)** (`POST /api/v1/games/scores`, `GET /api/v1/games/leaderboard/{setId}`) cho 4 Minigames: `MATCH`, `BLAST`, `GEMS`, `BLOCKS` (đồng thời đẩy các thẻ trả lời sai trong game vào `learning_logs`).
  - *Commit*: `feat(games): implement minigame score submission, leaderboard and mistake logging`
- [ ] **T6.3** 🔴 `[KHÓ — ⚠️ Đề xuất Model Mạnh/Pro]`: [BE] Xây dựng **Real-time Multiplayer Server** bằng `Spring WebSocket + STOMP` (`WebSocketConfig`, `LiveGameController`, `LiveRoomManager` lưu trạng thái phòng trên RAM `ConcurrentHashMap`): hỗ trợ tạo phòng mã PIN 6 số, sảnh chờ người chơi (Lobby), phát bộ câu hỏi/bàn cờ đồng loạt, tính điểm theo tốc độ phản hồi (ms) và broadcast tiến độ trực tiếp cho cả chế độ *Class Live Battle* và *1v1 Match Race*.
  - *Commit*: `feat(live-game): implement Spring WebSocket STOMP real-time multiplayer server and room state manager`
- [ ] **T6.4** 🟡 `[VỪA — Medium]`: [Web] Xây dựng `AdaptiveLearnPage` (lộ trình học 3 bước: Làm quen → Trắc nghiệm 4 đáp án → Tự gõ từ) và `TestPage` (làm bài kiểm tra trắc nghiệm/tự luận, chấm điểm & xem giải thích).
  - *Commit*: `feat(web-assessment): build AdaptiveLearnPage and multiple-choice TestPage`
- [ ] **T6.5** 🟡 `[VỪA — Medium]`: [Web] Xây dựng 2 Minigames phản xạ nhanh: **Ghép thẻ (`MatchGamePage` — lưới 4x4 tính giờ đến 0.1s, phạt +1s khi ghép sai)** và **Bắn thiên thạch (`BlastGamePage` — bắn tàu/thiên thạch mang đáp án đúng, nhân hệ số Combo)**.
  - *Commit*: `feat(web-games): build Quizlet-style Match card game and Blast meteor shooter game`
- [ ] **T6.6** 🔴 `[KHÓ — ⚠️ Đề xuất Model Mạnh/Pro]`: [Web] Xây dựng 2 Minigames chiến thuật mới của Quizlet: **Đá quý (`GemsGamePage` — trả lời câu hỏi nối chuỗi đá quý năng lượng)** và **Khối hộp (`BlocksGamePage` — trả lời đúng câu hỏi nhận 3 khối gạch đặt vào bảng lưới 8x8 xóa hàng/cột)**.
  - *Commit*: `feat(web-games): build Quizlet Gems chain game and Q-Blocks 8x8 puzzle game`
- [ ] **T6.7** 🔴 `[KHÓ — ⚠️ Đề xuất Model Mạnh/Pro]`: [Web] Xây dựng `LiveGameHostPage` (chiếu mã PIN 6 số + QR Code, quản lý sảnh chờ, bảng xếp hạng trực tiếp) và `LiveGamePlayPage` (kết nối `@stomp/stompjs` thi đấu thời gian thực).
  - *Commit*: `feat(web-live): build real-time LiveGameHostPage and player client using STOMP WebSocket`
- [ ] **T6.8** 🟡 `[VỪA — Medium]`: [Android] Xây dựng `TestActivity` (làm bài kiểm tra 4 đáp án) và `AdaptiveLearnActivity` trên di động.
  - *Commit*: `feat(android-assessment): implement multiple-choice TestActivity and AdaptiveLearnActivity`
- [ ] **T6.9** 🟡 `[VỪA — Medium]`: [Android] Xây dựng `GameMatchActivity` (ghép cặp thẻ tính giờ trên lưới 3x4) và `GameBlastActivity` (chạm phá thiên thạch đáp án đúng).
  - *Commit*: `feat(android-games): implement GameMatchActivity and GameBlastActivity with leaderboard`
- [ ] **T6.10** 🔴 `[KHÓ — ⚠️ Đề xuất Model Mạnh/Pro]`: [Android] Xây dựng `LiveGameActivity` kết nối WebSocket STOMP với Spring Boot, cho phép người dùng điện thoại Android nhập mã PIN 6 số vào chung phòng và thi đấu trực tiếp với người dùng trên Web React.
  - *Commit*: `feat(android-live): implement cross-platform real-time LiveGameActivity via WebSocket`

---

## Phase 7: Phân hệ Giáo viên – Lớp học, Chỉ định Hoạt động (Không cần đăng nhập) & Báo cáo
- [ ] **T7.1** 🟢 `[DỄ — Flash]`: [BE] Tạo JPA Entities & Repositories cho `Classroom` (`classes`), `ClassMember`, `AssignedActivity`, `ActivitySubmission`.
  - *Commit*: `feat(classroom): create entities and repositories for classes and assigned activities`
- [ ] **T7.2** 🟡 `[VỪA — Medium]`: [BE] Xây dựng API Quản lý Lớp học (`POST/GET/PUT/DELETE /api/v1/classes`): tạo lớp sinh `invite_code`, học sinh tham gia lớp, giáo viên duyệt/xóa học viên và gắn bộ thẻ vào lớp.
  - *Commit*: `feat(classroom): implement teacher class management and student enrollment apis`
- [ ] **T7.3** 🔴 `[KHÓ — ⚠️ Đề xuất Model Mạnh/Pro]`: [BE] Xây dựng API **Giáo viên Chỉ định Hoạt động (`POST /api/v1/assigned-activities`)** cho 1 trong 7 hoạt động (*Học, Thẻ ghi nhớ, Kiểm tra, Ghép thẻ, Đá quý, Khối hộp, Blast*) và API cho phép học sinh vào làm bài qua `activity_code` **không cần đăng nhập** (`allow_guest = true`, cấp `guest_session_token` và lưu tiến độ vào `activity_submissions`).
  - *Commit*: `feat(teacher-activity): implement assign activity api and login-free guest student submission flow`
- [ ] **T7.4** 🟡 `[VỪA — Medium]`: [BE] Xây dựng API **Theo dõi Tiến độ Hoạt động & Thống kê Lớp học** (`GET /api/v1/assigned-activities/{id}/report`, `GET /api/v1/classes/{id}/analytics`): tổng hợp danh sách học sinh *Đã hoàn thành / Đang làm*, điểm số, các thẻ cả lớp hay sai nhất và mức độ thành thạo DKT.
  - *Commit*: `feat(teacher-analytics): implement real-time activity progress tracking and class DKT mastery report`
- [ ] **T7.5** 🟡 `[VỪA — Medium]`: [Web] Xây dựng `AssignActivityModal` (thiết kế đúng chuẩn giao diện 2 cột của Quizlet như ảnh mẫu: cột trái banner minh họa lợi ích, cột phải radio chọn 1 trong 7 hoạt động *Học, Thẻ ghi nhớ, Kiểm tra, Ghép thẻ, Đá quý, Khối hộp, Blast* -> xuất Link & Mã tham gia).
  - *Commit*: `feat(web-teacher): build Quizlet-style AssignActivityModal with 7 activity types`
- [ ] **T7.6** 🟡 `[VỪA — Medium]`: [Web] Xây dựng `GuestActivityJoinPage` (`/join/:activityCode` — học sinh mở link chỉ cần nhập Họ tên để làm ngay hoạt động giáo viên giao không cần đăng nhập) và `TeacherClassDetailPage` (quản lý lớp, xem bảng theo dõi tiến độ hoạt động & khởi tạo trò chơi Live).
  - *Commit*: `feat(web-classroom): build login-free student activity join page and teacher progress dashboard`
- [ ] **T7.7** 🟡 `[VỪA — Medium]`: [Android] Xây dựng `ClassListActivity`, `ClassDetailActivity` và tính năng giao/làm hoạt động được chỉ định ngay trên ứng dụng Android.
  - *Commit*: `feat(android-classroom): implement class detail, activity assignment and progress view on Android`

---

## Phase 8: Gamification (Chuỗi học Streak, Thành tựu) & Phân hệ Quản trị viên (Admin Suite)
- [ ] **T8.1** 🟡 `[VỪA — Medium]`: [BE] Xây dựng `GamificationService`: tự động cập nhật **Chuỗi ngày học (`user_streaks`, `user_daily_activities`)** theo múi giờ người dùng sau mỗi phiên học/chơi game và kiểm tra điều kiện mở khóa **Thành tựu (`achievements`, `user_achievements`)** + Hộp thư thông báo (`user_notifications`).
  - *Commit*: `feat(gamification): implement daily streak engine, heatmap logs, achievements and user notifications`
- [ ] **T8.2** 🟢 `[DỄ — Flash]`: [BE] Tạo JPA Entities & Repositories cho toàn bộ phân hệ Admin (`Category`, `TeacherVerificationRequest`, `ContentReport`, `SensitiveKeyword`, `AiUsageLog`, `SystemSetting`, `SystemAnnouncement`, `AdminAuditLog`).
  - *Commit*: `feat(admin): create entities and repositories for 8 admin, taxonomy and moderation tables`
- [ ] **T8.3** 🟡 `[VỪA — Medium]`: [BE] Xây dựng API Quản trị **Cây Danh mục Môn học (`categories`)**, **Bộ thẻ Nổi bật (`is_featured`)**, **Duyệt Xác minh Giáo viên (`teacher_verification_requests` — cấp tích xanh Verified Teacher)** và Quản lý Tài khoản (`SUSPENDED / BANNED`) kèm ghi nhận `admin_audit_logs`.
  - *Commit*: `feat(admin): implement category taxonomy, featured sets, teacher verification and user moderation apis`
- [ ] **T8.4** 🟡 `[VỪA — Medium]`: [BE] Xây dựng API **Kiểm duyệt Báo cáo Đa đối tượng (`content_reports` cho `SET / CLASS / GROUP / USER`)**, bộ lọc tự động gắn cờ **Từ khóa nhạy cảm (`sensitive_keywords`)**, **Dashboard Analytics + Giám sát AI (`ai_usage_logs`)** và Cấu hình hệ thống động (`system_settings`, `system_announcements`).
  - *Commit*: `feat(admin): implement multi-entity moderation, sensitive keyword filter, AI monitoring and system settings apis`
- [ ] **T8.5** 🟡 `[VỪA — Medium]`: [Web] Xây dựng giao diện **Streak Widget**, **Biểu đồ nhiệt (Activity Heatmap)**, bộ sưu tập **Huy hiệu Thành tựu (Badges)**, quả chuông Thông báo (`user_notifications`) và form gửi **Yêu cầu Xác minh Giáo viên**.
  - *Commit*: `feat(web-gamification): build streak counter, heatmap, badges gallery, notifications and teacher verification form`
- [ ] **T8.6** 🟡 `[VỪA — Medium]`: [Web] Xây dựng **Admin Portal — Phần 1**: `AdminDashboardPage` (biểu đồ tăng trưởng, tỷ lệ nhớ FSRS, phân bố chế độ học, biểu đồ giám sát token/latency AI & OCR) và `AdminUsersAndTeachersPage` (quản lý user + duyệt hồ sơ *Verified Teacher*).
  - *Commit*: `feat(web-admin): build executive analytics, AI monitoring dashboard and teacher verification portal`
- [ ] **T8.7** 🟡 `[VỪA — Medium]`: [Web] Xây dựng **Admin Portal — Phần 2**: `AdminTaxonomyPage` (CRUD cây danh mục môn học & ghim Featured Sets), `AdminModerationPage` (duyệt báo cáo vi phạm đa đối tượng + từ điển từ khóa cấm), và `AdminSystemConfigPage` (chỉnh Quota AI/OCR, CRUD huy hiệu thành tựu, đăng Banner thông báo & xem Audit Logs).
  - *Commit*: `feat(web-admin): build category taxonomy, content moderation queue, AI quota settings and audit log pages`
- [ ] **T8.8** 🟡 `[VỪA — Medium]`: [Android] Hiển thị Streak, Huy hiệu, Thông báo trên ứng dụng Android và cài đặt `StudyReminderWorker` (`WorkManager`) đẩy thông báo nhắc học hàng ngày khi sắp mất chuỗi Streak.
  - *Commit*: `feat(android-gamification): add streak widget, badges view, notifications and daily study reminder`
- [ ] **T8.9** 🟢 `[DỄ — Flash]`: Tạo script/data mẫu (`DataSeeder.java`) khởi tạo sẵn cây danh mục môn học, cấu hình `system_settings`, 10 huy hiệu thành tựu, tài khoản mẫu cho 3 vai trò (`admin`, `teacher`, `student`), 5 bộ thẻ mẫu đa chủ đề và lớp học mẫu để phục vụ chạy thử nghiệm & demo bảo vệ đồ án.
  - *Commit*: `feat(seed): add comprehensive demo data seeder for categories, settings, roles, sets and classes`

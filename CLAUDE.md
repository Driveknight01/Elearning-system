# CLAUDE.md / PROJECT_STATE.md — Trạng thái Dự án & Nhật ký Phát triển

> **LƯU Ý CHO AI AGENT**: Hãy đọc [AGENTS.md](file:///D:/Elearning-system/AGENTS.md) trước khi viết bất kỳ dòng code nào. Sau mỗi task hoàn thành trong [FEATURE.md](file:///D:/Elearning-system/FEATURE.md), bạn **BẮT BUỘC** phải cập nhật mục **Trạng thái hiện tại**, **CHANGELOG** trong file này (đồng bộ với [PROJECT_STATE.md](file:///D:/Elearning-system/PROJECT_STATE.md)) và nhắc người dùng chuyển đổi mô hình AI (`🟢 Flash` / `🟡 Medium` / `🔴 Pro`) cho task kế tiếp.

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

### 🔄 Đang thực hiện (In Progress)
- Đang triển khai **Phase 1**: Khởi tạo khung dự án Local Native (`T1.3` → `T1.9`).

### ⏭️ Kế tiếp (Next Up)
- [ ] **T1.3** 🟡 `[VỪA — Medium]`: Viết migration Flyway `V1__init_core_users_and_cards.sql` cho nhóm bảng Danh mục, Người dùng và Bộ thẻ.
- [ ] **T1.4** 🟡 `[VỪA — Medium]`: Viết migration Flyway `V2__init_study_and_activities.sql` cho nhóm bảng Học tập FSRS/DKT, Kiểm tra & Game và Lớp học/Giáo viên.
- [ ] **T1.5** 🟡 `[VỪA — Medium]`: Viết migration Flyway `V3__init_gamification_chat_admin_and_indexes.sql` cho nhóm bảng Nhóm học, Streak, Thành tựu, AI Chat, 8 bảng Quản trị & Kiểm duyệt và toàn bộ `CREATE INDEX`.

---

## 3. CHANGELOG (Nhật ký theo từng buổi làm việc)

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

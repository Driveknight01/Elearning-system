# AGENTS.md — Quy chuẩn Kỹ thuật, Luật Code & Quy trình Làm việc cho AI Agents

Tài liệu này là **Hiến pháp Kỹ thuật** của dự án **Hệ thống học tập đa nền tảng cá nhân hóa (SmartFlash)**. Mọi AI Agent và lập trình viên tham gia dự án **BẮT BUỘC** đọc kỹ và tuân thủ nghiêm ngặt các quy định dưới đây.

---

## 1. MỆNH LỆNH BẮT BUỘC VỀ QUẢN LÝ TRẠNG THÁI (MANDATORY AGENT PROTOCOL)

> [!IMPORTANT]
> **LUẬT BẤT DI BẤT DỊCH KHI LÀM VIỆC THEO TASK**:
> 1. **Trước khi bắt đầu code**: Luôn đọc [CLAUDE.md](file:///D:/Elearning-system/CLAUDE.md) (hoặc [PROJECT_STATE.md](file:///D:/Elearning-system/PROJECT_STATE.md)) và [FEATURE.md](file:///D:/Elearning-system/FEATURE.md) để nắm chính xác task hiện tại đang làm dở và không viết trùng lặp hoặc phá vỡ cấu trúc đã có.
> 2. **Khi thiết kế hoặc sửa đổi Database**: Phải đối chiếu và cập nhật ngay vào [docs/DATABASE.md](file:///D:/Elearning-system/docs/DATABASE.md).
> 3. **Ngay sau khi hoàn thành mỗi Task**: Agent **BẮT BUỘC** phải thực hiện 2 thao tác trước khi kết thúc lượt trả lời:
>    - Đánh dấu `[x]` vào mã Task tương ứng (ví dụ `T1.1`, `T2.1`...) trong file [FEATURE.md](file:///D:/Elearning-system/FEATURE.md).
>    - Cập nhật mục **Trạng thái hiện tại (Đã xong / Đang dở / Kế tiếp)** và ghi thêm 1 dòng vào **CHANGELOG** trong cả [CLAUDE.md](file:///D:/Elearning-system/CLAUDE.md) và [PROJECT_STATE.md](file:///D:/Elearning-system/PROJECT_STATE.md).
> 4. **Nhắc nhở Chuyển đổi Mô hình AI (Model Switching Reminder)**: Khi kết thúc một task và giới thiệu task kế tiếp trong [FEATURE.md](file:///D:/Elearning-system/FEATURE.md), Agent phải kiểm tra nhãn độ khó của task kế tiếp (`🟢 [DỄ — Flash]`, `🟡 [VỪA — Medium]`, hoặc `🔴 [KHÓ — ⚠️ Đề xuất Model Mạnh/Pro]`) để **chủ động nhắc người dùng** chuyển sang mô hình phù hợp trước khi chạy task tiếp theo.
> 5. **Nhắc nhở Chiến lược Tạo Nhánh Git (Git Branching Strategy Reminder)**:
>    - **Phase 1 (`T1.1` → `T1.9`)**: Được phép commit trực tiếp trên nhánh `main` vì là thiết lập khung sườn chung.
>    - **Từ Phase 2 trở đi**: Khi bắt đầu một Phase mới (ví dụ `T2.1`, `T3.1`, `T4.1`...), Agent **BẮT BUỘC** phải chủ động nhắc người dùng tạo và chuyển sang nhánh riêng của Phase đó theo mẫu `feat/phase-<số_phase>-<tên_phase>` (ví dụ: `git checkout -b feat/phase-2-auth`). Khi hoàn tất toàn bộ Phase và kiểm thử xanh mới nhắc merge về `main`.
>    - **Khi thử nghiệm kỹ thuật rủi ro/khó**: Nhắc người dùng tách nhánh `exp/<tên-tính-năng>` để giữ an toàn cho code chính.

---

## 2. Stack Công nghệ Cốt lõi & Môi trường Phát triển (Local Native — Không cần Docker)

Máy phát triển đã cài đặt sẵn **IntelliJ IDEA**, **PostgreSQL Native** và **Android Studio**, do đó toàn bộ hệ thống chạy trực tiếp (Native Local) để tiết kiệm RAM, không bắt buộc dùng `docker-compose.yml`:

| Thành phần | Công nghệ & Phiên bản (Local Native) | Vai trò trong Hệ thống |
| :--- | :--- | :--- |
| **DBMS (Cơ sở dữ liệu)** | **PostgreSQL 16+ (Cài trực tiếp trên máy)** | Hệ quản trị CSDL quan hệ chính (`smartflash_db`); tận dụng `TIMESTAMPTZ`, `JSONB`, `TEXT[]` và `GIN Index`. |
| **Cache & Real-time State** | **`ConcurrentHashMap` / `Spring Cache (Caffeine)`** *(tùy chọn Redis sau này)* | Lưu trạng thái phòng chơi Live (`LiveRoomState`), Bảng xếp hạng Minigames và Cache ngay trên RAM của Spring Boot (0ms latency, không cần cài Redis container). |
| **Object Storage** | **Local `./uploads/`** + **Cloudinary Free Tier** | Lưu ảnh minh họa thẻ Flashcard, ảnh chụp OCR và Avatar người dùng trực tiếp mà không cần chạy MinIO Docker. |
| **Core Backend** | **Java 17/21 + Spring Boot 3.x (IntelliJ IDEA)** | RESTful API, Spring Security (JWT + Google OAuth2), Spring Data JPA, Flyway, **FSRS Engine (Java)**, **WebSocket (STOMP)** cho Live Game. |
| **AI / ML Microservice** | **Python 3.10+ + FastAPI** | 1. **DKT (Deep Knowledge Tracing)** bằng PyTorch LSTM.<br>2. **OCR Engine** (PaddleOCR / VietOCR / Tesseract).<br>3. **Context-Aware AI Chatbot & Structured Card Generator** (Gemini API / OpenAI). |
| **Frontend Web** | **React 18+ (Vite) + TailwindCSS** | Giao diện Web cho 4 vai trò (Guest, User, Teacher, Admin), Zustand, Axios, `@stomp/stompjs`, Web Speech API (TTS). |
| **Mobile App** | **Android Native (Java, Empty Views Activity - Android Studio)** | Kiến trúc MVVM, XML Views (`ConstraintLayout`, `RecyclerView`, `ViewPager2`), Retrofit2, **RoomDB** (Offline FSRS), CameraX (OCR), Android TTS. |

---

## 3. Quy ước Đặt tên (Naming Conventions)

### 3.1. Cơ sở dữ liệu (PostgreSQL — xem chi tiết tại `docs/DATABASE.md`)
- Tên bảng: `snake_case`, số nhiều (`users`, `flashcard_sets`, `assigned_activities`).
- Tên cột: `snake_case`, số ít (`user_id`, `next_review_at`, `share_token`).
- Khóa ngoại: `fk_<tên_bảng>_<tên_cột>`, Index: `idx_<tên_bảng>_<tên_cột>`.
- Migration Flyway: `V<số_thứ_tự>__<mô_tả_snake_case>.sql` (ví dụ: `V1__init_schema.sql`).

### 3.2. Backend (Spring Boot - Java)
- Package gốc: `com.elearning` chia theo module nghiệp vụ (`auth`, `user`, `flashcard`, `study`, `assessment`, `chatbot`, `classroom`, `group`, `admin`).
- Class: `PascalCase` (`FlashcardSetController`, `FsrsSchedulerService`, `AssignedActivityRepository`).
- Biến & Phương thức: `camelCase` (`calculateNextReview`, `shareToken`).
- Hằng số & Enum: `UPPER_SNAKE_CASE` (`Role.ROLE_TEACHER`, `Visibility.LINK_ONLY`).
- DTO: Hậu tố `Request` hoặc `Response` (`CreateSetRequest`, `SetDetailResponse`, `SuggestedSetDto`). **Tuyệt đối không trả trực tiếp JPA Entity ra Controller**.
- REST Endpoint: `/api/v1/<resource-plural-kebab-case>` (ví dụ: `/api/v1/flashcard-sets`, `/api/v1/assigned-activities`).

### 3.3. AI/ML Service (FastAPI - Python)
- File, module, biến, hàm: `snake_case` (`card_parser.py`, `predict_mastery_scores()`).
- Class & Pydantic Schema: `PascalCase` (`DktPredictRequest`, `SuggestedFlashcardSet`).

### 3.4. Frontend Web (React)
- Component & Page files: `PascalCase.jsx` / `.tsx` (`SetEditorPage.jsx`, `AssignActivityModal.jsx`, `AiChatDrawer.jsx`).
- Hooks & Utils: `camelCase.js` (`useAuth.js`, `useTextToSpeech.js`, `axiosClient.js`).
- CSS: Ưu tiên class tiện ích của **TailwindCSS**; tránh viết inline style rời rạc.

### 3.5. Mobile Android (Java - Empty Views Activity)
- Activity / Fragment / Adapter / ViewModel: `PascalCase` (`FlashcardStudyActivity.java`, `CreateEditSetActivity.java`, `CardListAdapter.java`).
- Layout XML: `<loại>_<tên_snake_case>.xml` (`activity_flashcard_study.xml`, `fragment_home.xml`, `item_flashcard_edit.xml`, `dialog_assign_activity.xml`).
- View ID trong XML: `<viết_tắt_view>_<tên>` (`btn_submit`, `tv_term`, `et_definition`, `rv_cards`).

---

## 4. Luật Code & Nguyên tắc Kiến trúc (Coding & Architectural Rules)

1. **Chuẩn hóa Response API từ Spring Boot**:
   Mọi API trong `backend-springboot` phải trả về wrapper thống nhất để Web và Android parse đồng bộ:
   ```json
   {
     "success": true,
     "message": "Thành công",
     "data": { ... },
     "timestamp": "2026-11-16T09:30:00Z"
   }
   ```
2. **Tách biệt FSRS và DKT để đảm bảo Tốc độ & Chịu lỗi (Resilience)**:
   - **FSRS** chạy trực tiếp trong Java (`study` module) đồng bộ trong transaction để phản hồi thao tác vuốt thẻ < 10ms.
   - **DKT** (Python FastAPI) được gọi bất đồng bộ (`@Async`) sau khi lưu `learning_logs`, hoặc có timeout ngắn (1.5s) kèm **Fallback** về FSRS thuần nếu `ai-ml-service` chưa bật.
3. **Bảo mật & Quyền truy cập Bộ thẻ (`Visibility`)**:
   - Mọi truy vấn xem bộ thẻ (`GET /api/v1/flashcard-sets/{id}`) phải kiểm tra:
     - Nếu `PUBLIC`: Cho phép tất cả (kể cả Guest).
     - Nếu `LINK_ONLY`: Cho phép chủ sở hữu hoặc request truyền đúng `?token={share_token}` (hoặc học sinh truy cập qua `activity_code` giáo viên chỉ định).
     - Nếu `PRIVATE`: Chỉ `owner_id` (hoặc Admin) mới được truy cập.
4. **Luồng Forward từ AI Chatbot sang Trang Tạo Thẻ (CRUD)**:
   - AI Chatbot khi sinh bộ thẻ phải trả về JSON đúng cấu trúc `suggestedSet: { title, description, cards: [{ term, definition }] }` và **không tự ý lưu thẳng vào DB** mà phải cho phép người dùng bấm Forward sang màn hình CRUD (`SetEditorPage` trên Web / `CreateEditSetActivity` trên Android) để rà soát và lưu.

---

## 5. Quy tắc Git & Kiểm thử (Git & Testing Rules)

1. **Quy ước Commit Message (Conventional Commits)**:
   - `feat(scope): mô tả tính năng mới` (vd: `feat(study): implement FSRS-4.5 scheduler in Java`)
   - `fix(scope): mô tả lỗi đã sửa`
   - `docs(scope): cập nhật tài liệu / trạng thái dự án`
   - `refactor(scope): tối ưu code không đổi nghiệp vụ`
2. **Quy tắc Kiểm thử trước khi đánh dấu Hoàn thành (`[x]`)**:
   - **Backend Java**: Đảm bảo `mvn compile` / `mvn test` không lỗi; viết Unit Test cho `FsrsScheduler` và logic phân quyền.
   - **AI Service Python**: Kiểm tra endpoint bằng Swagger (`/docs`) hoặc `pytest`.
   - **Frontend Web**: Đảm bảo `npm run build` không phát sinh lỗi cú pháp hoặc thiếu import.
   - **Android Java**: Đảm bảo `./gradlew assembleDebug` biên dịch thành công, không thiếu khai báo Activity trong `AndroidManifest.xml`.

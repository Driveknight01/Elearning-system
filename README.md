# SmartFlash — Hệ thống Học tập Đa nền tảng Cá nhân hóa (FSRS + Deep Knowledge Tracing)

> **Đồ án Tốt nghiệp**: Nghiên cứu và xây dựng hệ thống học tập đa nền tảng cá nhân hóa sử dụng **Spaced Repetition (FSRS)** và **Machine Learning (DKT)** trên nền tảng **Spring Boot**, **React** và **Android Native (Java)**.

Hệ thống kế thừa trải nghiệm học tập trực quan của **Quizlet** (Web & Mobile) và nâng cấp với lõi trí tuệ nhân tạo cá nhân hóa: thuật toán lặp lại ngắt quãng **FSRS-4.5**, mạng học sâu **Deep Knowledge Tracing (PyTorch LSTM)**, **OCR** quét ảnh tạo thẻ, **AI Chatbot tùy biến ngữ cảnh** (tự động sinh bộ thẻ điền sẵn sang màn hình CRUD), bộ **Minigames** (*Ghép thẻ, Blast, Đá quý, Khối hộp*) và **Đấu trường Live** thời gian thực qua WebSocket.

---

## 1. Kiến trúc & Stack Công nghệ (Local Native)

| Phân hệ | Công nghệ sử dụng | Vai trò |
| :--- | :--- | :--- |
| **Database (DBMS)** | **PostgreSQL 16+** (Native) | Lưu trữ dữ liệu quan hệ, `TIMESTAMPTZ`, `JSONB`, `TEXT[]` + `GIN Index`, quản lý migration bằng **Flyway**. |
| **Core Backend** | **Java 17/21 + Spring Boot 3.x** | Modular Monolith: REST API, Spring Security (JWT + Google OAuth2), RBAC 4 vai trò, **FSRS-4.5 Engine (Java)**, **WebSocket (STOMP)** cho Live Game. |
| **AI / ML Service** | **Python 3.10+ + FastAPI** | Microservice tách riêng cho **DKT (PyTorch LSTM)**, **OCR (PaddleOCR/VietOCR)** và **Context-Aware AI Chatbot** (Gemini/OpenAI). |
| **Frontend Web** | **React 18+ (Vite) + TailwindCSS** | Giao diện Web cho 4 vai trò (*Guest, User, Teacher, Admin*), Zustand, Axios, `@stomp/stompjs`, Web Speech API (TTS). |
| **Mobile App** | **Android Native (Java, Empty Views)** | Kiến trúc MVVM, XML Views, Retrofit2, **RoomDB** (Offline FSRS + WorkManager Sync), **CameraX** (OCR), Android TTS. |

---

## 2. Cấu trúc Thư mục & Tài liệu Dự án

```text
D:\Elearning-system\
├── README.md                    # Tài liệu giới thiệu & hướng dẫn chạy dự án
├── system_design.md             # Phân tích thiết kế hệ thống, màn hình & kiến trúc
├── FEATURE.md                   # Danh sách 66 Task chia nhỏ chuẩn 1 Task = 1 Git Commit
├── AGENTS.md                    # Quy chuẩn kỹ thuật, luật code & quy trình làm việc
├── CLAUDE.md                    # Trạng thái hiện tại & CHANGELOG theo từng buổi code
├── PROJECT_STATE.md             # Bản đồng bộ trạng thái dự án
├── docs/
│   └── DATABASE.md              # Thiết kế chi tiết 41 bảng PostgreSQL & ERD
│
├── backend-springboot/          # Core Backend (Mở bằng IntelliJ IDEA)
├── ai-ml-service/               # AI/ML Microservice (Python FastAPI)
├── frontend-web/                # Web App (React + Vite)
└── mobile-android/              # Android App (Mở bằng Android Studio)
```

---

## 3. Hướng dẫn Cài đặt & Chạy trên Máy Local (Không cần Docker)

### Yêu cầu môi trường
- **JDK 17+** & **Maven** (hoặc dùng trực tiếp qua **IntelliJ IDEA**)
- **PostgreSQL 16+** (đã cài đặt và chạy service trên port `5432`)
- **Python 3.10+**
- **Node.js 18+** (`npm`)
- **Android Studio** (JDK / Android SDK)

### Bước 1: Tạo Database PostgreSQL
Mở **pgAdmin 4** hoặc **psql** và chạy lệnh:
```sql
CREATE DATABASE smartflash_db;
```

### Bước 2: Chạy Core Backend (`backend-springboot`)
1. Mở **IntelliJ IDEA** → Chọn **Open** → trỏ trực tiếp vào thư mục `D:\Elearning-system\backend-springboot` (hoặc chọn file `pom.xml` → *Open as Project*).
2. Cấu hình username/password PostgreSQL trong `src/main/resources/application.yml` (hoặc biến môi trường).
3. Bấm **Run** `ElearningApplication.java` — Flyway sẽ tự động tạo toàn bộ 41 bảng vào `smartflash_db`.
4. Kiểm tra API Swagger tại: `http://localhost:8080/swagger-ui.html`

### Bước 3: Chạy AI/ML Microservice (`ai-ml-service`)
```bash
cd ai-ml-service
python -m venv venv
# Windows:
venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```
Kiểm tra Swagger của FastAPI tại: `http://localhost:8000/docs`

### Bước 4: Chạy Frontend Web (`frontend-web`)
```bash
cd frontend-web
npm install
npm run dev
```
Truy cập Web tại: `http://localhost:5173`

### Bước 5: Chạy Mobile App (`mobile-android`)
1. Mở **Android Studio** → Chọn **Open** → trỏ vào thư mục `D:\Elearning-system\mobile-android`.
2. Đợi Gradle Sync hoàn tất và bấm **Run** trên Emulator hoặc điện thoại Android thật.
   - *Lưu ý*: Nếu chạy trên Emulator, địa chỉ trỏ về Backend Spring Boot trên máy tính là `http://10.0.2.2:8080`.

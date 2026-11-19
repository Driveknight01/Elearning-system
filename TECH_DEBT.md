# TECH_DEBT.md — Danh sách Nợ Kỹ thuật (Technical Debt)

Tài liệu này ghi nhận các hạng mục còn hạn chế hoặc tạm hoãn để tối ưu, nâng cấp ở các giai đoạn tiếp theo.

---

## 1. Phase 3 — Mobile Android (`T3.11`: OCR & Tự động tách thẻ)
- **Vấn đề**: Tính năng nhận diện OCR tự động tách cặp `Term - Definition` trong [ScanDocumentActivity.java](file:///d:/Elearning-system/mobile-android/app/src/main/java/com/elearning/smartflash/ui/set/ScanDocumentActivity.java) và [card_parser.py](file:///d:/Elearning-system/ai-ml-service/app/ocr/card_parser.py) **vẫn chưa tốt**:
  - Độ chính xác nhận diện tiếng Việt có dấu giảm mạnh khi rơi vào fallback Google ML Kit on-device (do model Latin mặc định không tối ưu cho dấu tiếng Việt) hoặc khi ảnh đầu vào có độ phân giải thấp / kích thước nhỏ.
  - Bộ tách cặp tự động theo dấu phân cách & trục cột (`Global Column Split X`) vẫn có thể tách thiếu hoặc nhầm đối với ảnh chụp nghiêng, nền phức tạp, nhiều cột lồng nhau hoặc văn bản xuống dòng không đều.
- **Hướng xử lý dự kiến**:
  - Tiền xử lý ảnh trước khi OCR (upscale ảnh nhỏ, thêm viền trắng padding, tăng tương phản / khử nhiễu).
  - Tinh chỉnh pipeline `RapidOCR + VietOCR` hoặc ưu tiên Vision LLM (`Gemini Vision`) khi có cấu hình API key để trả về JSON chuẩn cặp `[{term, definition}]` chính xác hơn.
  - Bổ sung cơ chế hiển thị thông báo rõ nguyên nhân khi server `ai-ml-service` chưa kết nối được (thay vì âm thầm fallback khiến người dùng tưởng server đang nhận diện kém).

---

## 2. Phase 3 — Mobile Android & Web (`T3.10`: Trang chủ Active Learner)
- **Vấn đề**: Giao diện Active Learner và việc nối API tiến độ học thật + `/flashcard-sets/explore` trên [HomeFragment.java](file:///d:/Elearning-system/mobile-android/app/src/main/java/com/elearning/smartflash/ui/home/HomeFragment.java) (cùng Web `HomePage`) tạm hoãn.
- **Hướng xử lý dự kiến**: Hoàn thiện sau khi xong **Phase 5** (có dữ liệu học tập thật từ FSRS/DKT).

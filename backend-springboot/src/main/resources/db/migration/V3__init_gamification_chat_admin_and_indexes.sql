-- ============================================================================
-- V3: Khởi tạo Nhóm học, Gamification, AI Chatbot, Quản trị Admin & Indexes
-- DBMS: PostgreSQL 16+
-- ============================================================================

-- 26. Nhóm học tự lập (Study Groups)
CREATE TABLE study_groups (
    id          BIGSERIAL PRIMARY KEY,
    creator_id  BIGINT NOT NULL,
    name        VARCHAR(150) NOT NULL,
    description TEXT NULL,
    invite_code VARCHAR(15) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_study_groups_creator_id FOREIGN KEY (creator_id) REFERENCES users(id)
);

-- 27. Thành viên Nhóm học
CREATE TABLE study_group_members (
    group_id    BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    role        VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_id, user_id),
    CONSTRAINT fk_sgm_group_id FOREIGN KEY (group_id) REFERENCES study_groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_sgm_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 28. Bộ thẻ chia sẻ trong Nhóm học
CREATE TABLE study_group_sets (
    group_id    BIGINT NOT NULL,
    set_id      BIGINT NOT NULL,
    shared_by   BIGINT NOT NULL,
    shared_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_id, set_id),
    CONSTRAINT fk_sgs_group_id FOREIGN KEY (group_id) REFERENCES study_groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_sgs_set_id FOREIGN KEY (set_id) REFERENCES flashcard_sets(id) ON DELETE CASCADE,
    CONSTRAINT fk_sgs_shared_by FOREIGN KEY (shared_by) REFERENCES users(id)
);

-- 29. Chuỗi học (Streak) & Tổng XP của người dùng
CREATE TABLE user_streaks (
    user_id             BIGINT PRIMARY KEY,
    current_streak      INT NOT NULL DEFAULT 0,
    longest_streak      INT NOT NULL DEFAULT 0,
    last_active_date    DATE NULL,
    freeze_count        INT NOT NULL DEFAULT 0,
    total_xp            BIGINT NOT NULL DEFAULT 0,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_streaks_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 30. Hoạt động học tập theo từng ngày (Vẽ Heatmap & Lịch Streak)
CREATE TABLE user_daily_activities (
    user_id         BIGINT NOT NULL,
    activity_date   DATE NOT NULL,
    cards_reviewed  INT NOT NULL DEFAULT 0,
    study_seconds   INT NOT NULL DEFAULT 0,
    xp_earned       INT NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, activity_date),
    CONSTRAINT fk_uda_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 31. Danh mục Huy hiệu Thành tựu (Achievements)
CREATE TABLE achievements (
    id                      BIGSERIAL PRIMARY KEY,
    code                    VARCHAR(50) NOT NULL UNIQUE,
    name                    VARCHAR(100) NOT NULL,
    description             VARCHAR(255) NOT NULL,
    icon_url                TEXT NULL,
    condition_type          VARCHAR(40) NOT NULL,
    condition_threshold     INT NOT NULL,
    xp_reward               INT NOT NULL DEFAULT 50
);

-- 32. Huy hiệu người dùng đã mở khóa
CREATE TABLE user_achievements (
    user_id         BIGINT NOT NULL,
    achievement_id  BIGINT NOT NULL,
    unlocked_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, achievement_id),
    CONSTRAINT fk_ua_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ua_achievement_id FOREIGN KEY (achievement_id) REFERENCES achievements(id) ON DELETE CASCADE
);

-- 33. Phiên hội thoại AI Chatbot tùy biến ngữ cảnh
CREATE TABLE ai_chat_sessions (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    context_set_id  BIGINT NULL,
    title           VARCHAR(150) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_sessions_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_sessions_set_id FOREIGN KEY (context_set_id) REFERENCES flashcard_sets(id) ON DELETE SET NULL
);

-- 34. Tin nhắn AI Chatbot (Bao gồm JSON bộ thẻ gợi ý để Forward sang CRUD)
CREATE TABLE ai_chat_messages (
    id                  BIGSERIAL PRIMARY KEY,
    session_id          BIGINT NOT NULL,
    sender              VARCHAR(15) NOT NULL,
    content             TEXT NOT NULL,
    intent              VARCHAR(30) NOT NULL DEFAULT 'GENERAL_CHAT',
    generated_set_json  JSONB NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_messages_session_id FOREIGN KEY (session_id) REFERENCES ai_chat_sessions(id) ON DELETE CASCADE
);

-- 35. Yêu cầu xác minh & nâng cấp quyền Giáo viên (Verified Teacher)
CREATE TABLE teacher_verification_requests (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    school_name         VARCHAR(200) NOT NULL,
    edu_email           VARCHAR(150) NULL,
    proof_document_url  TEXT NULL,
    bio_message         TEXT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_note          TEXT NULL,
    reviewed_by         BIGINT NULL,
    reviewed_at         TIMESTAMPTZ NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tvr_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_tvr_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL
);

-- 36. Báo cáo vi phạm đa đối tượng (SET, CLASS, GROUP, USER)
CREATE TABLE content_reports (
    id              BIGSERIAL PRIMARY KEY,
    reporter_id     BIGINT NOT NULL,
    target_type     VARCHAR(20) NOT NULL,
    target_id       BIGINT NOT NULL,
    reason          VARCHAR(50) NOT NULL,
    details         TEXT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    action_taken    VARCHAR(30) NULL,
    reviewed_by     BIGINT NULL,
    reviewed_at     TIMESTAMPTZ NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reports_reporter_id FOREIGN KEY (reporter_id) REFERENCES users(id),
    CONSTRAINT fk_reports_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL
);

-- 37. Bộ lọc từ khóa nhạy cảm / từ khóa cấm tự động gắn cờ
CREATE TABLE sensitive_keywords (
    id          BIGSERIAL PRIMARY KEY,
    keyword     VARCHAR(100) NOT NULL UNIQUE,
    severity    VARCHAR(25) NOT NULL DEFAULT 'FLAG_FOR_REVIEW',
    created_by  BIGINT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sensitive_kw_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

-- 38. Giám sát tài nguyên AI Chatbot, OCR, AI Image Gen & DKT
CREATE TABLE ai_usage_logs (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NULL,
    feature_type    VARCHAR(30) NOT NULL,
    model_name      VARCHAR(60) NULL,
    tokens_used     INT NOT NULL DEFAULT 0,
    latency_ms      INT NOT NULL,
    is_success      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_usage_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- 39. Cấu hình hệ thống động (AI Quota, FSRS Default Weights, System Prompt)
CREATE TABLE system_settings (
    setting_key     VARCHAR(80) PRIMARY KEY,
    setting_value   JSONB NOT NULL,
    description     VARCHAR(255) NULL,
    updated_by      BIGINT NULL,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sys_settings_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
);

-- 40. Thông báo / Banner toàn hệ thống từ Admin
CREATE TABLE system_announcements (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    content     TEXT NOT NULL,
    target_role VARCHAR(20) NOT NULL DEFAULT 'ALL',
    banner_type VARCHAR(20) NOT NULL DEFAULT 'INFO',
    starts_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at  TIMESTAMPTZ NULL,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_by  BIGINT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sys_announce_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

-- 41. Hộp thư thông báo cá nhân (Chuông thông báo trên Web & Android)
CREATE TABLE user_notifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    type        VARCHAR(40) NOT NULL,
    title       VARCHAR(150) NOT NULL,
    message     TEXT NOT NULL,
    action_url  TEXT NULL,
    is_read     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_notif_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 42. Nhật ký kiểm toán hành động Quản trị viên (Admin Audit Logs)
CREATE TABLE admin_audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    admin_id        BIGINT NOT NULL,
    action          VARCHAR(60) NOT NULL,
    target_type     VARCHAR(30) NOT NULL,
    target_id       VARCHAR(50) NOT NULL,
    details_json    JSONB NULL,
    ip_address      VARCHAR(45) NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_admin_audit_admin_id FOREIGN KEY (admin_id) REFERENCES users(id)
);

-- ============================================================================
-- CHIẾN LƯỢC ĐÁNH INDEX TỐI ƯU HIỆU NĂNG POSTGRESQL
-- ============================================================================

-- 1. Tìm kiếm nhanh bộ thẻ PUBLIC, Danh mục, Tags & Bộ thẻ Gần đây / Đã lưu
CREATE INDEX idx_flashcard_sets_visibility_created
    ON flashcard_sets(visibility, moderation_status, created_at DESC)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_flashcard_sets_owner_id ON flashcard_sets(owner_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_flashcard_sets_category_id ON flashcard_sets(category_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_flashcard_sets_featured
    ON flashcard_sets(is_featured)
    WHERE visibility = 'PUBLIC' AND deleted_at IS NULL;
CREATE INDEX idx_flashcard_sets_tags_gin ON flashcard_sets USING GIN(tags);
CREATE INDEX idx_flashcard_sets_share_token ON flashcard_sets(share_token);
CREATE INDEX idx_flashcards_set_id_order ON flashcards(set_id, order_index);

CREATE INDEX idx_usi_user_recents ON user_set_interactions(user_id, last_accessed_at DESC);
CREATE INDEX idx_usi_user_saved ON user_set_interactions(user_id) WHERE is_saved = TRUE;

-- 2. Truy vấn thẻ đến hạn ôn tập FSRS (Quan trọng nhất)
CREATE INDEX idx_ucp_user_due ON user_card_progress(user_id, next_review_at ASC);
CREATE INDEX idx_ucp_user_set ON user_card_progress(user_id, set_id);

-- 3. Lấy chuỗi lịch sử học tập cho mô hình DKT (Deep Knowledge Tracing)
CREATE INDEX idx_learning_logs_user_time ON learning_logs(user_id, reviewed_at DESC);
CREATE INDEX idx_learning_logs_set_id ON learning_logs(set_id);

-- 4. Bảng xếp hạng Minigames & Theo dõi hoạt động Giáo viên giao
CREATE INDEX idx_game_scores_leaderboard ON game_scores(set_id, game_type, score DESC, time_ms ASC);
CREATE INDEX idx_activity_submissions_lookup ON activity_submissions(activity_id, progress_status);
CREATE INDEX idx_assigned_activities_code ON assigned_activities(activity_code);
CREATE INDEX idx_live_rooms_pin ON live_game_rooms(pin_code);

-- 5. Tối ưu Dashboard Quản trị, Kiểm duyệt, Giám sát AI & Thông báo
CREATE INDEX idx_content_reports_status_created ON content_reports(status, created_at DESC);
CREATE INDEX idx_teacher_req_status ON teacher_verification_requests(status, created_at DESC);
CREATE INDEX idx_ai_usage_user_date ON ai_usage_logs(user_id, feature_type, created_at DESC);
CREATE INDEX idx_user_notifications_unread ON user_notifications(user_id, is_read, created_at DESC);
CREATE INDEX idx_admin_audit_created ON admin_audit_logs(created_at DESC);

-- ============================================================================
-- V2: Khởi tạo Lõi Học tập (FSRS & DKT), Kiểm tra, Minigames, Live & Lớp học
-- DBMS: PostgreSQL 16+
-- ============================================================================

-- 12. Trạng thái lặp lại ngắt quãng FSRS-4.5 cho từng cặp (user, card)
CREATE TABLE user_card_progress (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    card_id             BIGINT NOT NULL,
    set_id              BIGINT NOT NULL,
    state               SMALLINT NOT NULL DEFAULT 0,
    stability           DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    difficulty          DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    elapsed_days        INT NOT NULL DEFAULT 0,
    scheduled_days      INT NOT NULL DEFAULT 0,
    reps                INT NOT NULL DEFAULT 0,
    lapses              INT NOT NULL DEFAULT 0,
    last_rating         SMALLINT NULL,
    last_review_at      TIMESTAMPTZ NULL,
    next_review_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dkt_mastery_prob    DOUBLE PRECISION NOT NULL DEFAULT 0.5,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ucp_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ucp_card_id FOREIGN KEY (card_id) REFERENCES flashcards(id) ON DELETE CASCADE,
    CONSTRAINT fk_ucp_set_id FOREIGN KEY (set_id) REFERENCES flashcard_sets(id) ON DELETE CASCADE,
    CONSTRAINT uq_ucp_user_card UNIQUE (user_id, card_id)
);

-- 13. Chuỗi lịch sử tương tác học tập (Đầu vào cho DKT LSTM & FSRS Optimizer)
CREATE TABLE learning_logs (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    card_id             BIGINT NOT NULL,
    set_id              BIGINT NOT NULL,
    study_mode          VARCHAR(30) NOT NULL,
    fsrs_rating         SMALLINT NULL,
    is_correct          BOOLEAN NOT NULL,
    response_time_ms    INT NULL,
    state_before        SMALLINT NULL,
    stability_before    DOUBLE PRECISION NULL,
    difficulty_before   DOUBLE PRECISION NULL,
    reviewed_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_learning_logs_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_learning_logs_card_id FOREIGN KEY (card_id) REFERENCES flashcards(id) ON DELETE CASCADE,
    CONSTRAINT fk_learning_logs_set_id FOREIGN KEY (set_id) REFERENCES flashcard_sets(id) ON DELETE CASCADE
);

-- 14. Quản lý các phiên bản mô hình Machine Learning DKT
CREATE TABLE ai_model_versions (
    id                      BIGSERIAL PRIMARY KEY,
    model_type              VARCHAR(30) NOT NULL DEFAULT 'DKT_LSTM',
    version_tag             VARCHAR(50) NOT NULL UNIQUE,
    weights_path            TEXT NOT NULL,
    auc_score               NUMERIC(5,4) NULL,
    loss_value              NUMERIC(6,4) NULL,
    trained_samples_count   BIGINT NOT NULL DEFAULT 0,
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    trained_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 15. Cache xác suất nắm vững kiến thức DKT theo từng bộ thẻ
CREATE TABLE dkt_knowledge_states (
    user_id             BIGINT NOT NULL,
    set_id              BIGINT NOT NULL,
    model_version_id    BIGINT NULL,
    overall_mastery     NUMERIC(5,4) NOT NULL DEFAULT 0.5000,
    card_mastery_map    JSONB NOT NULL DEFAULT '{}'::jsonb,
    weak_card_ids       BIGINT[] NOT NULL DEFAULT '{}',
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, set_id),
    CONSTRAINT fk_dkt_states_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_dkt_states_set_id FOREIGN KEY (set_id) REFERENCES flashcard_sets(id) ON DELETE CASCADE,
    CONSTRAINT fk_dkt_states_model_id FOREIGN KEY (model_version_id) REFERENCES ai_model_versions(id) ON DELETE SET NULL
);

-- 16. Lịch sử làm bài Kiểm tra (Trắc nghiệm 4 đáp án & Tự luận)
CREATE TABLE test_attempts (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    set_id              BIGINT NOT NULL,
    total_questions     INT NOT NULL,
    correct_answers     INT NOT NULL,
    score_percent       NUMERIC(5,2) NOT NULL,
    duration_seconds    INT NOT NULL,
    details_json        JSONB NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_test_attempts_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_test_attempts_set_id FOREIGN KEY (set_id) REFERENCES flashcard_sets(id) ON DELETE CASCADE
);

-- 17. Bảng xếp hạng Minigames (MATCH, BLAST, GEMS, BLOCKS)
CREATE TABLE game_scores (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    set_id          BIGINT NOT NULL,
    game_type       VARCHAR(20) NOT NULL,
    score           INT NOT NULL DEFAULT 0,
    time_ms         INT NOT NULL,
    mistakes_count  INT NOT NULL DEFAULT 0,
    played_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_game_scores_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_game_scores_set_id FOREIGN KEY (set_id) REFERENCES flashcard_sets(id) ON DELETE CASCADE
);

-- 18. Lớp học của Giáo viên
CREATE TABLE classes (
    id                      BIGSERIAL PRIMARY KEY,
    teacher_id              BIGINT NOT NULL,
    name                    VARCHAR(150) NOT NULL,
    description             TEXT NULL,
    school_name             VARCHAR(150) NULL,
    invite_code             VARCHAR(15) NOT NULL UNIQUE,
    allow_member_add_sets   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at              TIMESTAMPTZ NULL,
    CONSTRAINT fk_classes_teacher_id FOREIGN KEY (teacher_id) REFERENCES users(id)
);

-- 19. Thành viên trong Lớp học
CREATE TABLE class_members (
    class_id    BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (class_id, user_id),
    CONSTRAINT fk_class_members_class_id FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
    CONSTRAINT fk_class_members_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 20. Bộ thẻ gắn vào Lớp học
CREATE TABLE class_sets (
    class_id    BIGINT NOT NULL,
    set_id      BIGINT NOT NULL,
    added_by    BIGINT NOT NULL,
    added_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (class_id, set_id),
    CONSTRAINT fk_class_sets_class_id FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
    CONSTRAINT fk_class_sets_set_id FOREIGN KEY (set_id) REFERENCES flashcard_sets(id) ON DELETE CASCADE,
    CONSTRAINT fk_class_sets_added_by FOREIGN KEY (added_by) REFERENCES users(id)
);

-- 21. Thư mục gắn vào Lớp học
CREATE TABLE class_folders (
    class_id    BIGINT NOT NULL,
    folder_id   BIGINT NOT NULL,
    added_by    BIGINT NOT NULL,
    added_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (class_id, folder_id),
    CONSTRAINT fk_class_folders_class_id FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
    CONSTRAINT fk_class_folders_folder_id FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE CASCADE,
    CONSTRAINT fk_class_folders_added_by FOREIGN KEY (added_by) REFERENCES users(id)
);

-- 22. Phòng chơi Live thời gian thực (Quizlet Live / 1v1 Match Race)
CREATE TABLE live_game_rooms (
    id          BIGSERIAL PRIMARY KEY,
    host_id     BIGINT NOT NULL,
    set_id      BIGINT NOT NULL,
    class_id    BIGINT NULL,
    pin_code    VARCHAR(8) NOT NULL,
    game_mode   VARCHAR(30) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    started_at  TIMESTAMPTZ NULL,
    ended_at    TIMESTAMPTZ NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_live_rooms_host_id FOREIGN KEY (host_id) REFERENCES users(id),
    CONSTRAINT fk_live_rooms_set_id FOREIGN KEY (set_id) REFERENCES flashcard_sets(id) ON DELETE CASCADE,
    CONSTRAINT fk_live_rooms_class_id FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE SET NULL
);

-- 23. Người tham gia phòng chơi Live (Hỗ trợ cả Guest bằng nickname)
CREATE TABLE live_game_participants (
    id              BIGSERIAL PRIMARY KEY,
    room_id         BIGINT NOT NULL,
    user_id         BIGINT NULL,
    nickname        VARCHAR(60) NOT NULL,
    team_name       VARCHAR(50) NULL,
    final_score     INT NOT NULL DEFAULT 0,
    correct_count   INT NOT NULL DEFAULT 0,
    rank_position   INT NULL,
    CONSTRAINT fk_live_part_room_id FOREIGN KEY (room_id) REFERENCES live_game_rooms(id) ON DELETE CASCADE,
    CONSTRAINT fk_live_part_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- 24. Hoạt động Giáo viên chỉ định (7 loại: LEARN, FLASHCARDS, TEST, MATCH, GEMS, BLOCKS, BLAST)
CREATE TABLE assigned_activities (
    id              BIGSERIAL PRIMARY KEY,
    teacher_id      BIGINT NOT NULL,
    class_id        BIGINT NULL,
    set_id          BIGINT NOT NULL,
    title           VARCHAR(200) NOT NULL,
    activity_type   VARCHAR(30) NOT NULL,
    activity_code   VARCHAR(20) NOT NULL UNIQUE,
    allow_guest     BOOLEAN NOT NULL DEFAULT TRUE,
    config_json     JSONB NOT NULL DEFAULT '{}'::jsonb,
    starts_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_at          TIMESTAMPTZ NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_assigned_act_teacher_id FOREIGN KEY (teacher_id) REFERENCES users(id),
    CONSTRAINT fk_assigned_act_class_id FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE SET NULL,
    CONSTRAINT fk_assigned_act_set_id FOREIGN KEY (set_id) REFERENCES flashcard_sets(id) ON DELETE CASCADE
);

-- 25. Bài làm / Tiến độ Học sinh trong Hoạt động được giao (Hỗ trợ cả Guest không cần đăng nhập)
CREATE TABLE activity_submissions (
    id                      BIGSERIAL PRIMARY KEY,
    activity_id             BIGINT NOT NULL,
    user_id                 BIGINT NULL,
    guest_student_name      VARCHAR(100) NULL,
    guest_session_token     VARCHAR(64) NULL,
    progress_status         VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    completion_percent      NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    score                   NUMERIC(7,2) NULL,
    duration_seconds        INT NOT NULL DEFAULT 0,
    wrong_card_ids          BIGINT[] NOT NULL DEFAULT '{}',
    submission_details      JSONB NOT NULL DEFAULT '{}'::jsonb,
    started_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at            TIMESTAMPTZ NULL,
    CONSTRAINT fk_act_sub_activity_id FOREIGN KEY (activity_id) REFERENCES assigned_activities(id) ON DELETE CASCADE,
    CONSTRAINT fk_act_sub_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

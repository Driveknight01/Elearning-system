-- ============================================================================
-- V1: Khởi tạo Danh mục, Người dùng (Auth/RBAC/Privacy) & Bộ thẻ Flashcard
-- DBMS: PostgreSQL 16+
-- ============================================================================

-- 1. Cây Danh mục Môn học & Chủ đề (Subject Taxonomy)
CREATE TABLE categories (
    id              BIGSERIAL PRIMARY KEY,
    parent_id       BIGINT NULL,
    name            VARCHAR(100) NOT NULL,
    slug            VARCHAR(120) NOT NULL UNIQUE,
    icon_name       VARCHAR(60) NULL,
    color_hex       VARCHAR(10) NOT NULL DEFAULT '#4255FF',
    display_order   INT NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_categories_parent_id FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL
);

-- 2. Người dùng (USER, TEACHER, ADMIN)
CREATE TABLE users (
    id                      BIGSERIAL PRIMARY KEY,
    username                VARCHAR(50) NOT NULL UNIQUE,
    email                   VARCHAR(150) NOT NULL UNIQUE,
    password_hash           VARCHAR(255) NULL,
    full_name               VARCHAR(100) NOT NULL,
    avatar_url              TEXT NULL,
    bio                     VARCHAR(500) NULL,
    role                    VARCHAR(20) NOT NULL DEFAULT 'USER',
    is_verified_teacher     BOOLEAN NOT NULL DEFAULT FALSE,
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    suspended_until         TIMESTAMPTZ NULL,
    ban_reason              TEXT NULL,
    daily_goal_cards        INT NOT NULL DEFAULT 20,
    timezone                VARCHAR(50) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    profile_visibility      VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    profile_privacy_config  JSONB NOT NULL DEFAULT '{"showSets":true,"showFolders":true,"showClasses":true,"showAchievements":true}'::jsonb,
    fsrs_weights            JSONB NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at              TIMESTAMPTZ NULL
);

-- 3. Tài khoản liên kết OAuth2 (Google Sign-In / Sign-Up)
CREATE TABLE oauth_accounts (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    provider            VARCHAR(30) NOT NULL,
    provider_user_id    VARCHAR(150) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_oauth_accounts_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_oauth_provider_user UNIQUE (provider, provider_user_id)
);

-- 4. Quản lý Refresh Token & FCM Push Token đa thiết bị (Web + Android)
CREATE TABLE refresh_tokens (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    token_hash      VARCHAR(255) NOT NULL UNIQUE,
    platform        VARCHAR(20) NOT NULL DEFAULT 'WEB',
    device_info     VARCHAR(255) NULL,
    fcm_token       TEXT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 5. Mã OTP xác thực Email & Quên mật khẩu
CREATE TABLE verification_tokens (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    token_code      VARCHAR(64) NOT NULL,
    token_type      VARCHAR(30) NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    used_at         TIMESTAMPTZ NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_verification_tokens_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 6. Bộ thẻ Flashcard
CREATE TABLE flashcard_sets (
    id                  BIGSERIAL PRIMARY KEY,
    owner_id            BIGINT NOT NULL,
    category_id         BIGINT NULL,
    cloned_from_set_id  BIGINT NULL,
    title               VARCHAR(200) NOT NULL,
    description         TEXT NULL,
    visibility          VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    moderation_status   VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    is_featured         BOOLEAN NOT NULL DEFAULT FALSE,
    share_token         VARCHAR(40) NOT NULL UNIQUE,
    term_lang           VARCHAR(10) NOT NULL DEFAULT 'en',
    def_lang            VARCHAR(10) NOT NULL DEFAULT 'vi',
    tags                TEXT[] NOT NULL DEFAULT '{}',
    source_type         VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    card_count          INT NOT NULL DEFAULT 0,
    view_count          INT NOT NULL DEFAULT 0,
    clone_count         INT NOT NULL DEFAULT 0,
    avg_rating          NUMERIC(3,2) NOT NULL DEFAULT 0.00,
    rating_count        INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at          TIMESTAMPTZ NULL,
    CONSTRAINT fk_flashcard_sets_owner_id FOREIGN KEY (owner_id) REFERENCES users(id),
    CONSTRAINT fk_flashcard_sets_category_id FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    CONSTRAINT fk_flashcard_sets_cloned_from FOREIGN KEY (cloned_from_set_id) REFERENCES flashcard_sets(id) ON DELETE SET NULL
);

-- 7. Thẻ Flashcard chi tiết
CREATE TABLE flashcards (
    id              BIGSERIAL PRIMARY KEY,
    set_id          BIGINT NOT NULL,
    term            TEXT NOT NULL,
    definition      TEXT NOT NULL,
    hint            VARCHAR(500) NULL,
    image_url       TEXT NULL,
    audio_url       TEXT NULL,
    concept_tag     VARCHAR(100) NULL,
    order_index     INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_flashcards_set_id FOREIGN KEY (set_id) REFERENCES flashcard_sets(id) ON DELETE CASCADE
);

-- 8. Tương tác người dùng với bộ thẻ (Đã lưu, Học gần đây, Đánh giá 1-5 sao, Resume đa nền tảng)
CREATE TABLE user_set_interactions (
    user_id             BIGINT NOT NULL,
    set_id              BIGINT NOT NULL,
    is_saved            BOOLEAN NOT NULL DEFAULT FALSE,
    last_study_mode     VARCHAR(30) NULL,
    last_accessed_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resume_state_json   JSONB NOT NULL DEFAULT '{}'::jsonb,
    rating              SMALLINT NULL CHECK (rating BETWEEN 1 AND 5),
    review_comment      VARCHAR(500) NULL,
    rated_at            TIMESTAMPTZ NULL,
    PRIMARY KEY (user_id, set_id),
    CONSTRAINT fk_usi_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_usi_set_id FOREIGN KEY (set_id) REFERENCES flashcard_sets(id) ON DELETE CASCADE
);

-- 9. Thẻ lẻ được gắn dấu sao (Starred Cards)
CREATE TABLE starred_cards (
    user_id         BIGINT NOT NULL,
    card_id         BIGINT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, card_id),
    CONSTRAINT fk_starred_cards_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_starred_cards_card_id FOREIGN KEY (card_id) REFERENCES flashcards(id) ON DELETE CASCADE
);

-- 10. Thư mục (Folders)
CREATE TABLE folders (
    id              BIGSERIAL PRIMARY KEY,
    owner_id        BIGINT NOT NULL,
    name            VARCHAR(150) NOT NULL,
    description     TEXT NULL,
    visibility      VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    share_token     VARCHAR(40) NOT NULL UNIQUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_folders_owner_id FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 11. Quan hệ N-N giữa Thư mục và Bộ thẻ
CREATE TABLE folder_sets (
    folder_id       BIGINT NOT NULL,
    set_id          BIGINT NOT NULL,
    added_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (folder_id, set_id),
    CONSTRAINT fk_folder_sets_folder_id FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE CASCADE,
    CONSTRAINT fk_folder_sets_set_id FOREIGN KEY (set_id) REFERENCES flashcard_sets(id) ON DELETE CASCADE
);

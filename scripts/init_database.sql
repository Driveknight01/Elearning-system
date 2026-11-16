-- =============================================================================
-- SmartFlash E-Learning System — Script Khởi tạo Cơ sở Dữ liệu PostgreSQL Local
-- Chạy bằng quyền superuser (postgres):
--   psql -U postgres -f scripts/init_database.sql
-- =============================================================================

-- 1. Tạo Database smartflash_db (nếu chạy trong psql)
SELECT 'CREATE DATABASE smartflash_db WITH ENCODING ''UTF8'' TEMPLATE template0'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'smartflash_db')\gexec

\c smartflash_db;

-- 2. Bật các PostgreSQL Extensions cần thiết cho tìm kiếm toàn văn (Full-text / Trigram) và UUID
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "unaccent";

-- 3. Thiết lập múi giờ mặc định UTC cho phiên làm việc chuẩn TIMESTAMPTZ
ALTER DATABASE smartflash_db SET timezone TO 'UTC';

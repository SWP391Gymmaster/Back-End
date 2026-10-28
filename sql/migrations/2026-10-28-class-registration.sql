USE swp391_group3;

-- Migration: đăng ký sớm có giờ + đăng ký lớp.
-- 1) registration_open_at / registration_close_at: DATE -> DATETIME (chạy lại nhiều lần vẫn an toàn).
-- 2) Bảng class_registrations (hội viên đăng ký LỚP trước khi đăng ký buổi học).

SET @open_type = (
    SELECT DATA_TYPE FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'classes' AND COLUMN_NAME = 'registration_open_at'
);
SET @close_type = (
    SELECT DATA_TYPE FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'classes' AND COLUMN_NAME = 'registration_close_at'
);

SET @sql = IF(@open_type = 'date',
    'ALTER TABLE classes MODIFY COLUMN registration_open_at DATETIME NULL;',
    'SELECT "registration_open_at is not DATE, skipped";'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(@close_type = 'date',
    'ALTER TABLE classes MODIFY COLUMN registration_close_at DATETIME NULL;',
    'SELECT "registration_close_at is not DATE, skipped";'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Dữ liệu cũ chỉ có ngày: giờ mở = 00:00 (đã đúng sau khi đổi kiểu), giờ đóng = hết ngày (23:59).
SET @sql = IF(@close_type = 'date',
    'UPDATE classes SET registration_close_at = registration_close_at + INTERVAL 1439 MINUTE WHERE registration_close_at IS NOT NULL;',
    'SELECT "registration_close_at already migrated, skipped";'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS class_registrations (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    member_id     BIGINT      NOT NULL,
    class_id      BIGINT      NOT NULL,
    registered_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_class_registration_member_class UNIQUE (member_id, class_id),
    INDEX idx_class_registration_class (class_id)
);
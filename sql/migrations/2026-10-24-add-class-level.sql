USE swp391_group3;

-- Migration B2: Bổ sung trường level cho bảng classes (BEGINNER, INTERMEDIATE, ADVANCED)
SET @has_level = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'classes'
      AND COLUMN_NAME = 'level'
);

SET @sql = IF(@has_level = 0,
    'ALTER TABLE classes ADD COLUMN level VARCHAR(20) NULL AFTER subject;',
    'SELECT "Column level already exists in classes";'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

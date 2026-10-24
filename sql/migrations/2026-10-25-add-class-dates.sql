USE swp391_group3;

-- Migration B2: Bổ sung ngày bắt đầu / ngày kết thúc của cả lớp (start_date, end_date) cho bảng classes.
-- Hai cột cho phép NULL để không ảnh hưởng các lớp đã có.

SET @has_start_date = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'classes'
      AND COLUMN_NAME = 'start_date'
);

SET @sql = IF(@has_start_date = 0,
    'ALTER TABLE classes ADD COLUMN start_date DATE NULL AFTER description;',
    'SELECT "Column start_date already exists in classes";'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_end_date = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'classes'
      AND COLUMN_NAME = 'end_date'
);

SET @sql = IF(@has_end_date = 0,
    'ALTER TABLE classes ADD COLUMN end_date DATE NULL AFTER start_date;',
    'SELECT "Column end_date already exists in classes";'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Lớp cũ đã có lịch: suy ra khoảng ngày từ các buổi học (chỉ điền khi đang để trống).
UPDATE classes c
JOIN (
    SELECT class_id, MIN(DATE(start_time)) AS first_day, MAX(DATE(end_time)) AS last_day
    FROM schedules
    GROUP BY class_id
) s ON s.class_id = c.id
SET c.start_date = COALESCE(c.start_date, s.first_day),
    c.end_date   = COALESCE(c.end_date, s.last_day);

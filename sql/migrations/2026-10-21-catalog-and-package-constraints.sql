-- Migration T5.1 & T5.4: Bổ sung mã định danh unique và trạng thái cho catalog gói tập, phòng, lớp.
-- Chạy trên database hiện có để cập nhật schema khớp với các entity Java mới.
USE swp391_group3;

-- 1. Cập nhật bảng membership_packages (T5.1)
-- Thêm cột package_code nếu chưa có
SET @has_pkg_code = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'membership_packages'
      AND COLUMN_NAME = 'package_code'
);

-- Thêm cột status nếu chưa có
SET @has_status = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'membership_packages'
      AND COLUMN_NAME = 'status'
);

-- Tự động sinh mã cho các dòng cũ chưa có package_code
UPDATE membership_packages
SET package_code = CONCAT('PKG-', id)
WHERE package_code IS NULL OR package_code = '';

-- Gán status = ACTIVE nếu chưa có
UPDATE membership_packages
SET status = 'ACTIVE'
WHERE status IS NULL OR status = '';

-- 2. Cập nhật bảng rooms (T5.4)
UPDATE rooms
SET room_code = CONCAT('ROOM-', id)
WHERE room_code IS NULL OR room_code = '';

-- 3. Cập nhật bảng classes (T5.4)
UPDATE classes
SET class_code = CONCAT('CLS-', id)
WHERE class_code IS NULL OR class_code = '';

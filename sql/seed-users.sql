-- MySQL. Chay SAU khi ung dung da chay it nhat 1 lan (de tu tao bang users).
-- Mat khau demo cua tat ca tai khoan: password123 (BCrypt hash, tuong thich BCryptPasswordEncoder)
-- INSERT IGNORE: bo qua neu email / user_name da ton tai (bang users co rang buoc unique).
USE swp391_group3;

INSERT IGNORE INTO users (user_name, full_name, email, password_hash, phone, role, enabled, email_verified) VALUES
('manager1',      'Demo Manager',      'manager@example.com',
 '$2a$12$AQBLfBFPxUBLiV9PkiEGL.hvDSMKSUHsS3/pefc/SXsM4fGDHFuai', '0900000001', 'MANAGER', 1, 1),
('coach1',        'Demo Coach',        'coach@example.com',
 '$2a$12$AQBLfBFPxUBLiV9PkiEGL.hvDSMKSUHsS3/pefc/SXsM4fGDHFuai', '0900000002', 'COACH', 1, 1),
('member1',       'Demo Member',       'member@example.com',
 '$2a$12$AQBLfBFPxUBLiV9PkiEGL.hvDSMKSUHsS3/pefc/SXsM4fGDHFuai', '0900000003', 'MEMBER', 1, 1),
('receptionist1', 'Demo Receptionist', 'receptionist@example.com',
 '$2a$12$AQBLfBFPxUBLiV9PkiEGL.hvDSMKSUHsS3/pefc/SXsM4fGDHFuai', '0900000004', 'RECEPTIONIST', 1, 1),
('coach_lan',     'Nguyễn Thị Lan',    'coach.lan@example.com',
 '$2a$12$AQBLfBFPxUBLiV9PkiEGL.hvDSMKSUHsS3/pefc/SXsM4fGDHFuai', '0900000005', 'COACH', 1, 1),
('coach_minh',    'Trần Quốc Minh',    'coach.minh@example.com',
 '$2a$12$AQBLfBFPxUBLiV9PkiEGL.hvDSMKSUHsS3/pefc/SXsM4fGDHFuai', '0900000006', 'COACH', 1, 1),
('coach_trang',   'Lê Thu Trang',      'coach.trang@example.com',
 '$2a$12$AQBLfBFPxUBLiV9PkiEGL.hvDSMKSUHsS3/pefc/SXsM4fGDHFuai', '0900000007', 'COACH', 1, 1),
('coach_hung',    'Phạm Văn Hùng',     'coach.hung@example.com',
 '$2a$12$AQBLfBFPxUBLiV9PkiEGL.hvDSMKSUHsS3/pefc/SXsM4fGDHFuai', '0900000008', 'COACH', 1, 1);

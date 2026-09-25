-- MySQL. Chay SAU khi ung dung da chay it nhat 1 lan (de tu tao bang users).
-- Mat khau demo cua ca 4 tai khoan: password123 (BCrypt hash, tuong thich BCryptPasswordEncoder)
-- INSERT IGNORE: bo qua neu email / user_name da ton tai (bang users co rang buoc unique).
USE swp391_group3;

INSERT IGNORE INTO users (user_name, full_name, email, password_hash, phone, role) VALUES
('manager1',      'Demo Manager',      'manager@example.com',
 '$2a$12$AQBLfBFPxUBLiV9PkiEGL.hvDSMKSUHsS3/pefc/SXsM4fGDHFuai', '0900000001', 'MANAGER'),
('coach1',        'Demo Coach',        'coach@example.com',
 '$2a$12$AQBLfBFPxUBLiV9PkiEGL.hvDSMKSUHsS3/pefc/SXsM4fGDHFuai', '0900000002', 'COACH'),
('member1',       'Demo Member',       'member@example.com',
 '$2a$12$AQBLfBFPxUBLiV9PkiEGL.hvDSMKSUHsS3/pefc/SXsM4fGDHFuai', '0900000003', 'MEMBER'),
('receptionist1', 'Demo Receptionist', 'receptionist@example.com',
 '$2a$12$AQBLfBFPxUBLiV9PkiEGL.hvDSMKSUHsS3/pefc/SXsM4fGDHFuai', '0900000004', 'RECEPTIONIST');

-- Run after the application has created the users table in swp391_gym.
-- Demo password for all four accounts: password123
-- The value below is a BCrypt hash (cost 12), compatible with BCryptPasswordEncoder.
USE swp391_gym;
GO

BEGIN TRANSACTION;

IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'manager@example.com' OR user_name = 'manager1')
    INSERT INTO users (user_name, full_name, email, password_hash, phone, role)
    VALUES ('manager1', 'Demo Manager', 'manager@example.com',
            '$2a$12$AQBLfBFPxUBLiV9PkiEGL.hvDSMKSUHsS3/pefc/SXsM4fGDHFuai', '0900000001', 'MANAGER');

IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'coach@example.com' OR user_name = 'coach1')
    INSERT INTO users (user_name, full_name, email, password_hash, phone, role)
    VALUES ('coach1', 'Demo Coach', 'coach@example.com',
            '$2a$12$AQBLfBFPxUBLiV9PkiEGL.hvDSMKSUHsS3/pefc/SXsM4fGDHFuai', '0900000002', 'COACH');

IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'member@example.com' OR user_name = 'member1')
    INSERT INTO users (user_name, full_name, email, password_hash, phone, role)
    VALUES ('member1', 'Demo Member', 'member@example.com',
            '$2a$12$AQBLfBFPxUBLiV9PkiEGL.hvDSMKSUHsS3/pefc/SXsM4fGDHFuai', '0900000003', 'MEMBER');

IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'receptionist@example.com' OR user_name = 'receptionist1')
    INSERT INTO users (user_name, full_name, email, password_hash, phone, role)
    VALUES ('receptionist1', 'Demo Receptionist', 'receptionist@example.com',
            '$2a$12$AQBLfBFPxUBLiV9PkiEGL.hvDSMKSUHsS3/pefc/SXsM4fGDHFuai', '0900000004', 'RECEPTIONIST');

COMMIT TRANSACTION;
GO

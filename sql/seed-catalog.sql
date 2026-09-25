-- Du lieu mau cho goi tap, phong, lop va lich (de demo).
-- Chay SAU khi: (1) ung dung da chay it nhat 1 lan de tu tao bang,
--               (2) da chay seed-users.sql (can tai khoan coach@example.com).
USE swp391_group3;
GO

BEGIN TRANSACTION;

IF NOT EXISTS (SELECT 1 FROM membership_packages)
    INSERT INTO membership_packages (name, description, price, duration_days, active) VALUES
    (N'Gói 1 tháng',  N'Tập không giới hạn khu Gym',              500000,  30,  1),
    (N'Gói 3 tháng',  N'Tiết kiệm 10%, tham gia 2 lớp nhóm',     1350000, 90,  1),
    (N'Gói 12 tháng', N'Tiết kiệm 20%, không giới hạn lớp nhóm', 4800000, 365, 1);

IF NOT EXISTS (SELECT 1 FROM rooms)
    INSERT INTO rooms (name, capacity) VALUES
    (N'Phòng Gym A', 40),
    (N'Phòng Yoga B', 20),
    (N'Phòng Boxing C', 15);

DECLARE @coachId BIGINT = (SELECT id FROM users WHERE email = 'coach@example.com');

IF NOT EXISTS (SELECT 1 FROM classes)
    INSERT INTO classes (name, subject, description, capacity, coach_id) VALUES
    (N'Boxing cơ bản', N'Boxing', N'Lớp Boxing cho người mới', 15, @coachId);

-- 1 buoi Boxing vao 18:00-19:30 ngay mai
IF NOT EXISTS (SELECT 1 FROM schedules)
    INSERT INTO schedules (class_id, coach_id, room_id, start_time, end_time)
    SELECT c.id, @coachId, r.id,
           DATEADD(MINUTE, 18 * 60,      CAST(CAST(DATEADD(DAY, 1, GETDATE()) AS DATE) AS DATETIME2)),
           DATEADD(MINUTE, 19 * 60 + 30, CAST(CAST(DATEADD(DAY, 1, GETDATE()) AS DATE) AS DATETIME2))
    FROM classes c CROSS JOIN rooms r
    WHERE c.name = N'Boxing cơ bản' AND r.name = N'Phòng Boxing C';

COMMIT TRANSACTION;
GO

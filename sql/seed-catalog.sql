-- MySQL. Du lieu mau cho goi tap, phong, lop va lich (de demo).
-- Chay SAU khi: (1) ung dung da chay it nhat 1 lan de tu tao bang,
--               (2) da chay seed-users.sql (can tai khoan coach@example.com).
-- Moi bang chi duoc nap khi dang trong, nen chay lai nhieu lan cung khong bi trung.
USE swp391_group3;

INSERT INTO membership_packages (name, description, price, duration_days, active)
SELECT * FROM (
    SELECT 'Gói 1 tháng' AS name, 'Tập không giới hạn khu Gym' AS description, 500000 AS price, 30 AS duration_days, 1 AS active
    UNION ALL SELECT 'Gói 3 tháng',  'Tiết kiệm 10%, tham gia 2 lớp nhóm',     1350000, 90,  1
    UNION ALL SELECT 'Gói 12 tháng', 'Tiết kiệm 20%, không giới hạn lớp nhóm', 4800000, 365, 1
) AS t
WHERE NOT EXISTS (SELECT 1 FROM membership_packages);

INSERT INTO rooms (name, capacity)
SELECT * FROM (
    SELECT 'Phòng Gym A' AS name, 40 AS capacity
    UNION ALL SELECT 'Phòng Yoga B', 20
    UNION ALL SELECT 'Phòng Boxing C', 15
) AS t
WHERE NOT EXISTS (SELECT 1 FROM rooms);

SET @coachId = (SELECT id FROM users WHERE email = 'coach@example.com');
SET @memberId = (SELECT id FROM users WHERE email = 'member@example.com');
SET @demoPackageId = (SELECT id FROM membership_packages ORDER BY duration_days LIMIT 1);
SET @demoDurationDays = (SELECT duration_days FROM membership_packages WHERE id = @demoPackageId);

-- Demo member package validity for class enrollment APIs.
INSERT INTO member_memberships (member_id, package_id, start_date, end_date)
SELECT @memberId, @demoPackageId, CURDATE(), CURDATE() + INTERVAL (@demoDurationDays - 1) DAY
FROM DUAL
WHERE @memberId IS NOT NULL
  AND @demoPackageId IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM member_memberships
      WHERE member_id = @memberId
        AND package_id = @demoPackageId
        AND start_date = CURDATE()
  );

INSERT INTO classes (name, subject, description, capacity, coach_id)
SELECT 'Boxing cơ bản', 'Boxing', 'Lớp Boxing cho người mới', 15, @coachId
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM classes);

-- 1 buoi Boxing 18:00-19:30 ngay mai
INSERT INTO schedules (class_id, coach_id, room_id, start_time, end_time)
SELECT c.id, @coachId, r.id,
       TIMESTAMP(CURDATE() + INTERVAL 1 DAY, '18:00:00'),
       TIMESTAMP(CURDATE() + INTERVAL 1 DAY, '19:30:00')
FROM classes c CROSS JOIN rooms r
WHERE c.name = 'Boxing cơ bản' AND r.name = 'Phòng Boxing C'
  AND NOT EXISTS (SELECT 1 FROM schedules);

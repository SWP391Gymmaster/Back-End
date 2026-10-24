-- MySQL. Du lieu mau cho goi tap, phong, lop va lich (de demo).
-- Chay SAU khi: (1) ung dung da chay it nhat 1 lan de tu tao bang,
--               (2) da chay seed-users.sql (can tai khoan coach@example.com).
-- Chay lai nhieu lan khong bi trung (goi tap, phong, lop kiem tra theo ten; lich kiem tra theo lop + gio bat dau).
USE swp391_group3;

-- Goi tap theo bang gia chinh sach CITIGYM (citigym.asia/chinh-sach-gia):
-- Bronze = chi Gym 1 CLB; Silver = Gym + Yoga/Group X 1 CLB; Gold = toan he thong + Yoga/Group X.
-- Moi goi chi them neu chua co ma/ten do (T5.1).
INSERT INTO membership_packages (package_code, name, description, price, duration_days, status)
SELECT t.package_code, t.name, t.description, t.price, t.duration_days, 'ACTIVE' FROM (
    SELECT 'PKG-BRONZE-1M' AS package_code, 'Bronze 1 tháng' AS name, 'Khu Gym tại 1 CLB, tủ đồ, khăn, nước uống miễn phí' AS description, 1000000 AS price, 30 AS duration_days
    UNION ALL SELECT 'PKG-BRONZE-3M',  'Bronze 3 tháng',  'Khu Gym tại 1 CLB (900.000đ/tháng)',                      2700000, 90
    UNION ALL SELECT 'PKG-BRONZE-6M',  'Bronze 6 tháng',  'Khu Gym tại 1 CLB (800.000đ/tháng)',                      4800000, 180
    UNION ALL SELECT 'PKG-BRONZE-12M', 'Bronze 12 tháng', 'Khu Gym tại 1 CLB (600.000đ/tháng)',                      7200000, 365
    UNION ALL SELECT 'PKG-SILVER-1M',  'Silver 1 tháng',  'Gym + Yoga + Group X tại 1 CLB',                          1250000, 30
    UNION ALL SELECT 'PKG-SILVER-3M',  'Silver 3 tháng',  'Gym + Yoga + Group X tại 1 CLB (1.125.000đ/tháng)',       3375000, 90
    UNION ALL SELECT 'PKG-SILVER-6M',  'Silver 6 tháng',  'Gym + Yoga + Group X tại 1 CLB (1.000.000đ/tháng)',       6000000, 180
    UNION ALL SELECT 'PKG-SILVER-12M', 'Silver 12 tháng', 'Gym + Yoga + Group X tại 1 CLB (750.000đ/tháng)',         9000000, 365
    UNION ALL SELECT 'PKG-GOLD-1M',    'Gold 1 tháng',    'Tập toàn hệ thống + Yoga + Group X',                      1400000, 30
    UNION ALL SELECT 'PKG-GOLD-3M',    'Gold 3 tháng',    'Tập toàn hệ thống + Yoga + Group X (1.260.000đ/tháng)',   3780000, 90
    UNION ALL SELECT 'PKG-GOLD-6M',    'Gold 6 tháng',    'Tập toàn hệ thống + Yoga + Group X (1.120.000đ/tháng)',   6720000, 180
    UNION ALL SELECT 'PKG-GOLD-12M',   'Gold 12 tháng',   'Tập toàn hệ thống + Yoga + Group X (840.000đ/tháng)',    10080000, 365
) AS t
WHERE NOT EXISTS (SELECT 1 FROM membership_packages m WHERE m.package_code = t.package_code OR m.name = t.name);

-- Khu tap (CITIGYM co cac khu: Gym, Yoga, Group X, Cycling). Moi phong chi them neu chua co ma/ten do (T5.4).
INSERT INTO rooms (room_code, name, capacity, type)
SELECT t.room_code, t.name, t.capacity, t.type FROM (
    SELECT 'ROOM-GYM-A' AS room_code, 'Phòng Gym A' AS name, 120 AS capacity, 'GYM' AS type
    UNION ALL SELECT 'ROOM-YOGA-B', 'Phòng Yoga B',     30, 'YOGA'
    UNION ALL SELECT 'ROOM-BOX-C',   'Phòng Boxing C',   15, 'BOXING'
    UNION ALL SELECT 'ROOM-GROUP-X', 'Studio Group X',   40, 'GROUP_X'
    UNION ALL SELECT 'ROOM-CYCLE',   'Phòng Cycling',    30, 'CYCLING'
) AS t
WHERE NOT EXISTS (SELECT 1 FROM rooms r WHERE r.room_code = t.room_code OR r.name = t.name);

-- Phong tao truoc khi co cot type: suy ra tu ten, con lai la OTHER
UPDATE rooms SET type = CASE
    WHEN name LIKE '%Gym%'    THEN 'GYM'
    WHEN name LIKE '%Yoga%'   THEN 'YOGA'
    WHEN name LIKE '%Boxing%' THEN 'BOXING'
    WHEN name LIKE '%Group X%' THEN 'GROUP_X'
    WHEN name LIKE '%Cycling%' THEN 'CYCLING'
    WHEN name LIKE '%Cardio%' THEN 'CARDIO'
    ELSE 'OTHER' END
WHERE type IS NULL;

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

-- Lop hoc (capacity <= suc chua phong). Coach phu trach mac dinh theo email (T5.4, B2).
-- start_date / end_date: mỗi lớp chạy một khóa 3 tháng kể từ hôm nay (chạy migration 2026-10-25-add-class-dates.sql trước).
INSERT INTO classes (class_code, name, subject, level, description, start_date, end_date, capacity, coach_id)
SELECT t.class_code, t.name, t.subject, t.level, t.description,
       CURDATE(), CURDATE() + INTERVAL 3 MONTH, t.capacity, u.id FROM (
    SELECT 'CLS-BOX-01' AS class_code, 'Boxing cơ bản' AS name, 'Boxing' AS subject, 'BEGINNER' AS level, 'Lớp Boxing cho người mới' AS description, 15 AS capacity, 'coach@example.com' AS coach_email
    UNION ALL SELECT 'CLS-BOX-02', 'Boxing nâng cao',      'Boxing',   'ADVANCED',     'Sparring và kỹ thuật phối hợp đòn',       12, 'coach@example.com'
    UNION ALL SELECT 'CLS-YOGA-01', 'Yoga Flow buổi sáng',  'Yoga',     'BEGINNER',     'Yoga vinyasa nhẹ nhàng đón ngày mới',     30, 'coach.lan@example.com'
    UNION ALL SELECT 'CLS-YOGA-02', 'Yoga thư giãn',        'Yoga',     'INTERMEDIATE', 'Yoga phục hồi, giãn cơ sau giờ làm',      30, 'coach.lan@example.com'
    UNION ALL SELECT 'CLS-GYM-01', 'Strength & Conditioning', 'Gym',   'INTERMEDIATE', 'Tập tạ có hướng dẫn, tăng sức mạnh',      30, 'coach.minh@example.com'
    UNION ALL SELECT 'CLS-GX-01', 'Body Pump',            'Group X',  'INTERMEDIATE', 'Tạ nhẹ theo nhạc, săn chắc toàn thân (60 phút)', 40, 'coach.trang@example.com'
    UNION ALL SELECT 'CLS-GX-02', 'Body Combat',          'Group X',  'ADVANCED',     'Võ thuật theo nhạc, đốt calo (60 phút)',  35, 'coach.trang@example.com'
    UNION ALL SELECT 'CLS-GX-03', 'Zumba',                'Group X',  'BEGINNER',     'Nhảy aerobic theo nhạc Latin',            40, 'coach.trang@example.com'
    UNION ALL SELECT 'CLS-GX-04', 'Bums & Tums',          'Group X',  'BEGINNER',     'Chuyên mông và bụng',                     30, 'coach.trang@example.com'
    UNION ALL SELECT 'CLS-CYCLE-01', 'RPM Cycling',          'Cycling',  'INTERMEDIATE', 'Đạp xe trong nhà theo nhạc',              30, 'coach.hung@example.com'
) AS t
JOIN users u ON u.email = t.coach_email
WHERE NOT EXISTS (SELECT 1 FROM classes c WHERE c.class_code = t.class_code OR c.name = t.name);

-- Lich 7 ngay toi. step=1: moi ngay, step=2: ngay cach ngay.
-- Khong trung gio trong cung phong / cung coach; chay lai khong tao trung (kiem tra class + gio bat dau).
INSERT INTO schedules (class_id, coach_id, room_id, start_time, end_time)
SELECT c.id, u.id, r.id,
       TIMESTAMP(CURDATE() + INTERVAL d.n DAY, t.start_at),
       TIMESTAMP(CURDATE() + INTERVAL d.n DAY, t.end_at)
FROM (
    SELECT 'Strength & Conditioning' AS class_name, 'Phòng Gym A' AS room_name, 'coach.minh@example.com' AS coach_email, '06:00:00' AS start_at, '07:00:00' AS end_at, 1 AS step
    UNION ALL SELECT 'Strength & Conditioning', 'Phòng Gym A',    'coach.minh@example.com',  '18:30:00', '19:30:00', 1
    UNION ALL SELECT 'Yoga Flow buổi sáng',     'Phòng Yoga B',   'coach.lan@example.com',   '07:00:00', '08:00:00', 1
    UNION ALL SELECT 'Yoga thư giãn',           'Phòng Yoga B',   'coach.lan@example.com',   '19:00:00', '20:00:00', 2
    UNION ALL SELECT 'Boxing cơ bản',           'Phòng Boxing C', 'coach@example.com',       '18:00:00', '19:30:00', 1
    UNION ALL SELECT 'Boxing nâng cao',         'Phòng Boxing C', 'coach@example.com',       '20:00:00', '21:30:00', 2
    UNION ALL SELECT 'Zumba',                   'Studio Group X', 'coach.trang@example.com', '06:30:00', '07:30:00', 2
    UNION ALL SELECT 'Bums & Tums',             'Studio Group X', 'coach.trang@example.com', '12:00:00', '12:45:00', 2
    UNION ALL SELECT 'Body Pump',               'Studio Group X', 'coach.trang@example.com', '18:00:00', '19:00:00', 1
    UNION ALL SELECT 'Body Combat',             'Studio Group X', 'coach.trang@example.com', '19:15:00', '20:15:00', 2
    UNION ALL SELECT 'RPM Cycling',             'Phòng Cycling',  'coach.hung@example.com',  '06:30:00', '07:15:00', 1
    UNION ALL SELECT 'RPM Cycling',             'Phòng Cycling',  'coach.hung@example.com',  '18:30:00', '19:15:00', 1
) AS t
JOIN classes c ON c.name = t.class_name
JOIN rooms r   ON r.name = t.room_name
JOIN users u   ON u.email = t.coach_email
JOIN (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
) AS d ON MOD(d.n, t.step) = 0
WHERE NOT EXISTS (
    SELECT 1 FROM schedules s
    WHERE s.class_id = c.id
      AND s.start_time = TIMESTAMP(CURDATE() + INTERVAL d.n DAY, t.start_at)
);

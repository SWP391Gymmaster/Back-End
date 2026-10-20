# Gym Master – Back-End

Back-End cho dự án SWP391 Gym Master (Spring Boot 4, Java 21, MySQL 8).

Repo này được gộp từ 2 repo cá nhân, **giữ nguyên lịch sử commit**:
- [SWP391_BE](https://github.com/huynhhuuphuocpro9-sketch/SWP391_BE): đăng ký, đăng nhập, JWT, phân quyền
- [sports-center-backend](https://github.com/vduyv1011/sports-center-backend): gói thành viên, phòng, lớp, lịch

## Cần cài
- JDK 21
- MySQL Server 8 (MySQL Community Server) + MySQL Workbench để xem dữ liệu. Ghi nhớ mật khẩu `root` đặt lúc cài.

## Chạy dự án
1. Nếu mật khẩu `root` trên máy bạn khác `123`, đặt biến môi trường `DB_PASSWORD` (hoặc sửa tạm trong `application.properties`, **đừng commit mật khẩu thật**).
2. Chạy `Swp391BeApplication` (hoặc `mvnw spring-boot:run`). Database `swp391_group3` và các bảng sẽ được tự động tạo.
3. Mở http://localhost:8080/api/test, thấy `Backend is running!` là thành công.
4. Nạp dữ liệu mẫu trong MySQL Workbench (File → Open SQL Script → bấm ⚡ Execute), theo thứ tự:
   - `sql/seed-users.sql`: 4 tài khoản (mật khẩu `password123`)
   - `sql/seed-catalog.sql`: gói tập, phòng, lớp, lịch mẫu

| Vai trò | Email |
|---|---|
| Center Manager | manager@example.com |
| Receptionist | receptionist@example.com |
| Coach | coach@example.com |
| Member | member@example.com |

## API
Các API có dấu 🔒 cần header `Authorization: Bearer <token>` (token lấy từ `/auth/login`).

| Method | Đường dẫn | Quyền |
|---|---|---|
| POST | `/auth/register` | Công khai (tạo tài khoản MEMBER) |
| POST | `/auth/login` | Công khai, trả về `token, email, fullName, role, userId` |
| GET | `/api/membership-packages`, `/api/membership-packages/{id}` | Công khai |
| GET | `/api/rooms`, `/api/classes`, `/api/schedules`, `/api/coaches` | 🔒 Đã đăng nhập |
| GET | `/api/schedules?coachId=..` hoặc `?classId=..` | 🔒 Đã đăng nhập |
| POST / PUT / DELETE | `/api/membership-packages`, `/api/rooms`, `/api/classes`, `/api/schedules` | 🔒 MANAGER |

Ví dụ tạo lịch (bỏ trống `coachId` thì lấy Coach đang phụ trách lớp):
```json
POST /api/schedules
{ "classId": 1, "roomId": 3, "coachId": 3, "startTime": "2026-09-29T18:00:00", "endTime": "2026-09-29T19:30:00" }
```
Nếu Coach hoặc phòng đã có lịch giao nhau, API trả về `400` kèm `message`.

Mã lỗi chung: `400` dữ liệu sai / vi phạm nghiệp vụ, `401` chưa đăng nhập, `403` không đủ quyền, `404` không tìm thấy, `409` email hoặc username đã tồn tại.

### Account verification and admin users

Public registration creates an unverified MEMBER and returns no JWT. The user verifies through `POST /auth/verify-email` with `{ "token": "..." }`; `POST /auth/resend-verification` returns a generic `202` response. Verification links are single-use and expire after 24 hours. Correct credentials for an unverified account return `403 EMAIL_NOT_VERIFIED`.

ADMIN-only endpoints manage non-admin accounts:

| Method | Path | Action |
|---|---|---|
| GET | `/api/admin/users?page=0&size=20&q=&role=&enabled=&emailVerified=` | Search/filter users; ADMIN accounts are excluded |
| GET | `/api/admin/users/{id}` | View a managed user |
| POST | `/api/admin/users` | Create a managed user; verification required |
| PATCH | `/api/admin/users/{id}` | Update profile, email, or role |
| DELETE | `/api/admin/users/{id}` | Deactivate and preserve business history |
| PATCH | `/api/admin/users/{id}/status` | `{ "enabled": true }` to enable or disable |
| POST | `/api/admin/users/{id}/verification-email` | Resend verification email |

Email changes require verification again. Configure `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `MAIL_FROM`, and `FRONTEND_BASE_URL` for email delivery. In production use the `production` profile and an HTTPS frontend URL. Admin-created users receive no password email; deliver the initial password over a separate trusted channel. Provision an ADMIN using `sql/provision-admin.sql` with operator-supplied values.

Before exposing public authentication endpoints, configure a trusted reverse proxy with per-IP limits (registration 10/hour, resend 20/hour, verification 60/minute, login 10/minute) and request body limits. Trust forwarded client IP headers only from that proxy; per-account email quotas do not replace these limits.

For an existing database, inspect and resolve conflicts with `sql/migrations/2026-10-20-user-management-preflight.sql`, back up the database, then run `sql/migrations/2026-10-20-user-management.sql` once before starting the updated application. Existing accounts are marked verified for compatibility. Existing JWT sessions must log in again because tokens now identify immutable user IDs.

### Member class enrollment

Member endpoints:

| Method | Path | Role |
|---|---|---|
| GET | `/api/member/class-sessions/available` | MEMBER |
| GET | `/api/member/class-enrollments` | MEMBER |
| POST | `/api/member/class-sessions/{scheduleId}/enrollment` | MEMBER |
| DELETE | `/api/member/class-sessions/{scheduleId}/enrollment` | MEMBER |

`MembershipPackage` is the package catalog. A member's usable period is stored in `member_memberships`. A package is valid for enrollment/cancellation only when `start_date <= today <= end_date` and the class session date is also inside the same date range.

Available session response fields include `scheduleId`, `classId`, `className`, `subject`, `coachId`, `roomId`, `startTime`, `endTime`, `capacity`, `registeredCount`, `remainingSlots`, and `registeredByMe`.

Acceptance checks:
- Valid package and available seat: `POST /api/member/class-sessions/{scheduleId}/enrollment` returns `201`.
- Valid package and own enrollment: `DELETE /api/member/class-sessions/{scheduleId}/enrollment` returns `204`.
- Expired or uncovered package: POST/DELETE returns `400` with `Membership package is expired or does not cover this class session`.
- Full class session: POST returns `400` with `Class session is full`.
- Wrong role: member routes return `403`.

PowerShell example:

```powershell
$login = Invoke-RestMethod -Uri "http://localhost:8080/auth/login" -Method Post -ContentType "application/json" -Body '{"email":"member@example.com","password":"password123"}'
$headers = @{ Authorization = "Bearer $($login.token)" }
Invoke-RestMethod -Uri "http://localhost:8080/api/member/class-sessions/available" -Headers $headers
Invoke-RestMethod -Uri "http://localhost:8080/api/member/class-sessions/1/enrollment" -Method Post -Headers $headers
Invoke-WebRequest -Uri "http://localhost:8080/api/member/class-sessions/1/enrollment" -Method Delete -Headers $headers
```

## Front-End
Front-End (Vite) chạy ở http://localhost:5173 đã được cho phép gọi API (CORS). Muốn thêm địa chỉ khác, đặt biến môi trường `CORS_ORIGINS`.

## Chưa có (cần làm tiếp)
- Mua / gia hạn gói tập (membership của hội viên), thanh toán, hóa đơn
- Lễ tân đăng ký hội viên tại quầy, tra cứu hội viên
- Điểm danh, kế hoạch tập luyện, thông báo

# Gym Master – Back-End

Back-End cho dự án SWP391 Gym Master (Spring Boot 4, Java 21, SQL Server).

Repo này được gộp từ 2 repo cá nhân, **giữ nguyên lịch sử commit**:
- [SWP391_BE](https://github.com/huynhhuuphuocpro9-sketch/SWP391_BE): đăng ký, đăng nhập, JWT, phân quyền
- [sports-center-backend](https://github.com/vduyv1011/sports-center-backend): gói thành viên, phòng, lớp, lịch

## Cần cài
- JDK 21
- SQL Server 2022 Express + SSMS (bật Mixed Mode, bật TCP/IP cổng 1433)

## Chạy dự án
1. Trong SSMS tạo database: `CREATE DATABASE swp391_group3;`
2. Nếu mật khẩu `sa` trên máy bạn khác `123`, đặt biến môi trường `DB_PASSWORD` (hoặc sửa tạm trong `application.properties`, **đừng commit mật khẩu thật**).
3. Chạy `Swp391BeApplication` (hoặc `mvnw spring-boot:run`). Các bảng sẽ được tự động tạo.
4. Mở http://localhost:8080/api/test, thấy `Backend is running!` là thành công.
5. Nạp dữ liệu mẫu trong SSMS, theo thứ tự:
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

## Front-End
Front-End (Vite) chạy ở http://localhost:5173 đã được cho phép gọi API (CORS). Muốn thêm địa chỉ khác, đặt biến môi trường `CORS_ORIGINS`.

## Chưa có (cần làm tiếp)
- Mua / gia hạn gói tập (membership của hội viên), thanh toán, hóa đơn
- Lễ tân đăng ký hội viên tại quầy, tra cứu hội viên
- Điểm danh, kế hoạch tập luyện, thông báo

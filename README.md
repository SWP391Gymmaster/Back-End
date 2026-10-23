# Gym Master – Back-End

Back-End cho dự án SWP391 Gym Master (Spring Boot 4, Java 21, MySQL 8).

---

## 1. Yêu cầu môi trường & Cài đặt
- **JDK 21** trở lên.
- **MySQL Server 8** (MySQL Community Server) + **MySQL Workbench**.
- Ghi nhớ mật khẩu `root` đặt lúc cài đặt MySQL.

---

## 2. Cấu hình biến môi trường
Các cấu hình có thể ghi đè qua biến môi trường hoặc cấu hình trong `src/main/resources/application.properties`:

| Biến môi trường | Mặc định | Mô tả |
|---|---|---|
| `DB_USERNAME` | `root` | Tên tài khoản MySQL |
| `DB_PASSWORD` | `1234` | Mật khẩu tài khoản MySQL |
| `JWT_SECRET` | *(base64 key 256-bit)* | Khóa bí mật ký JWT |
| `CORS_ORIGINS` | `http://localhost:5173` | Danh sách origin Front-End được phép gọi API (ngăn cách bằng dấu phẩy) |
| `FRONTEND_BASE_URL` | `http://localhost:5173` | URL gốc Front-End (dùng cho link kích hoạt tài khoản trong email) |
| `SMTP_HOST` | *(trống)* | Host SMTP gửi mail (vd: `smtp.gmail.com`) |
| `SMTP_PORT` | `587` | Cổng SMTP |
| `SMTP_USERNAME` | *(trống)* | Email gửi |
| `SMTP_PASSWORD` | *(trống)* | Mật khẩu ứng dụng (App Password) |
| `MAIL_FROM` | `no-reply@example.com` | Email người gửi hiển thị |
| `PAYOS_CLIENT_ID` | `DEMO_CLIENT_ID` | Client ID từ PayOS Dashboard (https://my.payos.vn) |
| `PAYOS_API_KEY` | `DEMO_API_KEY` | API Key kết nối cổng PayOS |
| `PAYOS_CHECKSUM_KEY` | `DEMO_CHECKSUM_KEY` | Checksum Key dùng để xác thực chữ ký Webhook & giao dịch |
| `PAYOS_RETURN_URL` | `http://localhost:8080/api/payos/callback` | Return URL redirect người dùng về sau khi thanh toán thành công |
| `PAYOS_CANCEL_URL` | `http://localhost:8080/api/payos/cancel` | Cancel URL redirect người dùng về khi bấm hủy thanh toán |

---

## 3. Khởi chạy dự án
1. Đặt biến môi trường `DB_PASSWORD` nếu mật khẩu `root` máy bạn khác `1234`.
2. Chạy ứng dụng qua terminal hoặc IDE:
   ```bash
   ./mvnw spring-boot:run
   ```
   Database `swp391_group3` và toàn bộ bảng sẽ tự động được tạo.
3. Mở http://localhost:8080/api/test, nhận `Backend is running!` là thành công.
4. Nạp dữ liệu mẫu trong MySQL Workbench (File → Open SQL Script → bấm ⚡ Execute), theo thứ tự:
   - `sql/seed-users.sql`: Tài khoản mẫu các vai trò (mật khẩu mặc định: `password123`).
   - `sql/seed-catalog.sql`: Gói tập, phòng, lớp, lịch tập mẫu.

### Tài khoản mẫu thử nghiệm
| Vai trò | Email | Mật khẩu |
|---|---|---|
| Center Manager | `manager@example.com` | `password123` |
| Receptionist | `receptionist@example.com` | `password123` |
| Coach | `coach@example.com` | `password123` |
| Member | `member@example.com` | `password123` |

---

## 4. Danh mục API & Phân quyền
Các API có ký hiệu 🔒 yêu cầu header `Authorization: Bearer <token>` (lấy từ `/auth/login`).

### 4.1. Xác thực & Tài khoản (`/auth`)
| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| POST | `/auth/register` | Public | Đăng ký tài khoản MEMBER (cần xác thực email) |
| POST | `/auth/verify-email` | Public | Xác thực email bằng token một lần |
| POST | `/auth/resend-verification`| Public | Gửi lại email xác thực |
| POST | `/auth/login` | Public | Đăng nhập nhận JWT token và thông tin user |

### 4.2. Quản trị người dùng (`/api/admin/users`) – Quyền ADMIN
| Method | Đường dẫn | Mô tả |
|---|---|---|
| GET | `/api/admin/users` | Phân trang, tìm kiếm, lọc theo vai trò, trạng thái |
| GET | `/api/admin/users/{id}` | Xem chi tiết thông tin người dùng |
| POST | `/api/admin/users` | Tạo tài khoản nhân sự (MANAGER, RECEPTIONIST, COACH) |
| PATCH | `/api/admin/users/{id}` | Cập nhật hồ sơ, email, phân quyền |
| PATCH | `/api/admin/users/{id}/status` | Kích hoạt / vô hiệu hóa tài khoản |
| DELETE | `/api/admin/users/{id}` | Vô hiệu hóa và bảo toàn lịch sử giao dịch |
| POST | `/api/admin/users/{id}/verification-email` | Gửi lại email kích hoạt cho user |

### 4.3. Catalog Gói tập (`/api/membership-packages`)
- **Tách biệt mô hình**: `MembershipPackage` (định nghĩa gói sản phẩm, mã định danh `packageCode` duy nhất) và `MemberMembership` (hội viên sở hữu gói trong khoảng thời gian cụ thể).
- **Quy tắc nghiệp vụ**: Kiểm tra trùng tên/mã khi tạo mới; **khóa sửa/xóa** đối với gói đã phát sinh giao dịch/hóa đơn, chỉ cho phép chuyển sang trạng thái tạm ngưng (`SUSPENDED`).

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| GET | `/api/membership-packages` | Public / 🔒 | Lấy danh sách toàn bộ gói tập |
| GET | `/api/membership-packages/active` | Public / 🔒 | Lấy danh sách các gói đang mở bán (`ACTIVE`) |
| GET | `/api/membership-packages/{id}` | Public / 🔒 | Chi tiết gói tập theo ID |
| GET | `/api/membership-packages/by-code/{code}` | Public / 🔒 | Tra cứu gói tập theo mã định danh `packageCode` |
| POST | `/api/membership-packages` | 🔒 MANAGER | Tạo gói tập mới (bắt buộc `packageCode` duy nhất) |
| PUT | `/api/membership-packages/{id}` | 🔒 MANAGER | Cập nhật gói (chặn cập nhật giá/thời hạn nếu đã có giao dịch) |
| DELETE | `/api/membership-packages/{id}` | 🔒 MANAGER | Xóa gói (chặn xóa nếu đã có hóa đơn liên kết) |
| PATCH | `/api/membership-packages/{id}/suspend` | 🔒 MANAGER | Tạm ngưng kinh doanh gói (`SUSPENDED`) |
| PATCH | `/api/membership-packages/{id}/activate` | 🔒 MANAGER | Mở bán lại gói tập (`ACTIVE`) |

### 4.4. Phòng tập (`/api/rooms`) & Lớp tập (`/api/classes`)
- Nhận dạng duy nhất bằng mã: `roomCode` cho phòng (ví dụ: `ROOM-GYM-A`) và `classCode` cho lớp (ví dụ: `CLS-YOGA-01`).
- Ràng buộc lịch: Kiểm tra phòng và huấn luyện viên không bị trùng lịch trước khi lưu lịch tập.

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| GET | `/api/rooms` | 🔒 Đã đăng nhập | Danh sách phòng tập (kèm thống kê số lớp) |
| GET | `/api/rooms/{id}` | 🔒 Đã đăng nhập | Chi tiết phòng theo ID |
| GET | `/api/rooms/by-code/{code}` | 🔒 Đã đăng nhập | Tra cứu phòng theo `roomCode` |
| POST / PUT / DELETE | `/api/rooms` | 🔒 MANAGER | Thêm, sửa, xóa phòng tập (kiểm tra unique `roomCode`) |
| GET | `/api/classes` | 🔒 Đã đăng nhập | Danh sách lớp tập |
| GET | `/api/classes/{id}` | 🔒 Đã đăng nhập | Chi tiết lớp theo ID |
| GET | `/api/classes/by-code/{code}` | 🔒 Đã đăng nhập | Tra cứu lớp theo `classCode` |
| POST / PUT / DELETE | `/api/classes` | 🔒 MANAGER | Thêm, sửa, xóa lớp tập (kiểm tra unique `classCode`) |

### 4.5. Lịch tập (`/api/schedules`) & Đặt chỗ lớp học (`/api/member`)
- **Kiểm tra khi đăng ký lớp**:
  - Gói tập của hội viên phải còn hạn và ngày diễn ra buổi tập phải nằm trong hạn của gói.
  - Buổi tập chưa bắt đầu và còn chỗ trống (`registeredCount < capacity`).
  - Hội viên chưa đăng ký buổi tập đó trước đó.
- **Xử lý đồng thời (Concurrency)**: Sử dụng khóa bi quan `PESSIMISTIC_WRITE` trên lịch tập để chống vượt quá sức chứa khi nhiều thành viên tranh chỗ cuối cùng.
- **Tự động đóng lớp**: Scheduled Job (`ClassSessionCloseJob`) tự động quét mỗi phút để xử lý và đóng đăng ký các buổi tập đã bắt đầu.

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| GET | `/api/schedules` | 🔒 Đã đăng nhập | Xem lịch tập (hỗ trợ lọc `?coachId=..`, `?classId=..`) |
| POST / PUT / DELETE | `/api/schedules` | 🔒 MANAGER | Tạo, sửa, hủy lịch tập (kiểm tra trùng lịch phòng & coach) |
| GET | `/api/member/class-sessions/available` | 🔒 MEMBER | Danh sách buổi tập hợp lệ mà hội viên có thể đăng ký |
| GET | `/api/member/class-enrollments` | 🔒 MEMBER | Danh sách các buổi tập hội viên đã đăng ký |
| POST | `/api/member/class-sessions/{scheduleId}/enrollment` | 🔒 MEMBER | Đăng ký đặt chỗ vào buổi tập |
| DELETE | `/api/member/class-sessions/{scheduleId}/enrollment` | 🔒 MEMBER | Hủy đăng ký đặt chỗ buổi tập |

### 4.6. Thanh toán & Cổng PayOS (`/api/payments`, `/api/payos`)
- Quy trình thanh toán hỗ trợ cả tiền mặt tại quầy (`CASH`) và cổng thanh toán trực tuyến PayOS (`PAYOS`).
- Sau khi thanh toán thành công (qua PayOS Webhook xác thực chữ ký số), hệ thống tự động cập nhật hóa đơn (`Invoice`), thanh toán (`Payment`) sang trạng thái hoàn tất và kích hoạt/gia hạn thời gian gói tập (`MemberMembership`) cho hội viên.

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| POST | `/api/member/payments/checkout` | 🔒 MEMBER | Hội viên tạo yêu cầu thanh toán mua gói tập |
| GET | `/api/member/payments` | 🔒 MEMBER | Xem lịch sử các giao dịch thanh toán của bản thân |
| GET | `/api/member/invoices` | 🔒 MEMBER | Xem danh sách hóa đơn của bản thân |
| GET | `/api/member/memberships` | 🔒 MEMBER | Xem thông tin gói tập hiện tại và thời hạn sử dụng |
| POST | `/api/payments/cash-checkout` | 🔒 RECEPTIONIST / MANAGER | Thu tiền mặt trực tiếp tại quầy và kích hoạt gói ngay |
| POST | `/api/payments/{id}/confirm` | 🔒 MANAGER | Xác nhận giao dịch thanh toán |
| POST | `/api/payments/{id}/cancel` | 🔒 MANAGER | Hủy giao dịch thanh toán |
| **POST** | `/api/payos/create-payment-link` | 🔒 MEMBER / MANAGER / RECEPTIONIST | **Tạo link thanh toán PayOS (trả về URL cổng checkout)** |
| **POST** | `/api/payos/webhook` | Public | **Webhook PayOS: Nhận kết quả thanh toán server-to-server và xác thực chữ ký số** |
| **GET** | `/api/payos/callback` | Public | **Return URL: PayOS redirect người dùng về sau thanh toán** |
| **GET** | `/api/payos/cancel` | Public | **Cancel URL: PayOS redirect khi người dùng hủy giao dịch** |
| **POST** | `/api/payos/mock-success/{orderCode}` | 🔒 Đã đăng nhập | Giả lập thanh toán thành công (dùng cho test/demo) |

---

## 5. Ví dụ kiểm thử luồng chính (PowerShell)

### Bước 1: Đăng nhập lấy Bearer Token
```powershell
$login = Invoke-RestMethod -Uri "http://localhost:8080/auth/login" `
  -Method Post -ContentType "application/json" `
  -Body '{"email":"member@example.com","password":"password123"}'
$token = $login.token
$headers = @{ Authorization = "Bearer $token" }
```

### Bước 2: Tạo link thanh toán PayOS cho gói tập
```powershell
$payosReq = @{
  packageId = 1
  description = "Thanh toan goi tap"
} | ConvertTo-Json

$payUrlRes = Invoke-RestMethod -Uri "http://localhost:8080/api/payos/create-payment-link" `
  -Method Post -ContentType "application/json" -Headers $headers -Body $payosReq

Write-Host "Mở link sau trên trình duyệt để thanh toán PayOS:"
Write-Host $payUrlRes.paymentUrl
```

### Bước 3: Xem các buổi học có thể đăng ký và đặt chỗ
```powershell
# Xem danh sách buổi tập hợp lệ
$available = Invoke-RestMethod -Uri "http://localhost:8080/api/member/class-sessions/available" -Headers $headers

# Đặt chỗ buổi học số 1
Invoke-RestMethod -Uri "http://localhost:8080/api/member/class-sessions/1/enrollment" -Method Post -Headers $headers

# Xem danh sách lớp đã đặt chỗ
Invoke-RestMethod -Uri "http://localhost:8080/api/member/class-enrollments" -Headers $headers
```

---

## 6. Chạy Test Suite
Dự án có đầy đủ kiểm thử đơn vị, kiểm thử tích hợp và kiểm thử tương tranh (concurrency test):
```bash
./mvnw test
```

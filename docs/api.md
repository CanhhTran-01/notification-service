# Hợp đồng HTTP

Ứng dụng mặc định nghe ở cổng `8080`. Các API JSON thành công có body trả envelope `{success, code, message, data}`. `code` bên trong body có thể khác HTTP status. Chẳng hạn endpoint publish trả HTTP `202` và body có `code: 200`. `204` trả body rỗng. Với lỗi nghiệp vụ, `GlobalExceptionHandler` lấy HTTP status và mã ứng dụng từ `ErrorCode`. Validation lỗi ở request body trả HTTP `400`.

## Publish từ service khác

Prefix `/api/v1/external-service/notifications`.

| Method | Path | HTTP | Hành vi |
| --- | --- | --- | --- |
| POST | `/publish` | 202 | Tách IN_APP và external theo `channels` rồi publish lên broker |
| POST | `/publish-email` | 202 | Gửi nguyên event vào external exchange. Dùng cho EMAIL, SMS hoặc PUSH |
| POST | `/publish-inapp` | 202 | Gửi nguyên event vào IN_APP exchange. Consumer xử lý IN_APP |

HTTP `202` cho biết controller đã gọi publish thành công. Kết quả gửi và trạng thái xử lý được xác định về sau.

Ví dụ `NotificationEvent` dùng kênh IN_APP.

```json
{
  "eventId": "demo-inapp-001",
  "eventType": "WELCOME",
  "source": "IDENTITY_SERVICE",
  "timestamp": "2026-01-01T09:00:00",
  "recipient": {
    "userId": "demo-user"
  },
  "channels": ["IN_APP"],
  "payload": {
    "name": "Demo User",
    "message": "Chào mừng bạn đến với hệ thống"
  }
}
```

- Bean Validation yêu cầu `eventId` có nội dung, `recipient` và `payload` khác null, `channels` có ít nhất một phần tử. Cung cấp cả `eventType`, `source`, `timestamp` và dữ liệu recipient cần thiết để đi qua xử lý và replay. Các trường thời gian dùng `LocalDateTime` theo định dạng ISO không có offset.
- `Recipient` dùng `userId` cho IN_APP, `email` cho EMAIL, `phone` cho SMS và `deviceToken` cho PUSH. Dùng tên enum từ `Channel`, `EventType`, `ServiceSource`.
- `payload` là object JSON. EMAIL lấy biến render từ payload. Template seed `WELCOME` dùng `name` và `code`. Template `OTP_LOGIN` dùng `otp`.

## API người dùng

Prefix `/api/v1/users`.

| Method | Path | HTTP | Hành vi |
| --- | --- | --- | --- |
| GET | `/preferences/{recipientId}/all-channels` | 200 | Danh sách tùy chọn đã lưu |
| GET | `/preferences/{recipientId}/active-channels` | 200 | Danh sách tùy chọn đã lưu và đang bật |
| PUT | `/preferences/{recipientId}/{channel}` | 204 | Đảo trạng thái hàng hiện có, không gửi body |
| GET | `/notifications/{recipientId}` | 200 | Lịch sử IN_APP từ mới đến cũ. Mặc định `pageNumber=0`, `pageSize=5` |
| PUT | `/notifications/{notificationId}/{recipientId}/read` | 204 | Đánh dấu đã đọc, tìm theo ID thông báo kiểu UUID và recipient ID |
| GET | `/notifications/{recipientId}/unread-count` | 200 | Số thông báo IN_APP chưa đọc trong `data` |
| GET | `/notifications/stream/subscribe/{recipientId}` | SSE | Mở kết nối `text/event-stream` với event `notification` và `ping` |

Danh sách tùy chọn phản ánh dữ liệu đã lưu. Consumer vẫn mặc định bật kênh khi thiếu hàng tương ứng. Với SSE, event `notification` mang `id`, `eventType`, `payload`, `createdAt`. Event `ping` mang chuỗi `keep-alive`. Sau khi client kết nối lại, dùng API lịch sử và unread count để đồng bộ.

## API admin

Prefix `/api/v1/admin`. Path param `id` là UUID. Hai endpoint liệt kê mặc định `pageNumber=0`, `pageSize=10`.

| Method | Path | HTTP | Query hoặc body |
| --- | --- | --- | --- |
| POST | `/notifications` | 200 | Tạo `DRAFT`. Body gồm `eventId`, `source`, `recipientContact`, `channel`, `eventType`. DTO cho phép thêm `recipientId` và `payload` |
| POST | `/notifications/{id}/schedule` | 200 | Body `{"scheduledAt":"2027-01-01T09:00:00"}`. Thời điểm phải từ hiện tại trở đi theo đồng hồ hệ thống |
| GET | `/notifications/failed` | 200 | Liệt kê bản ghi `FAILED` từ mới đến cũ |
| PUT | `/notifications/{id}` | 200 | Bản ghi phải `FAILED`. Cập nhật `payload` và `errorMessage` nếu khác null. DTO nhận `recipientContact` nhưng entity hiện giữ contact cũ |
| POST | `/notifications/{id}/retry` | 204 | Reset bản ghi `FAILED` để scheduler xử lý |
| GET | `/dead-letter-events` | 200 | Có thể lọc theo `resolved` và `queueName` |
| POST | `/dead-letter-events/{id}/replay` | 204 | Query bắt buộc `queueName`. Tìm event chưa resolved và publish lại vào external exchange |

API trong repository hiện phục vụ demo. Controller chưa triển khai xác thực và phân quyền cho các route user, admin hoặc publisher. Điều kiện truy vấn theo `recipientId` là lọc dữ liệu dựa trên giá trị client cung cấp. SSE có cấu hình CORS rộng dành cho dev.

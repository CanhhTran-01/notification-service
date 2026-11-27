# Hướng dẫn làm việc với Codex

Áp dụng cho toàn bộ repository. Đây là dịch vụ thông báo Java 21, Spring Boot 3.4.x, Maven, MySQL và RabbitMQ. Package gốc là `com.project.notificationservice`. Mở [mục lục tài liệu](docs/README.md) để chọn phần liên quan đến task.

## Tìm điểm sửa

| Công việc | Đọc trước |
| --- | --- |
| Publish, routing, consume, replay | `NotificationPublisherController`, `RabbitNotificationPublisher`, `RabbitMQConfig`, các consumer và `AdminNotificationServiceImpl.replay` |
| Gửi, đổi trạng thái, retry hoặc lên lịch | `NotificationProcessorServiceImpl`, `NotificationStatusServiceImpl`, `Notification`, `NotificationRetryScheduler` |
| Lịch sử, badge, đánh dấu đã đọc, tùy chọn kênh hoặc SSE | `UserNotificationController`, `UserNotificationServiceImpl`, `NotificationPreferenceServiceImpl`, `SseEmitterServiceImpl` |
| Rate limit, locking, migration hoặc template | `RateLimitingServiceImpl`, `RateLimitingRepository`, `application.yaml`, `db/migration`, `EmailNotificationSender` |

## Quy ước khi sửa code

- Giữ controller tập trung vào HTTP và validation. Đặt nghiệp vụ trong `service/impl`, truy vấn trong repository và thao tác gửi theo kênh trong `NotificationSender`. Dùng DTO, `ApiResponse`, `BaseException` và `ErrorCode` hiện có.
- Khi sửa hợp đồng HTTP hoặc AMQP, kiểm tra DTO, controller, publisher và consumer cùng nhau. Giữ tương thích với caller hiện tại khi task không yêu cầu đổi hợp đồng.
- Giữ quy tắc idempotency theo `(event_id, channel)` ở cả processor và MySQL. Khi sửa retry, xem cả chuyển trạng thái, giao dịch, thời điểm `nextRetryTime` và log audit `REQUIRES_NEW`. Kiểm tra đường gọi qua Spring proxy trước khi dựa vào `@Async`, `@Transactional` hoặc `@Retryable`.
- Phân biệt retry bản ghi trong MySQL với replay event RabbitMQ. Dựa vào hành vi thực tế của code khi sửa hai đường này.
- Khi đổi schema, thêm Flyway migration phiên bản mới. Giữ `V1` và `V2` đã áp dụng, cập nhật entity và truy vấn đi kèm. Dùng cấu hình local riêng cho thông tin kết nối và credential.
- Phân loại EMAIL, IN_APP và sender mô phỏng khi mô tả kết quả. Với thay đổi liên quan tới user hoặc admin, kiểm tra cách lấy danh tính và quyền truy cập tại entry point. API hiện tại phục vụ demo và nhận `recipientId` từ client.

## Kiểm tra trước khi bàn giao

- Chọn unit test hoặc integration test theo phạm vi thay đổi. Dùng lệnh trong [hướng dẫn phát triển](docs/development.md#chạy-test-và-kiểm-tra-format) để chạy riêng từng nhóm.
- Dùng `bash ./mvnw spotless:check` để kiểm tra format. `verify` cũng chạy Spotless. Dùng `.\mvnw.cmd` tương ứng trên Windows PowerShell. Giữ thay đổi format trong phạm vi file đang sửa.
- Full suite cần cấu hình Spring cho cả context mặc định và profile `test`, cùng Docker cho MySQL Testcontainers. Nêu lệnh đã chạy và điều kiện còn thiếu nếu không thể chạy đủ.
- Cập nhật tài liệu liên quan khi đổi API, cấu hình, routing hoặc vòng đời thông báo.

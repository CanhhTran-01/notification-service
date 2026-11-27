# Notification Service

Dịch vụ nhận yêu cầu thông báo qua HTTP, chuyển event vào RabbitMQ, xử lý theo từng kênh và lưu trạng thái trong MySQL. EMAIL gửi qua SMTP, IN_APP lưu lịch sử và phát SSE, SMS/PUSH hiện ghi log mô phỏng. Admin có thể tạo bản nháp, lên lịch, retry bản ghi lỗi và replay event từ dead-letter queue.

| Tài liệu | Nội dung |
| --- | --- |
| [Kiến trúc và luồng xử lý](architecture.md) | Package, routing RabbitMQ, vòng đời thông báo, dữ liệu và các điểm cần kiểm tra khi sửa |
| [Hợp đồng HTTP](api.md) | Endpoint, DTO, mã phản hồi, ví dụ IN_APP và SSE |
| [Chạy local và kiểm tra](development.md) | Java/Maven, MySQL, RabbitMQ, SMTP, profile, kiểm tra nhanh và chạy test |
| [Hướng dẫn Codex](../AGENTS.md) | Điểm vào theo loại công việc, quy ước sửa code và xác minh |

Tài liệu mô tả nhánh `develop` tại thời điểm đọc commit [`6cea621`](https://github.com/CanhhTran-01/notification-service/commit/6cea621349603d3bcd657b321bc038c315d88cd1).

# Chạy local và kiểm tra

## Môi trường

- JDK 21. Spring Boot `3.4.13`. Maven Wrapper dùng Maven `3.9.12` và có thể tải Maven khi chạy lần đầu.
- MySQL với một database local đã tạo. Flyway tạo bảng rồi nạp dữ liệu mẫu. MySQL `8.4` là bản dùng trong integration test.
- RabbitMQ đang chạy và tài khoản có quyền khai báo exchange, queue, binding. Ứng dụng tự khai báo topology khi broker kết nối.
- SMTP local hoặc cấu hình SMTP dành cho dev. Bean `EmailNotificationSender` cần `JavaMailSender` và `spring.mail.username` ngay cả khi chỉ thử IN_APP.
- Docker hoặc môi trường tương thích Testcontainers khi chạy integration test.

Profile mặc định là `dev`. `application.yaml` được commit, còn `application-dev.yaml`, `application-prod.yaml`, `src/test/resources/application-test.yaml` và `.env` nằm trong `.gitignore`. `.env.example` minh họa tên biến kiểu `DBMS_*`, `RABBITMQ_*`, `MAIL_*`. Base YAML chưa map chúng và chưa khai báo import `.env`. Có thể dùng biến Spring Boot trực tiếp như bên dưới hoặc tham chiếu `.env.example` khi viết cấu hình profile local.

## Khởi động

Ví dụ với MySQL, RabbitMQ và SMTP local đã có sẵn. Dùng giá trị đúng với môi trường của bạn. SMTP local trong ví dụ lắng nghe tại `1025`.

| Biến Spring Boot | Giá trị mẫu |
| --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/notification_service` |
| `SPRING_DATASOURCE_USERNAME` | `notification_app` |
| `SPRING_DATASOURCE_PASSWORD` | Mật khẩu database local |
| `SPRING_RABBITMQ_HOST` | `localhost` |
| `SPRING_RABBITMQ_PORT` | `5672` |
| `SPRING_RABBITMQ_USERNAME` | Tài khoản broker local |
| `SPRING_RABBITMQ_PASSWORD` | Mật khẩu broker local |
| `SPRING_MAIL_HOST` | `localhost` |
| `SPRING_MAIL_PORT` | `1025` |
| `SPRING_MAIL_USERNAME` | `demo@example.test` |

Đặt các giá trị này trong môi trường phiên terminal hoặc run configuration của IDE. Nếu dùng YAML local, các prefix tương ứng là `spring.datasource`, `spring.rabbitmq` và `spring.mail`. Dùng thêm cấu hình xác thực/TLS theo SMTP server khi cần.

| Mục đích | Linux/macOS/Git Bash | Windows PowerShell |
| --- | --- | --- |
| Chạy ứng dụng | `bash ./mvnw spring-boot:run` | `.\mvnw.cmd spring-boot:run` |
| Build JAR, bỏ chạy test | `bash ./mvnw -DskipTests package` | `.\mvnw.cmd -DskipTests package` |
| Chạy JAR | `java -jar target/notificationservice-0.0.1-SNAPSHOT.jar` | `java -jar target/notificationservice-0.0.1-SNAPSHOT.jar` |

Với lỗi datasource, kiểm tra JDBC URL và quyền tạo bảng của user. Với lỗi gửi email, kiểm tra SMTP, recipient contact và template active tương ứng trong MySQL. `logback-spring.xml` có đường dẫn file log cố định dạng `D:/...`. Dùng console để xem log và kiểm tra quyền ghi khi chạy ngoài máy ban đầu.

## Kiểm tra nhanh IN_APP

1. Mở terminal riêng để subscribe SSE. `ping` xuất hiện theo heartbeat, mặc định 15 giây.

   ```bash
   curl -N http://localhost:8080/api/v1/users/notifications/stream/subscribe/demo-user
   ```

2. Lưu JSON mẫu trong [hợp đồng HTTP](api.md#publish-từ-service-khác) thành `event.json`. Publish ở terminal khác. Trên Windows PowerShell dùng `curl.exe`.

   ```bash
   curl -i -X POST http://localhost:8080/api/v1/external-service/notifications/publish -H 'Content-Type: application/json' --data-binary '@event.json'
   ```

3. Kiểm tra HTTP `202`, event `notification` trên stream, và gọi lịch sử cùng badge. Thay `eventId` cho lần gửi tiếp theo và đợi qua khoảng cách tối thiểu mặc định của IN_APP là 10 giây.

   ```bash
   curl -i http://localhost:8080/api/v1/users/notifications/demo-user
   curl -i http://localhost:8080/api/v1/users/notifications/demo-user/unread-count
   ```

## Chạy test và kiểm tra format

| Phạm vi | Linux/macOS/Git Bash | Windows PowerShell |
| --- | --- | --- |
| Unit test hiện có | `bash ./mvnw "-Dtest=ExternalNotificationConsumerTest,NotificationTest,RateLimitingServiceImplTest,UserNotificationServiceImplTest" test` | `.\mvnw.cmd "-Dtest=ExternalNotificationConsumerTest,NotificationTest,RateLimitingServiceImplTest,UserNotificationServiceImplTest" test` |
| Integration test | `bash ./mvnw "-Dtest=*IntegrationTest" test` | `.\mvnw.cmd "-Dtest=*IntegrationTest" test` |
| Full suite | `bash ./mvnw test` | `.\mvnw.cmd test` |
| Format | `bash ./mvnw spotless:check` | `.\mvnw.cmd spotless:check` |
| Full suite và kiểm tra ở phase verify | `bash ./mvnw verify` | `.\mvnw.cmd verify` |

- Unit test dùng JUnit, Mockito và AssertJ. Bao phủ lựa chọn kênh của external consumer, chuyển trạng thái entity, rate limit và thao tác user.
- Integration test chạy `@SpringBootTest`, profile `test` và MySQL Testcontainers với `@ServiceConnection`. Test hiện tập trung vào unique constraint, kiểm tra trùng và truy vấn bản ghi đến hạn. RabbitMQ và SMTP không được cấp container bởi `BaseIntegrationTest`.
- Khi chạy integration test, cấp mail config để khởi tạo bean và dùng RabbitMQ local cho các listener. Listener IN_APP dùng factory riêng, nên cờ Spring Boot `spring.rabbitmq.listener.simple.auto-startup` không tắt listener này.
- `NotificationserviceApplicationTests` khởi động Spring context mặc định mà không kế thừa `BaseIntegrationTest`. Cần datasource và mail config cho test này nếu chạy cả suite. Testcontainers MySQL ở integration suite không cấp datasource cho smoke test đó.
- Spotless dùng Palantir Java Format, xóa import thừa và kiểm tra ở phase `verify`. Lệnh sửa format là `spotless:apply`. Kiểm tra phạm vi diff sau khi áp dụng.

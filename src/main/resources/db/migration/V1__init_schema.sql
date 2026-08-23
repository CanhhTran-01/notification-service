-- 1. Bảng lưu trữ template thông báo
CREATE TABLE notification_templates (
                                        id VARCHAR(36) PRIMARY KEY,
                                        template_code VARCHAR(50) NOT NULL,
                                        channel VARCHAR(50) NOT NULL,
                                        subject_template TEXT NOT NULL,
                                        body_template TEXT NOT NULL,
                                        is_active BOOLEAN NOT NULL DEFAULT TRUE,
                                        created_at DATETIME(6),
                                        UNIQUE KEY uk_template_channel (template_code, channel)
);

-- 2. Bảng cài đặt nhận thông báo của user
CREATE TABLE notification_preferences (
                                          id VARCHAR(36) PRIMARY KEY,
                                          recipient_id VARCHAR(255),
                                          channel VARCHAR(50) NOT NULL,
                                          is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                                          updated_at DATETIME(6) NOT NULL,
                                          UNIQUE KEY uk_recipient_channel (recipient_id, channel)
);

-- 3. Bảng giới hạn tần suất gửi (Rate Limiting)
CREATE TABLE rate_limit_records (
                                    id VARCHAR(36) PRIMARY KEY,
                                    recipient_id VARCHAR(255) NOT NULL,
                                    channel VARCHAR(50) NOT NULL,
                                    event_type VARCHAR(50),
                                    source VARCHAR(50),
                                    window_end DATETIME(6) NOT NULL,
                                    send_count INT NOT NULL DEFAULT 1,
                                    last_send_at DATETIME(6),
                                    created_at DATETIME(6) NOT NULL,
                                    UNIQUE KEY uk_rate_limit (recipient_id, channel, source, event_type)
);

-- 4. Bảng lưu trữ thông tin Notification chính
CREATE TABLE notifications (
                               id VARCHAR(36) PRIMARY KEY,
                               source VARCHAR(50) NOT NULL,
                               recipient_id VARCHAR(255),
                               recipient_contact VARCHAR(255) NOT NULL,
                               channel VARCHAR(50) NOT NULL,
                               event_type VARCHAR(50) NOT NULL,
                               status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                               payload JSON,
                               max_retries INT NOT NULL,
                               retry_count INT NOT NULL DEFAULT 0,
                               error_message TEXT,
                               event_id VARCHAR(255) NOT NULL,
                               scheduled_at DATETIME(6),
                               sent_at DATETIME(6),
                               created_at DATETIME(6) NOT NULL,
                               is_read BOOLEAN NOT NULL DEFAULT FALSE,
                               read_at DATETIME(6),
                               next_retry_time DATETIME(6),
                               UNIQUE KEY uk_event_channel (event_id, channel)
);

-- 5. Bảng log trạng thái của Notification
CREATE TABLE notification_logs (
                                   id VARCHAR(36) PRIMARY KEY,
                                   notification_id VARCHAR(36) NOT NULL,
                                   old_status VARCHAR(50) NOT NULL,
                                   new_status VARCHAR(50) NOT NULL,
                                   message VARCHAR(255) NOT NULL,
                                   created_at DATETIME(6) NOT NULL,
                                   CONSTRAINT fk_log_notification FOREIGN KEY (notification_id) REFERENCES notifications (id) ON DELETE CASCADE
);

-- 6. Bảng lưu trữ các tin nhắn lỗi, cần xử lý lại (Dead Letter Queue)
CREATE TABLE dead_letter_events (
                                    id VARCHAR(36) PRIMARY KEY,
                                    queue_name VARCHAR(255) NOT NULL,
                                    payload TEXT,
                                    error_reason TEXT,
                                    retry_count INT NOT NULL DEFAULT 0,
                                    resolved BOOLEAN NOT NULL DEFAULT FALSE,
                                    created_at DATETIME(6) NOT NULL
);
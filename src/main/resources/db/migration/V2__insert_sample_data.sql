-- ==========================================
-- notification_templates
-- ==========================================
INSERT INTO notification_templates (id, template_code, channel, subject_template, body_template, is_active, created_at)
VALUES
    ('11111111-1111-1111-1111-111111111001', 'WELCOME', 'EMAIL',
     'Chào mừng [[${name}]]!',
     'Xin chào [[${name}]], cảm ơn bạn đã đăng ký. Mã xác nhận: [[${code}]]',
     true, NOW()),

    ('11111111-1111-1111-1111-111111111002', 'WELCOME', 'IN_APP',
     'Chào mừng bạn!',
     'Xin chào [[${name}]], cảm ơn bạn đã đăng ký hệ thống.',
     true, NOW()),

    ('11111111-1111-1111-1111-111111111003', 'OTP_LOGIN', 'EMAIL',
     'Mã OTP đăng nhập',
     'Mã OTP của bạn là: [[${otp}]]. Có hiệu lực trong 5 phút.',
     true, NOW());


-- ==========================================
-- notification_preferences
-- ==========================================
INSERT INTO notification_preferences (id, recipient_id, channel, is_enabled, updated_at)
VALUES
    ('22222222-2222-2222-2222-222222222001', 'user-001', 'EMAIL', true, NOW()),
    ('22222222-2222-2222-2222-222222222002', 'user-001', 'IN_APP', true, NOW()),
    ('22222222-2222-2222-2222-222222222003', 'user-002', 'EMAIL', true, NOW());


-- ==========================================
-- notifications
-- ==========================================
INSERT INTO notifications (
    id, source, recipient_id, recipient_contact, channel, event_type,
    status, payload, max_retries, retry_count, error_message, event_id,
    scheduled_at, sent_at, created_at, is_read, read_at, next_retry_time
)
VALUES
    -- EMAIL - PENDING
    ('33333333-3333-3333-3333-333333333001',
     'IDENTITY_SERVICE', 'user-001', 'test@example.com', 'EMAIL', 'WELCOME',
     'PENDING',
     '{"name": "Nguyen Van A", "code": "ABC123"}',
     3, 0, NULL, 'evt-welcome-001',
     NULL, NULL, NOW(), false, NULL, NULL),

    -- IN_APP - PENDING
    ('33333333-3333-3333-3333-333333333002',
     'IDENTITY_SERVICE', 'user-001', 'user-001', 'IN_APP', 'WELCOME',
     'PENDING',
     '{"name": "Nguyen Van A"}',
     3, 0, NULL, 'evt-welcome-001',
     NULL, NULL, NOW(), false, NULL, NULL),

    -- EMAIL - đã SENT
    ('33333333-3333-3333-3333-333333333003',
     'IDENTITY_SERVICE', 'user-002', 'user2@example.com', 'EMAIL', 'OTP_LOGIN',
     'SENT',
     '{"otp": "123456"}',
     3, 0, NULL, 'evt-otp-001',
     NULL, NOW() - INTERVAL 10 MINUTE, NOW() - INTERVAL 15 MINUTE,
     true, NOW() - INTERVAL 5 MINUTE, NULL);


-- ==========================================
-- notification_logs
-- ==========================================
INSERT INTO notification_logs (id, notification_id, old_status, new_status, message, created_at)
VALUES
    ('44444444-4444-4444-4444-444444444001',
     '33333333-3333-3333-3333-333333333003',
     'PENDING', 'PROCESSING', 'Bắt đầu xử lý gửi email', NOW() - INTERVAL 12 MINUTE),

    ('44444444-4444-4444-4444-444444444002',
     '33333333-3333-3333-3333-333333333003',
     'PROCESSING', 'SENT', 'Gửi email thành công', NOW() - INTERVAL 10 MINUTE);


-- ==========================================
-- dead_letter_events
-- ==========================================
INSERT INTO dead_letter_events (id, queue_name, payload, error_reason, retry_count, resolved, created_at)
VALUES
    ('66666666-6666-6666-6666-666666666001',
     'notification.email.queue',
     '{"eventId": "evt-fail-001", "channel": "EMAIL"}',
     'SMTP connection timeout',
     3, false, NOW() - INTERVAL 1 HOUR),

    ('66666666-6666-6666-6666-666666666002',
     'notification.inapp.queue',
     '{"eventId": "evt-fail-002", "channel": "IN_APP", "userId": "user-003"}',
     'User preference disabled for IN_APP',
     2, false, NOW() - INTERVAL 30 MINUTE);
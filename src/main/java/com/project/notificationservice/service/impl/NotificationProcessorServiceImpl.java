package com.project.notificationservice.service.impl;

import com.project.notificationservice.entity.Notification;
import com.project.notificationservice.enums.Channel;
import com.project.notificationservice.enums.NotificationStatus;
import com.project.notificationservice.helper.NotificationLogHelper;
import com.project.notificationservice.repository.NotificationRepository;
import com.project.notificationservice.sender.NotificationSender;
import com.project.notificationservice.service.NotificationProcessorService;
import com.project.notificationservice.service.NotificationStatusService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationProcessorServiceImpl implements NotificationProcessorService {

    private final Map<Channel, NotificationSender> senderMap;
    private final NotificationRepository notificationRepository;
    private final NotificationLogHelper logHelper;
    private final NotificationStatusService notificationStatusService;

    public NotificationProcessorServiceImpl(
            List<NotificationSender> senders,
            NotificationRepository notificationRepository,
            NotificationLogHelper logHelper,
            NotificationStatusService notificationStatusService) {

        this.senderMap = senders.stream().collect(Collectors.toMap(NotificationSender::getChannel, sender -> sender));
        this.notificationRepository = notificationRepository;
        this.notificationStatusService = notificationStatusService;
        this.logHelper = logHelper;
    }

    @Override
    public void process(Notification notification) {
        if (notification.getChannel() == Channel.IN_APP) sendInApp(notification);
        else sendExternal(notification);
    }

    // EMAIL, PUSH, SMS - with @Async
    @Async("notificationExecutor")
    @Override
    public void sendExternal(Notification notification) {
        processSending(notification);
    }

    // IN_APP - no @Async
    @Override
    public void sendInApp(Notification notification) {
        processSending(notification);
    }

    private void processSending(Notification notification) {

        // checking gateway 1: idempotency checking
        if (notification.getId() == null) {
            // CHƯA CÓ ID: message lần đầu được gửi đi, cần idempotency checking
            if (notificationRepository.existsByEventIdAndChannel(
                    notification.getEventId(), notification.getChannel())) {
                log.warn(
                        "Duplicate event detected, skipping: eventId [{}] with channel [{}]",
                        notification.getEventId(),
                        notification.getChannel());
                return;
            }

        } else {
            // ĐÃ CÓ ID: là tiến trình Retry lấy từ DB lên, bỏ qua idempotency checking
            log.info("Processing retry for notification ID: [{}]", notification.getId());
        }

        // checking gateway 2: channel checking
        NotificationSender sender = senderMap.get(notification.getChannel());
        if (sender == null) {
            NotificationStatus oldStatus = notification.getStatus(); // thường là "PENDNG"

            notificationStatusService.handleBusinessFailure(
                    notification, "Unsupported channel: " + notification.getChannel());

            // Business Log tạo Audit Trail cho Admin
            logHelper.log(
                    notification,
                    oldStatus,
                    notification.getStatus(),
                    "Unsupported channel: " + notification.getChannel()); // "PENDING" -> "FAILED"
            return;
        }

        // checking gateway 3: recipient_contact checking
        if (notification.getRecipientContact() == null) {
            NotificationStatus oldStatus = notification.getStatus(); // thường là "PENDNG"

            notificationStatusService.handleBusinessFailure(
                    notification, "Missing contact info for channel: " + notification.getChannel());

            // Business Log tạo Audit Trail cho Admin
            logHelper.log(
                    notification,
                    oldStatus,
                    notification.getStatus(),
                    "Missing contact info for channel: " + notification.getChannel()); // "PENDING" -> "FAILED"
            return;
        }

        // vượt qua hết 3 cổng checking, bắt đầu chuỗi xử lý
        NotificationStatus oldStatus = notification.getStatus(); // "PENDING"

        notificationStatusService.markAsProcessing(notification); // "PENDING" ---> "PROCESSING"
        logHelper.log(notification, oldStatus, notification.getStatus(), "processing started");

        try {
            oldStatus = notification.getStatus(); // "PROCESSING"
            sender.send(notification);

            notificationStatusService.markAsSent(notification); // "PROCESSING" ---> "SENT"
            logHelper.log(notification, oldStatus, notification.getStatus(), "sent successfully");

        } catch (Exception exception) {
            oldStatus = notification.getStatus(); // "PROCESSING"

            notificationStatusService.markAsRetryOrFailed(notification, exception); // "RETRYING" or "FALED"
            logHelper.log(notification, oldStatus, notification.getStatus(), exception.getMessage());
        }
    }
}

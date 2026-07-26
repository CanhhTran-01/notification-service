package com.project.notificationservice.scheduler;

import com.project.notificationservice.domain.entity.Notification;
import com.project.notificationservice.domain.entity.NotificationLog;
import com.project.notificationservice.domain.enums.NotificationStatus;
import com.project.notificationservice.repository.NotificationLogRepository;
import com.project.notificationservice.repository.NotificationRepository;
import com.project.notificationservice.service.NotificationProcessorService;
import java.time.LocalDateTime;
import com.project.notificationservice.service.NotificationProcessorService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRetryScheduler {

    private final NotificationProcessorService notificationProcessorService;
    private final NotificationRepository notificationRepository;
    private final NotificationLogRepository notificationLogRepository;

    @Scheduled(fixedDelay = 10000)
    public void retryPendingNotifications() {

        List<Notification> notifications =
                notificationRepository.findByStatusAndRetryCountLessThanAndNextRetryTimeBefore(
                        NotificationStatus.PENDING, 3, LocalDateTime.now());

        if (notifications.isEmpty()) {
            log.info("No pending notifications to retry");
            return;
        }

        log.info("Retry scheduler executed: found {} pending notifications", notifications.size());

        for (var notification : notifications) {

            log.info(
                    "Retrying notification: id={}, channel={}, retryCount={}",
                    notification.getId(),
                    notification.getChannel(),
                    notification.getRetryCount());

            NotificationStatus oldStatus = notification.getStatus(); // PENDING
            notification.markAsRetrying(); // PENDING -> RETRYING
            notificationRepository.save(notification);
            NotificationStatus newStatus = notification.getStatus(); // RETRYING

            logging(
                    notification,
                    oldStatus,
                    newStatus,
                    "retrying (attempt " + notification.getRetryCount() + " of " + notification.getMaxRetries()
                            + ")...");

            notificationProcessorService.send(notification);
            notificationProcessorService.send(notification);
        }
    }

    // handle logging notification
    private void logging(
            Notification notification, NotificationStatus oldStatus, NotificationStatus newStatus, String message) {

        notificationLogRepository.save(NotificationLog.builder()
                .notification(notification)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .message(message)
                .build());
    }
}

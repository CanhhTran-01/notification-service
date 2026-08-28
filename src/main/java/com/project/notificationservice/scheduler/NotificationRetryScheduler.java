package com.project.notificationservice.scheduler;

import com.project.notificationservice.entity.Notification;
import com.project.notificationservice.enums.NotificationStatus;
import com.project.notificationservice.helper.NotificationLogHelper;
import com.project.notificationservice.properties.RetryProperties;
import com.project.notificationservice.repository.NotificationRepository;
import com.project.notificationservice.service.NotificationProcessorService;
import com.project.notificationservice.service.NotificationStatusService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRetryScheduler {

    private final NotificationProcessorService notificationProcessorService;
    private final NotificationRepository notificationRepository;
    private final NotificationStatusService notificationStatusService;
    private final NotificationLogHelper logHelper;
    private final RetryProperties retryProperties;

    @Scheduled(fixedDelayString = "${app.retry.scheduler-fixed-delay-ms}")
    public void retryPendingNotifications() {

        List<Notification> notifications =
                notificationRepository.findByStatusAndRetryCountLessThanAndNextRetryTimeBefore(
                        NotificationStatus.PENDING, retryProperties.getMaxRetries(), LocalDateTime.now());

        // Không có cái nào cần retry
        if (notifications.isEmpty()) {
            log.info("No pending notifications to retry");
            return;
        }

        // Tìm thấy
        log.info("Retry scheduler executed: found {} pending notifications", notifications.size());

        for (var notification : notifications) {

            log.info(
                    "Retrying notification: id={}, channel={}, retryCount={}",
                    notification.getId(),
                    notification.getChannel(),
                    notification.getRetryCount());

            NotificationStatus oldStatus = notification.getStatus(); // PENDING
            notificationStatusService.markAsRetrying(notification); // PENDING ---> RETRYING

            logHelper.log(
                    notification,
                    oldStatus,
                    notification.getStatus(),
                    "retrying (attempt " + notification.getRetryCount() + " of " + notification.getMaxRetries()
                            + ")...");
            notificationProcessorService.process(notification);
        }
    }
}

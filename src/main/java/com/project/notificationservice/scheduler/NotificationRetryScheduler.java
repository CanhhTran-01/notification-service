package com.project.notificationservice.scheduler;

import com.project.notificationservice.domain.entity.Notification;
import com.project.notificationservice.domain.enums.NotificationStatus;
import com.project.notificationservice.repository.NotificationRepository;
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

    @Scheduled(fixedDelay = 300000)
    public void retryPendingNotifications() {

        List<Notification> notifications =
                notificationRepository.findByStatusAndRetryCountLessThan(NotificationStatus.PENDING, 3);

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
            notificationProcessorService.send(notification);
        }
    }
}

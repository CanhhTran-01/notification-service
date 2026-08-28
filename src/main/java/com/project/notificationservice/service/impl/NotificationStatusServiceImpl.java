package com.project.notificationservice.service.impl;

import com.project.notificationservice.entity.Notification;
import com.project.notificationservice.properties.RetryProperties;
import com.project.notificationservice.repository.NotificationRepository;
import com.project.notificationservice.service.NotificationStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationStatusServiceImpl implements NotificationStatusService {

    private final NotificationRepository notificationRepository;
    private final RetryProperties retryProperties;

    @Override
    @Transactional
    public void markAsProcessing(Notification notification) {
        notification.markAsProcessing();
        notificationRepository.saveAndFlush(notification); // INSERT ngay để có FK cho logHelper (REQUIRES_NEW)
    }

    @Override
    @Transactional
    public void markAsSent(Notification notification) {
        notification.markAsSent(); // PROCESSING ---> SENT
        notificationRepository.save(notification);
    }

    @Override
    @Transactional // Scheduled Job will scan DB for retrying with exponential backoff
    public void markAsRetryOrFailed(Notification notification, Exception exception) {
        notification.incrementRetryAndCalculateNextTime(retryProperties.getBaseDelaySeconds()); // >= maxRetries, FAILED
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void handleBusinessFailure(Notification notification, String errorMessage) {
        // System Log cho Dev/DevOps xem trên Console/Kibana
        log.warn(
                "Cannot process eventId [{}] with channel [{}]: {}",
                notification.getEventId(),
                notification.getChannel(),
                errorMessage);

        // Lưu notification FAILED vào DB phục vụ retry sau này
        notification.cancelling(errorMessage); // set status FAILED + errorMessage
        notificationRepository.saveAndFlush(notification);
    }

    @Override
    @Transactional
    public void markAsRetrying(Notification notification) {
        notification.markAsRetrying();
        notificationRepository.save(notification);
    }
}

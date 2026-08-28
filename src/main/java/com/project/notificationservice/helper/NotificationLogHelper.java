package com.project.notificationservice.helper;

import com.project.notificationservice.entity.Notification;
import com.project.notificationservice.entity.NotificationLog;
import com.project.notificationservice.enums.NotificationStatus;
import com.project.notificationservice.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationLogHelper {

    private final NotificationLogRepository notificationLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW) // log sẽ ở 1 transaction mới
    public void log(
            Notification notification, NotificationStatus oldStatus, NotificationStatus newStatus, String message) {

        NotificationLog log = NotificationLog.builder()
                .notification(notification)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .message(message)
                .build();

        notificationLogRepository.save(log);
    }
}

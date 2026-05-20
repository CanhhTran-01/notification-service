package com.project.notificationservice.service.impl;

import com.project.notificationservice.domain.entity.Notification;
import com.project.notificationservice.domain.enums.Channel;
import com.project.notificationservice.domain.enums.NotificationStatus;
import com.project.notificationservice.exception.BaseException;
import com.project.notificationservice.exception.ErrorCode;
import com.project.notificationservice.repository.NotificationRepository;
import com.project.notificationservice.sender.NotificationSender;
import com.project.notificationservice.service.NotificationService;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private final Map<Channel, NotificationSender> senderMap;
    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(List<NotificationSender> senders, NotificationRepository notificationRepository) {
        this.senderMap = senders.stream().collect(Collectors.toMap(NotificationSender::getChannel, sender -> sender));
        this.notificationRepository = notificationRepository;
    }

    @Async("notificationExecutor")
    @Transactional
    @Override
    public void send(Notification notification) {

        // idempotency checking
        if (notificationRepository.existsByEventId(notification.getEventId())) {
            log.warn("Duplicate event detected, skipping: {}", notification.getEventId());
            return;
        }

        // channel checking
        NotificationSender sender = senderMap.get(notification.getChannel());
        if (sender == null) {
            throw new BaseException(ErrorCode.UNSUPPORTED_CHANNEL);
        }

        // start processing
        notification.markAsProcessing();
        notificationRepository.save(notification);

        try {
            sender.send(notification);
            notification.markAsSent();
            notificationRepository.save(notification);

        } catch (Exception exception) {

            notification.incrementRetry(); // PENDING -> Scheduled Job scan DB for retrying
            notificationRepository.save(notification);

            if (notification.getStatus() == NotificationStatus.FAILED) {
                // FAILED
                throw new AmqpRejectAndDontRequeueException(
                        exception); // chấp nhận exception bị executor nuốt - fail silently
            }
        }
    }
}

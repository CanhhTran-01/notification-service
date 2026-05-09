package com.project.notificationservice.service.impl;

import com.project.notificationservice.domain.entity.Notification;
import com.project.notificationservice.domain.enums.Channel;
import com.project.notificationservice.domain.enums.NotificationStatus;
import com.project.notificationservice.exception.BaseException;
import com.project.notificationservice.exception.ErrorCode;
import com.project.notificationservice.repository.NotificationRepository;
import com.project.notificationservice.sender.NotificationSender;
import com.project.notificationservice.service.NotificationService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final Map<Channel, NotificationSender> senderMap;
    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(List<NotificationSender> senders, NotificationRepository notificationRepository) {
        this.senderMap = senders.stream().collect(Collectors.toMap(NotificationSender::getChannel, sender -> sender));
        this.notificationRepository = notificationRepository;
    }

    @Async("notificationExecutor")
    @Override
    public void send(Notification notification) {
        // checking before processing
        NotificationSender sender = senderMap.get(notification.getChannel());

        if (sender == null) {
            throw new BaseException(ErrorCode.UNSUPPORTED_CHANNEL);
        }

        // start processing
        notification.setStatus(NotificationStatus.PROCESSING);
        notificationRepository.save(notification);

        try {
            sender.send(notification);
            notification.setStatus(NotificationStatus.SENT);
            notificationRepository.save(notification);

        } catch (Exception exception) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(exception.getMessage());
            notificationRepository.save(notification);
        }
    }
}

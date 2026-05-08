package com.project.notificationservice.service.impl;

import com.project.notificationservice.domain.entity.Notification;
import com.project.notificationservice.domain.enums.Channel;
import com.project.notificationservice.exception.BaseException;
import com.project.notificationservice.exception.ErrorCode;
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

    public NotificationServiceImpl(List<NotificationSender> senders) {
        this.senderMap = senders.stream().collect(Collectors.toMap(NotificationSender::getChannel, sender -> sender));
    }

    @Async("notificationExecutor")
    @Override
    public void send(Notification notification) {
        NotificationSender sender = senderMap.get(notification.getChannel());

        if (sender == null) {
            // throw exception
        }

        sender.send(notification);
    }
}

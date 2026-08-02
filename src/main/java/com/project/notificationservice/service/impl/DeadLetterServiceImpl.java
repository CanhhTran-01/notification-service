package com.project.notificationservice.service.impl;

import com.project.notificationservice.entity.DeadLetterEvent;
import com.project.notificationservice.repository.DeadLetterEventRepository;
import com.project.notificationservice.service.DeadLetterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeadLetterServiceImpl implements DeadLetterService {

    private final DeadLetterEventRepository deadLetterEventRepository;

    @Override
    public void process(String payload, String queueName, String errorReason, int retryCount) {

        try {
            DeadLetterEvent event = DeadLetterEvent.builder()
                    .queueName(queueName)
                    .payload(payload)
                    .errorReason(errorReason)
                    .retryCount(retryCount)
                    .build();

            deadLetterEventRepository.save(event);

            log.info("Dead letter event saved: queue={}", queueName);

        } catch (Exception e) {
            log.error("Failed to save dead letter event: {}", e.getMessage());
        }
    }
}

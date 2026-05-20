package com.project.notificationservice.service;

public interface DeadLetterService {
    void process(String payload, String queueName, String errorReason, int retryCount);
}

package com.project.notificationservice.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.retry")
@Getter
@Setter
public class RetryProperties {
    private long schedulerFixedDelayMs;
    private int maxRetries;
    private long baseDelaySeconds;
}
package com.project.notificationservice.config.properties;

import java.util.Map;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "app.rate-limit")
@Data
@Component
public class RateLimitProperties {

    private Map<String, ChannelLimit> limits;

    @Setter @Getter
    public static class ChannelLimit {
        private int maxCount;
        private int windowMinutes;
        private int minIntervalSeconds;
    }
}

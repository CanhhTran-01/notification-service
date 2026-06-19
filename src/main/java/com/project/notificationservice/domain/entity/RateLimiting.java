package com.project.notificationservice.domain.entity;

import com.project.notificationservice.domain.enums.Channel;
import com.project.notificationservice.domain.enums.EventType;
import com.project.notificationservice.domain.enums.ServiceSource;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
        name = "rate_limit_records",
        uniqueConstraints = @UniqueConstraint(columnNames = {"recipient_id", "channel", "source", "event_type"}))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateLimiting {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "recipient_id", nullable = false, updatable = false)
    private String recipientId;

    @Column(nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private Channel channel;

    @Column(name = "event_type", updatable = false)
    @Enumerated(EnumType.STRING)
    private EventType eventType;

    @Column(updatable = false)
    @Enumerated(EnumType.STRING)
    private ServiceSource source;

    @Column(name = "window_end", nullable = false)
    private LocalDateTime windowEnd; // hoặc window_start -> trade-off

    @Column(name = "send_count", nullable = false)
    @Builder.Default
    private Integer sendCount = 1; // tạo mới record = đã gửi 1 lần

    @Column(name = "last_send_at")
    private LocalDateTime lastSendAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    // auto increment when sending
    public void recordSend() {
        this.sendCount++;
        this.lastSendAt = LocalDateTime.now();
    }

    // reset: new_window_end = now() + window-minutes
    public void resetWindow(LocalDateTime newWindowEnd) {
        this.sendCount = 1;
        this.windowEnd = newWindowEnd;
        this.lastSendAt = LocalDateTime.now();
    }
}

package com.project.notificationservice.domain.entity;

import com.project.notificationservice.domain.enums.Channel;
import com.project.notificationservice.domain.enums.NotificationStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "recipient_id")
    private String recipientId;

    @Column(name = "recipient_contact", nullable = false)
    private String recipientContact;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Channel channel;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> payload;

    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private Integer maxRetries = 3;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

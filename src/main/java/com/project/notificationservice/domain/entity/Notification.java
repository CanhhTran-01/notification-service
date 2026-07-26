package com.project.notificationservice.domain.entity;

import com.project.notificationservice.domain.enums.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notifications", uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "channel"}))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(updatable = false, nullable = false)
    @Enumerated(EnumType.STRING)
    private ServiceSource source;

    @Column(name = "recipient_id", updatable = false)
    private String recipientId;

    @Column(name = "recipient_contact", nullable = false, updatable = false)
    private String recipientContact;

    @Column(nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private Channel channel;

    @Column(name = "event_type", nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private EventType eventType;

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

    @Column(name = "event_id", nullable = false, updatable = false)
    private String eventId;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    @Setter
    private boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "next_retry_time")
    private LocalDateTime nextRetryTime;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    // cancelling message
    public void cancelling(String errorMessage) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public void markAsProcessing() {
        this.status = NotificationStatus.PROCESSING;
    }

    public void markAsSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    public void markAsFailed(String errorMessage) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public void markAsRetrying() {
        this.status = NotificationStatus.RETRYING;
    }

    public void incrementRetryAndCalculateNextTime(long baseDelaySeconds) {
        this.retryCount++;

        if (this.retryCount >= this.maxRetries) {

            this.markAsFailed("Đã đạt mức retry tối đa");
            this.nextRetryTime = null;

        } else {

            // delay = base_delay * 2^(retryCount - 1)
            // next_retry_time = now + delay
            this.nextRetryTime =
                    LocalDateTime.now().plusSeconds((long) (baseDelaySeconds * Math.pow(2, this.retryCount - 1)));
            this.status = NotificationStatus.PENDING;
        }
    }

    public void resetForRetry() {
        this.status = NotificationStatus.PENDING;
        this.retryCount = 0;
        this.errorMessage = null;
        this.nextRetryTime = LocalDateTime.now(); // retry ngay
    }
}

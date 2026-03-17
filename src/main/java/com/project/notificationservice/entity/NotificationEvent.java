package com.project.notificationservice.entity;

import com.project.notificationservice.enums.Channel;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notification_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private NotificationRecipient recipient;

    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EventType eventType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Channel channel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> metadata;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    protected void onCreate() {
        this.occurredAt = LocalDateTime.now();
    }

    public enum EventType {
        DELIVERED,
        READ,
        CLICKED,
        DISMISSED
    }
}

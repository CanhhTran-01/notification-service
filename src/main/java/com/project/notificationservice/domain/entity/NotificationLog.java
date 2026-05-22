package com.project.notificationservice.domain.entity;

import com.project.notificationservice.domain.enums.NotificationStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "notification_logs")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", updatable = false, nullable = false)
    private Notification notification;

    @Column(nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private NotificationStatus oldStatus;

    @Column(nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private NotificationStatus newStatus;

    @Column(nullable = false, updatable = false)
    private String message;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

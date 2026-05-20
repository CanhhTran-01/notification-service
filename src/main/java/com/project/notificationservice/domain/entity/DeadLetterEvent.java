package com.project.notificationservice.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "dead_letter_events")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeadLetterEvent {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "queue_name", nullable = false, updatable = false)
    private String queueName;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String payload;

    @Column(name = "error_reason", columnDefinition = "TEXT", updatable = false)
    private String errorReason;

    @Column(name = "retry_count", updatable = false)
    private int retryCount;

    @Column(nullable = false)
    @Setter
    @Builder.Default
    private boolean resolved = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

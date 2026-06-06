package com.project.notificationservice.domain.entity;

import com.project.notificationservice.domain.enums.Channel;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
        name = "notification_preferences",
        uniqueConstraints =
                @UniqueConstraint(
                        columnNames = {"recipient_id", "channel"})) // chỉ 1 recipient đi với 1 channel preference
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "recipient_id", updatable = false)
    private String recipientId;

    @Column(nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private Channel channel;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    @Setter
    private boolean isEnabled = true;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

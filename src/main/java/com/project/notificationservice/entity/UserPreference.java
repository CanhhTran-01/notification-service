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
@Table(
        name = "user_preferences",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "channel"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreference {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private TenantUser user;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Channel channel;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "quiet_hours", columnDefinition = "json")
    private Map<String, Object> quietHours;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

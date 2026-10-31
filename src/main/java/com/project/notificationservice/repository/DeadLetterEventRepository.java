package com.project.notificationservice.repository;

import com.project.notificationservice.entity.DeadLetterEvent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DeadLetterEventRepository
        extends JpaRepository<DeadLetterEvent, UUID>, JpaSpecificationExecutor<DeadLetterEvent> {
    Optional<DeadLetterEvent> findByIdAndQueueNameAndResolvedFalse(UUID id, String queueName);
}

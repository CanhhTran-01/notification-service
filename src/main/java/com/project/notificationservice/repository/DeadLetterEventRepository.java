package com.project.notificationservice.repository;

import com.project.notificationservice.domain.entity.DeadLetterEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeadLetterEventRepository extends JpaRepository<DeadLetterEvent, UUID> {

    // Tìm các event chưa được resolve — admin query để xử lý
    List<DeadLetterEvent> findByResolvedFalseOrderByCreatedAtDesc();

    // Tìm các event đã được resolve
    List<DeadLetterEvent> findByResolvedTrueOrderByCreatedAtDesc();

    // Tìm các event theo queue name
    List<DeadLetterEvent> findByQueueNameAndResolvedFalse(String queueName);
}

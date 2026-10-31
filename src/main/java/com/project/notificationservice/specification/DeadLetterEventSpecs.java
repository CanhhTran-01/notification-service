package com.project.notificationservice.specification;

import com.project.notificationservice.entity.DeadLetterEvent;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class DeadLetterEventSpecs {
    public static Specification<DeadLetterEvent> withCriteria(Boolean resolved, String queueName) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (resolved != null) {
                predicates.add(cb.equal(root.get("resolved"), resolved));
            }
            if (StringUtils.hasText(queueName)) {
                predicates.add(cb.equal(root.get("queueName"), queueName));
            }

            Objects.requireNonNull(query).orderBy(cb.desc(root.get("createdAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

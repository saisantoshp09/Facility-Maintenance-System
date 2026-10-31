package com.facility.maintenance.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkOrderCommentRepository extends JpaRepository<WorkOrderComment, UUID> {
    Page<WorkOrderComment> findByWorkOrderId(UUID workOrderId, Pageable pageable);

    List<WorkOrderComment> findByWorkOrderIdOrderByCreatedAt(UUID workOrderId);
}

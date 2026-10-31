package com.facility.maintenance.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.facility.maintenance.domain.Priority;
import com.facility.maintenance.domain.StatusHistoryEntry;
import com.facility.maintenance.domain.WorkOrder;
import com.facility.maintenance.domain.WorkOrderComment;
import com.facility.maintenance.domain.WorkOrderStatus;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class WorkOrderDtos {
    private WorkOrderDtos() {
    }

    public record CreateRequest(
            @NotNull UUID propertyId,
            UUID unitId,
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 4000) String description,
            @NotNull Priority priority,
            @Future Instant dueAt) {
    }

    public record TransitionRequest(
            @NotNull WorkOrderStatus targetStatus,
            @Size(max = 1000) String note,
            UUID assigneeId,
            @Future Instant dueAt) {
    }

    public record DueDateRequest(@NotNull @Future Instant dueAt) {
    }

    public record CommentRequest(@NotBlank @Size(max = 2000) String body) {
    }

    public record WorkOrderResponse(UUID id, UUID propertyId, UUID unitId, String title, String description,
            Priority priority, WorkOrderStatus status, UUID requesterId, UUID assigneeId, Instant dueAt,
            boolean overdue, Instant createdAt, Instant updatedAt) {
        public static WorkOrderResponse from(WorkOrder w, Instant now) {
            return new WorkOrderResponse(w.getId(), w.getPropertyId(), w.getUnitId(), w.getTitle(),
                    w.getDescription(), w.getPriority(), w.getStatus(), w.getRequesterId(), w.getAssigneeId(),
                    w.getDueAt(), w.isOverdue(now), w.getCreatedAt(), w.getUpdatedAt());
        }
    }

    public record CommentResponse(UUID id, UUID workOrderId, UUID authorId, String body, Instant createdAt) {
        public static CommentResponse from(WorkOrderComment c) {
            return new CommentResponse(c.getId(), c.getWorkOrderId(), c.getAuthorId(), c.getBody(),
                    c.getCreatedAt());
        }
    }

    public record HistoryResponse(UUID id, WorkOrderStatus fromStatus, WorkOrderStatus toStatus, UUID actorId,
            String note, Instant createdAt) {
        public static HistoryResponse from(StatusHistoryEntry h) {
            return new HistoryResponse(h.getId(), h.getFromStatus(), h.getToStatus(), h.getActorId(), h.getNote(),
                    h.getCreatedAt());
        }
    }
}

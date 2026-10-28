package com.facility.maintenance.application;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.facility.maintenance.api.dto.WorkOrderDtos.CreateRequest;
import com.facility.maintenance.api.dto.WorkOrderDtos.TransitionRequest;
import com.facility.maintenance.domain.Priority;
import com.facility.maintenance.domain.StatusHistoryEntry;
import com.facility.maintenance.domain.StatusHistoryRepository;
import com.facility.maintenance.domain.WorkOrder;
import com.facility.maintenance.domain.WorkOrderComment;
import com.facility.maintenance.domain.WorkOrderCommentRepository;
import com.facility.maintenance.domain.WorkOrderRepository;
import com.facility.maintenance.domain.WorkOrderStatus;

import jakarta.persistence.criteria.Predicate;

@Service
@Transactional
public class WorkOrderService {
    private static final Set<String> SORTS = Set.of("createdAt", "updatedAt", "dueAt", "priority", "status");
    private static final Set<String> COMMENT_SORTS = Set.of("createdAt");

    private final WorkOrderRepository workOrders;
    private final WorkOrderCommentRepository comments;
    private final StatusHistoryRepository history;
    private final PropertyDirectory properties;
    private final Clock clock;
    private final OutboxEventWriter outbox;

    public WorkOrderService(WorkOrderRepository workOrders, WorkOrderCommentRepository comments,
            StatusHistoryRepository history, PropertyDirectory properties, Clock clock, OutboxEventWriter outbox) {
        this.workOrders = workOrders;
        this.comments = comments;
        this.history = history;
        this.properties = properties;
        this.clock = clock;
        this.outbox = outbox;
    }

    public WorkOrder create(Caller caller, CreateRequest request) {
        PropertyAccessView access = properties.accessOf(caller, request.propertyId());
        if (!access.canManage()) {
            if (!access.isTenant()) {
                throw new AccessDeniedException("Only tenants and managers can open requests");
            }
            if (request.unitId() == null || !access.unitIds().contains(request.unitId())) {
                throw new AccessDeniedException("Tenants can only open requests for their own unit");
            }
        }
        Instant now = clock.instant();
        WorkOrder order = workOrders.save(new WorkOrder(request.propertyId(), request.unitId(),
                request.title().trim(), request.description().trim(), request.priority(), caller.userId(),
                request.dueAt(), now));
        history.save(new StatusHistoryEntry(order.getId(), null, WorkOrderStatus.OPEN, caller.userId(), null, now));
        outbox.write("WORK_ORDER_CREATED", caller.userId(), order.getRequesterId(), order.getAssigneeId(),
                order.getId(), "Work order created", "A work order was created");
        return order;
    }

    @Transactional(readOnly = true)
    public WorkOrder get(Caller caller, UUID id) {
        return load(caller, id).order();
    }

    @Transactional(readOnly = true)
    public Page<WorkOrder> list(Caller caller, UUID propertyId, WorkOrderStatus status, Priority priority,
            UUID assigneeId, Boolean overdue, Instant dueBefore, Pageable pageable) {
        PropertyAccessView access = properties.accessOf(caller, propertyId);
        Pageable safe = restrictSort(pageable, SORTS, Sort.by(Sort.Direction.DESC, "createdAt"));
        Instant now = clock.instant();
        Specification<WorkOrder> spec = (root, query, cb) -> {
            List<Predicate> all = new ArrayList<>();
            all.add(cb.equal(root.get("propertyId"), propertyId));
            if (!access.canManage()) {
                List<Predicate> visible = new ArrayList<>();
                if (access.isTenant()) {
                    visible.add(cb.equal(root.get("requesterId"), caller.userId()));
                }
                if (access.isTechnician()) {
                    visible.add(cb.equal(root.get("assigneeId"), caller.userId()));
                }
                all.add(visible.isEmpty() ? cb.disjunction() : cb.or(visible.toArray(new Predicate[0])));
            }
            if (status != null) {
                all.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                all.add(cb.equal(root.get("priority"), priority));
            }
            if (assigneeId != null) {
                all.add(cb.equal(root.get("assigneeId"), assigneeId));
            }
            if (dueBefore != null) {
                all.add(cb.lessThan(root.get("dueAt"), dueBefore));
            }
            if (overdue != null) {
                Predicate isOverdue = cb.and(cb.lessThan(root.get("dueAt"), now),
                        cb.not(root.get("status").in(WorkOrderStatus.RESOLVED, WorkOrderStatus.CLOSED,
                                WorkOrderStatus.REJECTED)));
                all.add(overdue ? isOverdue : cb.not(isOverdue));
            }
            return cb.and(all.toArray(new Predicate[0]));
        };
        return workOrders.findAll(spec, safe);
    }

    public WorkOrder transition(Caller caller, UUID id, TransitionRequest request) {
        Loaded loaded = load(caller, id);
        WorkOrder order = loaded.order();
        PropertyAccessView access = loaded.access();
        WorkOrderStatus target = request.targetStatus();

        if (!isPermitted(caller, access, order, target)) {
            throw new AccessDeniedException("You are not allowed to move this work order to " + target);
        }
        if (!order.getStatus().allowedNext().contains(target)) {
            throw new com.facility.maintenance.domain.InvalidTransitionException(order.getStatus(), target);
        }
        if (target == WorkOrderStatus.REJECTED && (request.note() == null || request.note().isBlank())) {
            throw new InvalidRequestException("A reason is required to reject a request");
        }

        WorkOrderStatus from = order.getStatus();
        Instant now = clock.instant();
        if (target == WorkOrderStatus.ASSIGNED) {
            if (request.assigneeId() == null) {
                throw new InvalidRequestException("assigneeId is required to assign a work order");
            }
            PropertyAccessView assignee = properties.memberAccess(caller, order.getPropertyId(), request.assigneeId());
            if (!assignee.isTechnician()) {
                throw new InvalidRequestException("The assignee is not a technician of this property");
            }
            order.assignTo(request.assigneeId(), request.dueAt(), now);
        } else {
            order.transitionTo(target, now);
        }
        history.save(new StatusHistoryEntry(order.getId(), from, target, caller.userId(),
                request.note() == null || request.note().isBlank() ? null : request.note().trim(), now));
        outbox.write("WORK_ORDER_STATUS_CHANGED", caller.userId(), order.getRequesterId(), order.getAssigneeId(),
                order.getId(), "Work order status changed", "Work order status changed from " + from + " to " + target);
        return order;
    }

    public WorkOrder changeDueDate(Caller caller, UUID id, Instant dueAt) {
        Loaded loaded = load(caller, id);
        if (!loaded.access().canManage()) {
            throw new AccessDeniedException("Only property managers can change due dates");
        }
        try {
            loaded.order().changeDueDate(dueAt, clock.instant());
            outbox.write("WORK_ORDER_DUE_DATE_CHANGED", caller.userId(), loaded.order().getRequesterId(),
                    loaded.order().getAssigneeId(), loaded.order().getId(), "Work order updated",
                    "The work order due date changed");
        } catch (IllegalStateException ex) {
            throw new ConflictException(ex.getMessage());
        }
        return loaded.order();
    }

    public WorkOrderComment addComment(Caller caller, UUID id, String body) {
        WorkOrder order = load(caller, id).order();
        WorkOrderComment comment = comments.save(new WorkOrderComment(id, caller.userId(), body.trim(), clock.instant()));
        outbox.write("WORK_ORDER_COMMENT_ADDED", caller.userId(), order.getRequesterId(), order.getAssigneeId(),
                order.getId(), "Work order updated", "A comment was added to a work order");
        return comment;
    }

    @Transactional(readOnly = true)
    public Page<WorkOrderComment> listComments(Caller caller, UUID id, Pageable pageable) {
        load(caller, id);
        return comments.findByWorkOrderId(id, restrictSort(pageable, COMMENT_SORTS, Sort.by("createdAt")));
    }

    @Transactional(readOnly = true)
    public List<StatusHistoryEntry> listHistory(Caller caller, UUID id) {
        load(caller, id);
        return history.findByWorkOrderIdOrderByCreatedAtAsc(id);
    }

    private boolean isPermitted(Caller caller, PropertyAccessView access, WorkOrder order, WorkOrderStatus target) {
        boolean assignee = access.isTechnician() && caller.userId().equals(order.getAssigneeId());
        return switch (target) {
            case OPEN, APPROVED, REJECTED, ASSIGNED -> access.canManage();
            case IN_PROGRESS, ON_HOLD, RESOLVED -> access.canManage() || assignee;
            case CLOSED -> access.canManage() || caller.userId().equals(order.getRequesterId());
        };
    }

    /** Loads the order and applies property-scoped visibility; hidden orders look like they do not exist. */
    private Loaded load(Caller caller, UUID id) {
        WorkOrder order = workOrders.findById(id).orElseThrow(() -> new NotFoundException("Work order not found"));
        PropertyAccessView access = properties.accessOf(caller, order.getPropertyId());
        boolean visible = access.canManage()
                || (access.isTenant() && caller.userId().equals(order.getRequesterId()))
                || (access.isTechnician() && caller.userId().equals(order.getAssigneeId()));
        if (!visible) {
            throw new NotFoundException("Work order not found");
        }
        return new Loaded(order, access);
    }

    private static Pageable restrictSort(Pageable pageable, Set<String> allowed, Sort defaultSort) {
        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), defaultSort);
        }
        pageable.getSort().forEach(order -> {
            if (!allowed.contains(order.getProperty())) {
                throw new InvalidRequestException("Unsupported sort field: " + order.getProperty());
            }
        });
        return pageable;
    }

    private record Loaded(WorkOrder order, PropertyAccessView access) {
    }
}

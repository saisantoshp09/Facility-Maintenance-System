package com.facility.maintenance.seed;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.facility.maintenance.domain.Priority;
import com.facility.maintenance.domain.StatusHistoryEntry;
import com.facility.maintenance.domain.StatusHistoryRepository;
import com.facility.maintenance.domain.WorkOrder;
import com.facility.maintenance.domain.WorkOrderComment;
import com.facility.maintenance.domain.WorkOrderCommentRepository;
import com.facility.maintenance.domain.WorkOrderRepository;
import com.facility.maintenance.domain.WorkOrderStatus;

/**
 * DEV ONLY: inserts synthetic work orders directly through the repositories. It deliberately bypasses
 * WorkOrderService so no outbox events (and therefore no notifications) are produced.
 */
@Component
@Profile("dev-seed")
public class DevSeedRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DevSeedRunner.class);

    // Seed ids are duplicated in every service (no shared lib); keep them in sync.
    public static final UUID MANAGER_ID = UUID.fromString("00000000-0000-4000-8000-0000000000a1");
    public static final UUID TENANT_ID = UUID.fromString("00000000-0000-4000-8000-0000000000a2");
    public static final UUID TECHNICIAN_ID = UUID.fromString("00000000-0000-4000-8000-0000000000a3");
    public static final UUID PROPERTY_ID = UUID.fromString("00000000-0000-4000-8000-00000000b001");
    public static final UUID UNIT_1_ID = UUID.fromString("00000000-0000-4000-8000-00000000b021");
    public static final UUID UNIT_2_ID = UUID.fromString("00000000-0000-4000-8000-00000000b022");
    public static final UUID WORK_ORDER_1_ID = UUID.fromString("00000000-0000-4000-8000-00000000c001");

    private static final UUID[] WORK_ORDER_IDS = new UUID[5];
    static {
        WORK_ORDER_IDS[0] = WORK_ORDER_1_ID;
        for (int i = 1; i < 5; i++) {
            WORK_ORDER_IDS[i] = UUID.fromString("00000000-0000-4000-8000-00000000c00" + (i + 1));
        }
    }

    private final WorkOrderRepository workOrders;
    private final StatusHistoryRepository history;
    private final WorkOrderCommentRepository comments;
    private final TransactionTemplate tx;
    private final Clock clock;

    public DevSeedRunner(WorkOrderRepository workOrders, StatusHistoryRepository history,
            WorkOrderCommentRepository comments, TransactionTemplate tx, Clock clock) {
        this.workOrders = workOrders;
        this.history = history;
        this.comments = comments;
        this.tx = tx;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer created = tx.execute(status -> seed());
        log.info("dev-seed: work orders created={}", created);
    }

    private int seed() {
        Instant now = clock.instant();
        int created = 0;
        created += seedOrder(0, "Lobby light flickering", "Ceiling light in the lobby flickers.",
                Priority.LOW, UNIT_1_ID, now.plus(Duration.ofDays(7)), WorkOrderStatus.OPEN, now);
        created += seedOrder(1, "Leaking tap in unit 102", "Kitchen tap drips constantly.",
                Priority.MEDIUM, UNIT_2_ID, now.plus(Duration.ofDays(5)), WorkOrderStatus.APPROVED, now);
        created += seedOrder(2, "Replace corridor smoke detector", "Detector battery warning on floor 1.",
                Priority.HIGH, null, now.plus(Duration.ofDays(3)), WorkOrderStatus.ASSIGNED, now);
        created += seedOrder(3, "Air conditioning not cooling", "Unit 101 AC blows warm air.",
                Priority.URGENT, UNIT_1_ID, now.minus(Duration.ofDays(2)), WorkOrderStatus.IN_PROGRESS, now);
        created += seedOrder(4, "Repaint stairwell", "Cosmetic repaint completed last week.",
                Priority.LOW, null, now.minus(Duration.ofDays(1)), WorkOrderStatus.CLOSED, now);
        return created;
    }

    private int seedOrder(int index, String title, String description, Priority priority, UUID unitId,
            Instant dueAt, WorkOrderStatus target, Instant now) {
        UUID id = WORK_ORDER_IDS[index];
        if (workOrders.existsById(id)) {
            return 0;
        }
        Instant created = now.minus(Duration.ofDays(10));
        WorkOrder order = new WorkOrder(PROPERTY_ID, unitId, title, description, priority, TENANT_ID, dueAt,
                created);
        setId(order, id);

        Instant at = created;
        WorkOrderStatus current = WorkOrderStatus.OPEN;
        List<StatusHistoryEntry> entries = new ArrayList<>();
        entries.add(new StatusHistoryEntry(id, null, current, TENANT_ID, "Created (synthetic seed)", at));
        for (WorkOrderStatus next : path(target)) {
            at = at.plus(Duration.ofHours(6));
            if (next == WorkOrderStatus.ASSIGNED) {
                order.assignTo(TECHNICIAN_ID, null, at);
            } else {
                order.transitionTo(next, at);
            }
            entries.add(new StatusHistoryEntry(id, current, next, actorFor(next), "Seed transition", at));
            current = next;
        }
        workOrders.save(order);
        history.saveAll(entries);
        if (index == 3) {
            comments.save(new WorkOrderComment(id, TECHNICIAN_ID, "Checking the compressor today.", at));
        }
        return 1;
    }

    private static WorkOrderStatus[] path(WorkOrderStatus target) {
        return switch (target) {
            case OPEN -> new WorkOrderStatus[] {};
            case APPROVED -> new WorkOrderStatus[] { WorkOrderStatus.APPROVED };
            case ASSIGNED -> new WorkOrderStatus[] { WorkOrderStatus.APPROVED, WorkOrderStatus.ASSIGNED };
            case IN_PROGRESS -> new WorkOrderStatus[] { WorkOrderStatus.APPROVED, WorkOrderStatus.ASSIGNED,
                    WorkOrderStatus.IN_PROGRESS };
            case CLOSED -> new WorkOrderStatus[] { WorkOrderStatus.APPROVED, WorkOrderStatus.ASSIGNED,
                    WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.RESOLVED, WorkOrderStatus.CLOSED };
            default -> throw new IllegalArgumentException("Unsupported seed status " + target);
        };
    }

    private static UUID actorFor(WorkOrderStatus status) {
        return switch (status) {
            case IN_PROGRESS, RESOLVED -> TECHNICIAN_ID;
            default -> MANAGER_ID;
        };
    }

    private static void setId(WorkOrder order, UUID id) {
        try {
            Field field = WorkOrder.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(order, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot assign seed id", e);
        }
    }
}

package com.facility.maintenance.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class WorkOrderTest {
    final Instant now = Instant.parse("2026-01-01T00:00:00Z");

    WorkOrder order() {
        return new WorkOrder(UUID.randomUUID(), null, "t", "d", Priority.LOW, UUID.randomUUID(), null, now);
    }

    @Test
    void followsTheFullLifecycle() {
        WorkOrder w = order();
        w.transitionTo(WorkOrderStatus.APPROVED, now);
        w.assignTo(UUID.randomUUID(), now.plus(1, ChronoUnit.DAYS), now);
        w.transitionTo(WorkOrderStatus.IN_PROGRESS, now);
        w.transitionTo(WorkOrderStatus.ON_HOLD, now);
        w.transitionTo(WorkOrderStatus.IN_PROGRESS, now);
        w.transitionTo(WorkOrderStatus.RESOLVED, now);
        w.transitionTo(WorkOrderStatus.CLOSED, now);
        assertEquals(WorkOrderStatus.CLOSED, w.getStatus());
    }

    @Test
    void rejectsEverySkippedOrBackwardsTransition() {
        for (WorkOrderStatus from : WorkOrderStatus.values()) {
            for (WorkOrderStatus to : WorkOrderStatus.values()) {
                if (from.allowedNext().contains(to)) {
                    continue;
                }
                WorkOrder w = order();
                walkTo(w, from);
                assertThrows(InvalidTransitionException.class, () -> w.transitionTo(to, now), from + "->" + to);
            }
        }
    }

    @Test
    void overdueOnlyWhilePastDueAndNotDone() {
        WorkOrder w = new WorkOrder(UUID.randomUUID(), null, "t", "d", Priority.LOW, UUID.randomUUID(),
                now.minus(1, ChronoUnit.DAYS), now);
        assertTrue(w.isOverdue(now));
        walkTo(w, WorkOrderStatus.RESOLVED);
        assertFalse(w.isOverdue(now));
    }

    private void walkTo(WorkOrder w, WorkOrderStatus target) {
        WorkOrderStatus[] path = {WorkOrderStatus.APPROVED, WorkOrderStatus.ASSIGNED, WorkOrderStatus.IN_PROGRESS,
                WorkOrderStatus.ON_HOLD, WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.RESOLVED,
                WorkOrderStatus.CLOSED};
        if (target == WorkOrderStatus.OPEN) {
            return;
        }
        if (target == WorkOrderStatus.REJECTED) {
            w.transitionTo(WorkOrderStatus.REJECTED, now);
            return;
        }
        for (WorkOrderStatus step : path) {
            if (step == WorkOrderStatus.ASSIGNED) {
                w.assignTo(UUID.randomUUID(), null, now);
            } else {
                w.transitionTo(step, now);
            }
            if (step == target && !(step == WorkOrderStatus.IN_PROGRESS && w.getStatus() != target)) {
                return;
            }
        }
    }
}

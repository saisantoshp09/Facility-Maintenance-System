package com.facility.maintenance.domain;

import java.util.EnumSet;
import java.util.Set;

public enum WorkOrderStatus {
    OPEN, APPROVED, REJECTED, ASSIGNED, IN_PROGRESS, ON_HOLD, RESOLVED, CLOSED;

    /** Lifecycle: OPEN → APPROVED → ASSIGNED → IN_PROGRESS ⇄ ON_HOLD → RESOLVED → CLOSED; OPEN → REJECTED. */
    public Set<WorkOrderStatus> allowedNext() {
        return switch (this) {
            case OPEN -> EnumSet.of(APPROVED, REJECTED);
            case APPROVED -> EnumSet.of(ASSIGNED);
            case ASSIGNED -> EnumSet.of(IN_PROGRESS);
            case IN_PROGRESS -> EnumSet.of(ON_HOLD, RESOLVED);
            case ON_HOLD -> EnumSet.of(IN_PROGRESS);
            case RESOLVED -> EnumSet.of(CLOSED);
            case REJECTED, CLOSED -> EnumSet.noneOf(WorkOrderStatus.class);
        };
    }

    public boolean isTerminalOrDone() {
        return this == RESOLVED || this == CLOSED || this == REJECTED;
    }
}

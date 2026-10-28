package com.facility.maintenance.domain;

public class InvalidTransitionException extends RuntimeException {
    public InvalidTransitionException(WorkOrderStatus from, WorkOrderStatus to) {
        super("Cannot change status from " + from + " to " + to);
    }
}

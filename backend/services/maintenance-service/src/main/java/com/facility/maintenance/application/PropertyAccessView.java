package com.facility.maintenance.application;

import java.util.Set;
import java.util.UUID;

/** Answer from the property service (the source of truth for property-scoped access). */
public record PropertyAccessView(UUID propertyId, boolean systemAdmin, Set<String> roles, Set<UUID> unitIds) {
    public boolean canManage() {
        return systemAdmin || roles.contains("PROPERTY_MANAGER");
    }

    public boolean isTenant() {
        return roles.contains("TENANT");
    }

    public boolean isTechnician() {
        return roles.contains("TECHNICIAN");
    }
}

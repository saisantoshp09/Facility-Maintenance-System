package com.facility.asset.application;

import java.util.Set;
import java.util.UUID;

public record PropertyAccessView(UUID propertyId, boolean systemAdmin, Set<String> roles, Set<UUID> unitIds) {
    public boolean canManage() { return systemAdmin || roles.contains("PROPERTY_MANAGER"); }
    public boolean isTechnician() { return roles.contains("TECHNICIAN"); }
    public boolean isTenant() { return roles.contains("TENANT"); }
}

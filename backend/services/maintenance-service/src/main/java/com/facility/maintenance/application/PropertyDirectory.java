package com.facility.maintenance.application;

import java.util.UUID;

/** Port to the property service. Implementations must throw NotFoundException when access is not granted. */
public interface PropertyDirectory {
    PropertyAccessView accessOf(Caller caller, UUID propertyId);

    /** Roles another user holds in the property; the caller must be allowed to manage it. */
    PropertyAccessView memberAccess(Caller caller, UUID propertyId, UUID userId);
}

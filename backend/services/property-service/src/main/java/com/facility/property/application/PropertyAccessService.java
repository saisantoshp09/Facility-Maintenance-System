package com.facility.property.application;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.facility.property.domain.MembershipRole;
import com.facility.property.domain.PropertyMembership;
import com.facility.property.domain.PropertyMembershipRepository;
import com.facility.property.domain.PropertyRepository;

/**
 * Single place where property-scoped authorization is decided. Callers who have no relationship to a
 * property get NotFoundException (404) so the existence of other properties is not disclosed.
 */
@Service
@Transactional(readOnly = true)
public class PropertyAccessService {
    private final PropertyRepository properties;
    private final PropertyMembershipRepository memberships;

    public PropertyAccessService(PropertyRepository properties, PropertyMembershipRepository memberships) {
        this.properties = properties;
        this.memberships = memberships;
    }

    public PropertyAccess requireAccess(Caller caller, UUID propertyId) {
        if (!properties.existsById(propertyId)) {
            throw new NotFoundException("Property not found");
        }
        List<PropertyMembership> held = memberships.findByPropertyIdAndUserId(propertyId, caller.userId());
        if (!caller.systemAdmin() && held.isEmpty()) {
            throw new NotFoundException("Property not found");
        }
        Set<MembershipRole> roles = held.stream().map(PropertyMembership::getRole).collect(Collectors.toSet());
        Set<UUID> unitIds = held.stream().map(PropertyMembership::getUnitId)
                .filter(id -> id != null).collect(Collectors.toSet());
        return new PropertyAccess(propertyId, caller.systemAdmin(), roles, unitIds);
    }

    /** Roles another user holds in a property; requires the caller to manage it. Never reports admin rights. */
    public PropertyAccess requireMemberAccess(Caller caller, UUID propertyId, UUID userId) {
        requireManager(caller, propertyId);
        List<PropertyMembership> held = memberships.findByPropertyIdAndUserId(propertyId, userId);
        Set<MembershipRole> roles = held.stream().map(PropertyMembership::getRole).collect(Collectors.toSet());
        Set<UUID> unitIds = held.stream().map(PropertyMembership::getUnitId)
                .filter(id -> id != null).collect(Collectors.toSet());
        return new PropertyAccess(propertyId, false, roles, unitIds);
    }

    public PropertyAccess requireManager(Caller caller, UUID propertyId) {
        PropertyAccess access = requireAccess(caller, propertyId);
        if (!access.canManage()) {
            throw new AccessDeniedException("Property management rights are required");
        }
        return access;
    }

    public record PropertyAccess(UUID propertyId, boolean systemAdmin, Set<MembershipRole> roles, Set<UUID> unitIds) {
        public boolean canManage() {
            return systemAdmin || roles.contains(MembershipRole.PROPERTY_MANAGER);
        }

        /** True when the caller is restricted to specific units (tenant only). */
        public boolean restrictedToUnits() {
            return !canManage() && !roles.contains(MembershipRole.TECHNICIAN);
        }
    }
}

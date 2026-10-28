package com.facility.property.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyMembershipRepository extends JpaRepository<PropertyMembership, UUID> {
    List<PropertyMembership> findByPropertyIdAndUserId(UUID propertyId, UUID userId);

    Page<PropertyMembership> findByPropertyId(UUID propertyId, Pageable pageable);

    Optional<PropertyMembership> findByIdAndPropertyId(UUID id, UUID propertyId);

    boolean existsByPropertyIdAndUserIdAndRoleAndUnitId(UUID propertyId, UUID userId, MembershipRole role, UUID unitId);

    boolean existsByPropertyIdAndUserIdAndRoleAndUnitIdIsNull(UUID propertyId, UUID userId, MembershipRole role);
}

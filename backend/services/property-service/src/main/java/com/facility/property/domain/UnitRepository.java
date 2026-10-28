package com.facility.property.domain;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnitRepository extends JpaRepository<Unit, UUID> {
    Page<Unit> findByPropertyId(UUID propertyId, Pageable pageable);

    Page<Unit> findByPropertyIdAndIdIn(UUID propertyId, Collection<UUID> ids, Pageable pageable);

    Optional<Unit> findByIdAndPropertyId(UUID id, UUID propertyId);

    boolean existsByPropertyIdAndUnitNumber(UUID propertyId, String unitNumber);
}

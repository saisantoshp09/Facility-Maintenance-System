package com.facility.property.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuildingRepository extends JpaRepository<Building, UUID> {
    Page<Building> findByPropertyId(UUID propertyId, Pageable pageable);

    Optional<Building> findByIdAndPropertyId(UUID id, UUID propertyId);

    boolean existsByPropertyIdAndName(UUID propertyId, String name);
}

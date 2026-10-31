package com.facility.property.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FloorRepository extends JpaRepository<Floor, UUID> {
    List<Floor> findByBuildingIdAndPropertyIdOrderByLevelNumber(UUID buildingId, UUID propertyId);

    Optional<Floor> findByIdAndPropertyId(UUID id, UUID propertyId);

    boolean existsByBuildingIdAndLevelNumber(UUID buildingId, int levelNumber);
}

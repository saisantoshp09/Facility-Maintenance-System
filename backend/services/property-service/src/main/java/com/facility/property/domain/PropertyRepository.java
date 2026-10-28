package com.facility.property.domain;

import java.util.Collection;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PropertyRepository extends JpaRepository<Property, UUID> {
    @Query("select p from Property p where p.id in "
            + "(select m.propertyId from PropertyMembership m where m.userId = :userId)")
    Page<Property> findAccessibleTo(@Param("userId") UUID userId, Pageable pageable);

    Page<Property> findByIdIn(Collection<UUID> ids, Pageable pageable);
}

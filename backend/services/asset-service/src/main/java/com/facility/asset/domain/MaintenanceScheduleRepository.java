package com.facility.asset.domain;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface MaintenanceScheduleRepository extends JpaRepository<MaintenanceSchedule,UUID> {
    List<MaintenanceSchedule> findByAssetId(UUID assetId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from MaintenanceSchedule s where s.id=:id") Optional<MaintenanceSchedule> findLockedById(@Param("id") UUID id);
}

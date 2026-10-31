package com.facility.asset.domain;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
public interface MaintenanceTaskRepository extends JpaRepository<MaintenanceTask,UUID>,JpaSpecificationExecutor<MaintenanceTask> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select t from MaintenanceTask t where t.id=:id") Optional<MaintenanceTask> findLockedById(@Param("id") UUID id);
    Optional<MaintenanceTask> findFirstByScheduleIdAndStatus(UUID scheduleId,TaskStatus status);
    List<MaintenanceTask> findByScheduleIdAndStatus(UUID scheduleId,TaskStatus status);
    List<MaintenanceTask> findByPropertyIdAndStatusAndDueDateBetweenOrderByDueDateAsc(UUID propertyId,TaskStatus status,LocalDate from,LocalDate to);
}

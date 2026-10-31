package com.facility.asset.domain;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
class MaintenanceScheduleTest {
    @Test void firstDueDateIsStartAndCompletionAdvancesFromActualCompletion(){
        LocalDate start=LocalDate.parse("2026-01-10"); MaintenanceSchedule s=new MaintenanceSchedule(UUID.randomUUID(),"Filter","Replace filter",30,start);
        assertThat(s.getNextDueDate()).isEqualTo(start);
        assertThat(s.completeOn(start.plusDays(2))).isEqualTo(start.plusDays(32));
        assertThat(s.getLastCompletedDate()).isEqualTo(start.plusDays(2));
    }
    @Test void intervalMustBePositive(){assertThatIllegalArgumentException().isThrownBy(()->new MaintenanceSchedule(UUID.randomUUID(),"x","x",0,LocalDate.now()));}
    @Test void deactivatedScheduleCannotBeCompleted(){MaintenanceSchedule s=new MaintenanceSchedule(UUID.randomUUID(),"x","x",1,LocalDate.now());s.deactivate();assertThatIllegalStateException().isThrownBy(()->s.completeOn(LocalDate.now()));}
    @Test void onlyPendingTaskMayBeCompleted(){MaintenanceTask t=new MaintenanceTask(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),LocalDate.now());t.complete(java.time.Instant.now());assertThatIllegalStateException().isThrownBy(()->t.complete(java.time.Instant.now()));}
}

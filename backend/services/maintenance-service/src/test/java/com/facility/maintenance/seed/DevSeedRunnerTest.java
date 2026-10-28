package com.facility.maintenance.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import com.facility.maintenance.domain.OutboxEventRepository;
import com.facility.maintenance.domain.StatusHistoryRepository;
import com.facility.maintenance.domain.WorkOrder;
import com.facility.maintenance.domain.WorkOrderRepository;
import com.facility.maintenance.domain.WorkOrderStatus;

class DevSeedRunnerTest {

    @Nested
    @SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:maintenance-seed;MODE=MySQL;DB_CLOSE_DELAY=-1")
    @ActiveProfiles({ "test", "dev-seed" })
    class WithProfile {
        @Autowired WorkOrderRepository orders;
        @Autowired StatusHistoryRepository history;
        @Autowired OutboxEventRepository outbox;
        @Autowired DevSeedRunner runner;

        @Test
        void seedsIdempotentlyWithoutOutboxEvents() throws Exception {
            long historyRows = history.count();
            assertThat(orders.count()).isEqualTo(5);
            runner.run(null);
            assertThat(orders.count()).isEqualTo(5);
            assertThat(history.count()).isEqualTo(historyRows).isGreaterThanOrEqualTo(5);
            assertThat(outbox.count()).isZero();

            List<WorkOrder> all = orders.findAll();
            assertThat(all).extracting(WorkOrder::getStatus).containsExactlyInAnyOrder(
                    WorkOrderStatus.OPEN, WorkOrderStatus.APPROVED, WorkOrderStatus.ASSIGNED,
                    WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.CLOSED);
            assertThat(all.stream().filter(o -> o.isOverdue(Instant.now())).count()).isEqualTo(1);
            assertThat(all.stream().filter(o -> o.getStatus() == WorkOrderStatus.ASSIGNED).findFirst()
                    .orElseThrow().getAssigneeId()).isEqualTo(DevSeedRunner.TECHNICIAN_ID);
        }
    }

    @Nested
    @SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:maintenance-noseed;MODE=MySQL;DB_CLOSE_DELAY=-1")
    @ActiveProfiles("test")
    class WithoutProfile {
        @Autowired WorkOrderRepository orders;
        @Autowired ApplicationContext context;

        @Test
        void doesNotRunByDefault() {
            assertThat(context.getBeanNamesForType(DevSeedRunner.class)).isEmpty();
            assertThat(orders.count()).isZero();
        }
    }
}

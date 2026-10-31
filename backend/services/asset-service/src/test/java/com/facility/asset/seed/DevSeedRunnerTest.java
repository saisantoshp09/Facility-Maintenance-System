package com.facility.asset.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import com.facility.asset.domain.AssetRepository;
import com.facility.asset.domain.MaintenanceScheduleRepository;
import com.facility.asset.domain.MaintenanceTask;
import com.facility.asset.domain.MaintenanceTaskRepository;
import com.facility.asset.domain.ServiceRecordRepository;
import com.facility.asset.domain.TaskStatus;

class DevSeedRunnerTest {

    @Nested
    @SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:asset-seed;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
    @ActiveProfiles({ "test", "dev-seed" })
    class WithProfile {
        @Autowired AssetRepository assets;
        @Autowired MaintenanceScheduleRepository schedules;
        @Autowired MaintenanceTaskRepository tasks;
        @Autowired ServiceRecordRepository records;
        @Autowired DevSeedRunner runner;

        @Test
        void seedsIdempotently() throws Exception {
            assertCounts();
            runner.run(null);
            assertCounts();
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            long overduePending = tasks.findAll().stream()
                    .filter(t -> t.getStatus() == TaskStatus.PENDING && t.getDueDate().isBefore(today)).count();
            assertThat(overduePending).isEqualTo(1);
            assertThat(tasks.findAll().stream().filter(t -> t.getStatus() == TaskStatus.PENDING)
                    .map(MaintenanceTask::getId)).hasSize(3);
        }

        private void assertCounts() {
            assertThat(assets.count()).isEqualTo(3);
            assertThat(schedules.count()).isEqualTo(3);
            assertThat(tasks.count()).isEqualTo(4);
            assertThat(records.count()).isEqualTo(1);
        }
    }

    @Nested
    @SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:asset-noseed;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
    @ActiveProfiles("test")
    class WithoutProfile {
        @Autowired AssetRepository assets;
        @Autowired ApplicationContext context;

        @Test
        void doesNotRunByDefault() {
            assertThat(context.getBeanNamesForType(DevSeedRunner.class)).isEmpty();
            assertThat(assets.count()).isZero();
        }
    }
}

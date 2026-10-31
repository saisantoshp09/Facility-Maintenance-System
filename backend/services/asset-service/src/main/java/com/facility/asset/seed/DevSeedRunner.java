package com.facility.asset.seed;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.facility.asset.domain.Asset;
import com.facility.asset.domain.AssetRepository;
import com.facility.asset.domain.MaintenanceSchedule;
import com.facility.asset.domain.MaintenanceScheduleRepository;
import com.facility.asset.domain.MaintenanceTask;
import com.facility.asset.domain.MaintenanceTaskRepository;
import com.facility.asset.domain.ServiceRecord;
import com.facility.asset.domain.ServiceRecordRepository;

/** DEV ONLY: inserts synthetic assets, schedules and tasks. Active only with the "dev-seed" profile. */
@Component
@Profile("dev-seed")
public class DevSeedRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DevSeedRunner.class);

    // Seed ids are duplicated in every service (no shared lib); keep them in sync.
    public static final UUID TECHNICIAN_ID = UUID.fromString("00000000-0000-4000-8000-0000000000a3");
    public static final UUID PROPERTY_ID = UUID.fromString("00000000-0000-4000-8000-00000000b001");
    public static final UUID HVAC_ID = UUID.fromString("00000000-0000-4000-8000-00000000d001");
    public static final UUID ELEVATOR_ID = UUID.fromString("00000000-0000-4000-8000-00000000d002");
    public static final UUID GENERATOR_ID = UUID.fromString("00000000-0000-4000-8000-00000000d003");

    private final AssetRepository assets;
    private final MaintenanceScheduleRepository schedules;
    private final MaintenanceTaskRepository tasks;
    private final ServiceRecordRepository records;
    private final TransactionTemplate tx;
    private final Clock clock;

    public DevSeedRunner(AssetRepository assets, MaintenanceScheduleRepository schedules,
            MaintenanceTaskRepository tasks, ServiceRecordRepository records, TransactionTemplate tx,
            Clock clock) {
        this.assets = assets;
        this.schedules = schedules;
        this.tasks = tasks;
        this.records = records;
        this.tx = tx;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer created = tx.execute(status -> seed());
        log.info("dev-seed: asset rows created={}", created);
    }

    private int seed() {
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        int created = 0;
        if (!assets.existsById(HVAC_ID)) {
            saveAsset(HVAC_ID, "Rooftop HVAC Unit", "HVAC", "SYN-HVAC-0001", "Roof", today.minusYears(3), now);
            UUID schedule = saveSchedule(0xe1, HVAC_ID, "Quarterly HVAC service", 90, today.minusDays(95));
            UUID done = seedId(0xf1);
            MaintenanceTask completed = setId(new MaintenanceTask(schedule, HVAC_ID, PROPERTY_ID,
                    today.minusDays(95)), done);
            completed.complete(now.minusSeconds(93L * 86_400));
            tasks.save(completed);
            ServiceRecord record = new ServiceRecord(HVAC_ID, done, TECHNICIAN_ID, today.minusDays(93),
                    "Filters replaced, coolant checked (synthetic).", new BigDecimal("150.00"), now);
            records.save(setId(record, seedId(0x101)));
            saveTask(0xf2, schedule, HVAC_ID, today.minusDays(5));
            created += 5;
        }
        if (!assets.existsById(ELEVATOR_ID)) {
            saveAsset(ELEVATOR_ID, "Main Elevator", "ELEVATOR", "SYN-ELEV-0001", "Core A", today.minusYears(5), now);
            UUID schedule = saveSchedule(0xe2, ELEVATOR_ID, "Monthly elevator inspection", 30, today.plusDays(14));
            saveTask(0xf3, schedule, ELEVATOR_ID, today.plusDays(14));
            created += 3;
        }
        if (!assets.existsById(GENERATOR_ID)) {
            saveAsset(GENERATOR_ID, "Backup Generator", "GENERATOR", "SYN-GEN-0001", "Basement", today.minusYears(2),
                    now);
            UUID schedule = saveSchedule(0xe3, GENERATOR_ID, "Generator load test", 60, today.plusDays(30));
            saveTask(0xf4, schedule, GENERATOR_ID, today.plusDays(30));
            created += 3;
        }
        return created;
    }

    private void saveAsset(UUID id, String name, String category, String serial, String location,
            LocalDate installedOn, Instant now) {
        Asset asset = new Asset(PROPERTY_ID, null, name, category, serial, location, installedOn, now);
        assets.save(setId(asset, id));
    }

    private UUID saveSchedule(int n, UUID assetId, String title, int intervalDays, LocalDate start) {
        UUID id = seedId(n);
        schedules.save(setId(new MaintenanceSchedule(assetId, title, "Synthetic seed schedule", intervalDays, start),
                id));
        return id;
    }

    private UUID saveTask(int n, UUID scheduleId, UUID assetId, LocalDate due) {
        UUID id = seedId(n);
        tasks.save(setId(new MaintenanceTask(scheduleId, assetId, PROPERTY_ID, due), id));
        return id;
    }

    private static UUID seedId(int n) {
        return UUID.fromString(String.format("00000000-0000-4000-8000-00000000%04x", n));
    }

    private static <T> T setId(T entity, UUID id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot assign seed id", e);
        }
    }
}

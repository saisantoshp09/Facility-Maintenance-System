package com.facility.property.seed;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.facility.property.domain.Building;
import com.facility.property.domain.BuildingRepository;
import com.facility.property.domain.Floor;
import com.facility.property.domain.FloorRepository;
import com.facility.property.domain.MembershipRole;
import com.facility.property.domain.Property;
import com.facility.property.domain.PropertyMembership;
import com.facility.property.domain.PropertyMembershipRepository;
import com.facility.property.domain.PropertyRepository;
import com.facility.property.domain.Unit;
import com.facility.property.domain.UnitRepository;

/** DEV ONLY: inserts a synthetic demo property. Active only with the "dev-seed" profile. */
@Component
@Profile("dev-seed")
public class DevSeedRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DevSeedRunner.class);

    // Seed ids are duplicated in every service (no shared lib); keep them in sync.
    public static final UUID MANAGER_ID = UUID.fromString("00000000-0000-4000-8000-0000000000a1");
    public static final UUID TENANT_ID = UUID.fromString("00000000-0000-4000-8000-0000000000a2");
    public static final UUID TECHNICIAN_ID = UUID.fromString("00000000-0000-4000-8000-0000000000a3");
    public static final UUID PROPERTY_ID = UUID.fromString("00000000-0000-4000-8000-00000000b001");
    public static final UUID BUILDING_ID = UUID.fromString("00000000-0000-4000-8000-00000000b002");
    public static final UUID FLOOR_1_ID = UUID.fromString("00000000-0000-4000-8000-00000000b011");
    public static final UUID FLOOR_2_ID = UUID.fromString("00000000-0000-4000-8000-00000000b012");
    public static final UUID UNIT_1_ID = UUID.fromString("00000000-0000-4000-8000-00000000b021");
    public static final UUID UNIT_2_ID = UUID.fromString("00000000-0000-4000-8000-00000000b022");
    public static final UUID UNIT_3_ID = UUID.fromString("00000000-0000-4000-8000-00000000b023");
    private static final UUID MEMBERSHIP_MANAGER_ID = UUID.fromString("00000000-0000-4000-8000-00000000b031");
    private static final UUID MEMBERSHIP_TENANT_ID = UUID.fromString("00000000-0000-4000-8000-00000000b032");
    private static final UUID MEMBERSHIP_TECHNICIAN_ID = UUID.fromString("00000000-0000-4000-8000-00000000b033");

    private final PropertyRepository properties;
    private final BuildingRepository buildings;
    private final FloorRepository floors;
    private final UnitRepository units;
    private final PropertyMembershipRepository memberships;
    private final TransactionTemplate tx;
    private final Clock clock;

    public DevSeedRunner(PropertyRepository properties, BuildingRepository buildings, FloorRepository floors,
            UnitRepository units, PropertyMembershipRepository memberships, TransactionTemplate tx, Clock clock) {
        this.properties = properties;
        this.buildings = buildings;
        this.floors = floors;
        this.units = units;
        this.memberships = memberships;
        this.tx = tx;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer created = tx.execute(status -> seed());
        log.info("dev-seed: property rows created={}", created);
    }

    private int seed() {
        if (properties.existsById(PROPERTY_ID)) {
            return 0;
        }
        Instant now = clock.instant();
        properties.save(withId(new Property("Demo Plaza", "1 Example Street", "Exampleville", "00000", "XX", now),
                PROPERTY_ID));
        buildings.save(withId(new Building(PROPERTY_ID, "Main Building", now), BUILDING_ID));
        floors.save(withId(new Floor(PROPERTY_ID, BUILDING_ID, 1, "Ground", now), FLOOR_1_ID));
        floors.save(withId(new Floor(PROPERTY_ID, BUILDING_ID, 2, "First", now), FLOOR_2_ID));
        units.save(withId(new Unit(PROPERTY_ID, BUILDING_ID, FLOOR_1_ID, "101", now), UNIT_1_ID));
        units.save(withId(new Unit(PROPERTY_ID, BUILDING_ID, FLOOR_1_ID, "102", now), UNIT_2_ID));
        units.save(withId(new Unit(PROPERTY_ID, BUILDING_ID, FLOOR_2_ID, "201", now), UNIT_3_ID));
        memberships.save(withId(new PropertyMembership(PROPERTY_ID, MANAGER_ID, MembershipRole.PROPERTY_MANAGER,
                null, now), MEMBERSHIP_MANAGER_ID));
        memberships.save(withId(new PropertyMembership(PROPERTY_ID, TENANT_ID, MembershipRole.TENANT,
                UNIT_1_ID, now), MEMBERSHIP_TENANT_ID));
        memberships.save(withId(new PropertyMembership(PROPERTY_ID, TECHNICIAN_ID, MembershipRole.TECHNICIAN,
                null, now), MEMBERSHIP_TECHNICIAN_ID));
        return 10;
    }

    private static <T> T withId(T entity, UUID id) {
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

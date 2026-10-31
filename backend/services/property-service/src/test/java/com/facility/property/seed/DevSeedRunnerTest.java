package com.facility.property.seed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import com.facility.property.domain.BuildingRepository;
import com.facility.property.domain.FloorRepository;
import com.facility.property.domain.MembershipRole;
import com.facility.property.domain.PropertyMembership;
import com.facility.property.domain.PropertyMembershipRepository;
import com.facility.property.domain.PropertyRepository;
import com.facility.property.domain.UnitRepository;

class DevSeedRunnerTest {

    @Nested
    @SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:property-seed;MODE=MySQL;DB_CLOSE_DELAY=-1")
    @ActiveProfiles({ "test", "dev-seed" })
    class WithProfile {
        @Autowired PropertyRepository properties;
        @Autowired BuildingRepository buildings;
        @Autowired FloorRepository floors;
        @Autowired UnitRepository units;
        @Autowired PropertyMembershipRepository memberships;
        @Autowired DevSeedRunner runner;

        @Test
        void seedsIdempotently() throws Exception {
            assertCounts();
            runner.run(null);
            assertCounts();
            PropertyMembership tenant = memberships.findAll().stream()
                    .filter(m -> m.getRole() == MembershipRole.TENANT).findFirst().orElseThrow();
            assertThat(tenant.getUserId()).isEqualTo(DevSeedRunner.TENANT_ID);
            assertThat(tenant.getUnitId()).isEqualTo(DevSeedRunner.UNIT_1_ID);
        }

        private void assertCounts() {
            assertThat(properties.count()).isEqualTo(1);
            assertThat(buildings.count()).isEqualTo(1);
            assertThat(floors.count()).isEqualTo(2);
            assertThat(units.count()).isEqualTo(3);
            assertThat(memberships.count()).isEqualTo(3);
        }
    }

    @Nested
    @SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:property-noseed;MODE=MySQL;DB_CLOSE_DELAY=-1")
    @ActiveProfiles("test")
    class WithoutProfile {
        @Autowired PropertyRepository properties;
        @Autowired ApplicationContext context;

        @Test
        void doesNotRunByDefault() {
            assertThat(context.getBeanNamesForType(DevSeedRunner.class)).isEmpty();
            assertThat(properties.existsById(DevSeedRunner.PROPERTY_ID)).isFalse();
        }
    }
}

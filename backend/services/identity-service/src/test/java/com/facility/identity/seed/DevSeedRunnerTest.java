package com.facility.identity.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.facility.identity.domain.Role;
import com.facility.identity.domain.UserAccount;
import com.facility.identity.domain.UserAccountRepository;

class DevSeedRunnerTest {

    @SpringBootTest(properties = {
            "DEV_SEED_PASSWORD=synthetic-test-pass-1",
            "spring.datasource.url=jdbc:h2:mem:identity-seed;MODE=MySQL;DB_CLOSE_DELAY=-1" })
    @ActiveProfiles({ "test", "dev-seed" })
    @Nested
    class WithProfile {
        @Autowired UserAccountRepository users;
        @Autowired DevSeedRunner runner;
        @Autowired PasswordEncoder encoder;

        @Test
        void seedsUsersIdempotently() throws Exception {
            assertThat(users.count()).isEqualTo(3);
            runner.run(null);
            assertThat(users.count()).isEqualTo(3);
            UserAccount tech = users.findById(DevSeedRunner.TECHNICIAN_ID).orElseThrow();
            assertThat(tech.getEmail()).isEqualTo("tech@example.test");
            assertThat(tech.isActive()).isTrue();
            assertThat(tech.getRoles()).isEqualTo(Set.of(Role.TECHNICIAN));
            assertThat(encoder.matches("synthetic-test-pass-1", tech.getPasswordHash())).isTrue();
            assertThat(users.findById(DevSeedRunner.MANAGER_ID).orElseThrow().getRoles())
                    .containsExactly(Role.PROPERTY_MANAGER);
        }
    }

    @SpringBootTest(properties = {
            "DEV_SEED_PASSWORD=short",
            "spring.datasource.url=jdbc:h2:mem:identity-seed-short;MODE=MySQL;DB_CLOSE_DELAY=-1" })
    @ActiveProfiles({ "test", "dev-seed" })
    @Nested
    class WeakPassword {
        @Autowired UserAccountRepository users;

        @Test
        void skipsWhenPasswordTooShort() {
            assertThat(users.count()).isZero();
        }
    }

    @SpringBootTest(properties = {
            "DEV_SEED_PASSWORD=synthetic-test-pass-1",
            "spring.datasource.url=jdbc:h2:mem:identity-noseed;MODE=MySQL;DB_CLOSE_DELAY=-1" })
    @ActiveProfiles("test")
    @Nested
    class WithoutProfile {
        @Autowired UserAccountRepository users;
        @Autowired ApplicationContext context;

        @Test
        void doesNotRunByDefault() {
            assertThat(context.getBeanNamesForType(DevSeedRunner.class)).isEmpty();
            assertThat(users.existsById(DevSeedRunner.MANAGER_ID)).isFalse();
        }
    }
}

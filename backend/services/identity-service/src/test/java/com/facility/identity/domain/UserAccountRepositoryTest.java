package com.facility.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserAccountRepositoryTest {
    @Autowired UserAccountRepository repository;
    @Autowired TestEntityManager entityManager;

    @Test
    void persistsRolesAndFindsByEmail() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        UserAccount user = UserAccount.create("a@example.test", "A", "hash",
                Set.of(Role.TENANT, Role.TECHNICIAN), now);
        repository.saveAndFlush(user);
        entityManager.clear();

        UserAccount found = repository.findByEmail("a@example.test").orElseThrow();
        assertThat(found.getId()).isEqualTo(user.getId());
        assertThat(found.getRoles()).containsExactlyInAnyOrder(Role.TENANT, Role.TECHNICIAN);
        assertThat(found.getCreatedAt()).isEqualTo(now);
        assertThat(repository.existsByEmail("missing@example.test")).isFalse();
    }
}

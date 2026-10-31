package com.facility.identity.seed;

import java.lang.reflect.Field;
import java.time.Clock;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.facility.identity.domain.Role;
import com.facility.identity.domain.UserAccount;
import com.facility.identity.domain.UserAccountRepository;

/** DEV ONLY: inserts synthetic demo users. Active only with the "dev-seed" profile. */
@Component
@Profile("dev-seed")
public class DevSeedRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DevSeedRunner.class);

    // Seed ids are duplicated in every service (no shared lib); keep them in sync.
    public static final UUID MANAGER_ID = UUID.fromString("00000000-0000-4000-8000-0000000000a1");
    public static final UUID TENANT_ID = UUID.fromString("00000000-0000-4000-8000-0000000000a2");
    public static final UUID TECHNICIAN_ID = UUID.fromString("00000000-0000-4000-8000-0000000000a3");

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String password;

    public DevSeedRunner(UserAccountRepository users, PasswordEncoder passwordEncoder, Clock clock,
            @Value("${DEV_SEED_PASSWORD:}") String password) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (password == null || password.length() < 12 || password.length() > 72) {
            log.warn("dev-seed: DEV_SEED_PASSWORD must be 12-72 characters; identity seed skipped");
            return;
        }
        int created = 0;
        created += seed(MANAGER_ID, "manager@example.test", "Demo Manager", Role.PROPERTY_MANAGER);
        created += seed(TENANT_ID, "tenant@example.test", "Demo Tenant", Role.TENANT);
        created += seed(TECHNICIAN_ID, "tech@example.test", "Demo Technician", Role.TECHNICIAN);
        log.info("dev-seed: identity users created={}", created);
    }

    private int seed(UUID id, String email, String displayName, Role role) {
        if (users.existsById(id) || users.existsByEmail(email)) {
            return 0;
        }
        UserAccount user = UserAccount.create(email, displayName, passwordEncoder.encode(password),
                Set.of(role), clock.instant());
        try {
            Field field = UserAccount.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot assign seed id", e);
        }
        users.save(user);
        return 1;
    }
}

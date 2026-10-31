package com.facility.identity.security;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.facility.identity.application.UserAdministrationService;
import com.facility.identity.domain.Role;
import com.facility.identity.domain.UserAccountRepository;

@Component
public class BootstrapAdminRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final BootstrapAdminProperties properties;
    private final UserAccountRepository users;
    private final UserAdministrationService administration;

    public BootstrapAdminRunner(BootstrapAdminProperties properties, UserAccountRepository users,
            UserAdministrationService administration) {
        this.properties = properties;
        this.users = users;
        this.administration = administration;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isConfigured() || users.count() > 0) {
            return;
        }
        if (properties.password().length() < 12 || properties.password().length() > 72) {
            throw new IllegalStateException("identity.bootstrap-admin.password must be 12-72 characters");
        }
        administration.createUser(null, properties.email(), "Bootstrap Administrator",
                properties.password(), Set.of(Role.SYSTEM_ADMIN));
        log.info("Created bootstrap administrator account");
    }
}

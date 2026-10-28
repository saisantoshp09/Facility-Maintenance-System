package com.facility.identity.application;

import java.time.Clock;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.facility.identity.domain.AccountStatus;
import com.facility.identity.domain.Role;
import com.facility.identity.domain.UserAccount;
import com.facility.identity.domain.UserAccountRepository;

@Service
@Transactional
public class UserAdministrationService {
    private static final Set<String> SORTABLE = Set.of("email", "displayName", "status", "createdAt");

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;
    private final Clock clock;

    public UserAdministrationService(UserAccountRepository users, PasswordEncoder passwordEncoder,
            AuditService audit, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
        this.clock = clock;
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public UserAccount createUser(UUID actorId, String email, String displayName, String rawPassword,
            Set<Role> roles) {
        String normalized = normalizeEmail(email);
        if (users.existsByEmail(normalized)) {
            throw new ConflictException("A user with this email already exists");
        }
        UserAccount user = UserAccount.create(normalized, displayName.trim(),
                passwordEncoder.encode(rawPassword), roles, clock.instant());
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("A user with this email already exists");
        }
        audit.record("USER_CREATED", actorId, user.getId(), "SUCCESS");
        return user;
    }

    @Transactional(readOnly = true)
    public UserAccount getUser(UUID id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public Page<UserAccount> listUsers(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE.contains(order.getProperty())) {
                throw new InvalidRequestException("Unsupported sort property: " + order.getProperty());
            }
        }
        return users.findAll(pageable);
    }

    public UserAccount changeStatus(UUID actorId, UUID id, AccountStatus status) {
        if (actorId.equals(id)) {
            throw new ConflictException("Administrators cannot change their own account status");
        }
        UserAccount user = getUser(id);
        user.changeStatus(status, clock.instant());
        audit.record("USER_STATUS_CHANGED", actorId, id, status.name());
        return user;
    }

    public UserAccount replaceRoles(UUID actorId, UUID id, Set<Role> roles) {
        if (actorId.equals(id) && !roles.contains(Role.SYSTEM_ADMIN)) {
            throw new ConflictException("Administrators cannot remove their own administrator role");
        }
        UserAccount user = getUser(id);
        user.replaceRoles(roles, clock.instant());
        audit.record("USER_ROLES_CHANGED", actorId, id, "SUCCESS");
        return user;
    }
}

package com.facility.identity.application;

import java.time.Clock;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.facility.identity.domain.UserAccount;
import com.facility.identity.domain.UserAccountRepository;
import com.facility.identity.security.AccessToken;
import com.facility.identity.security.AccessTokenIssuer;

@Service
public class AuthenticationService {
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenIssuer tokenIssuer;
    private final AuditService audit;
    private final Clock clock;
    private final String decoyHash;

    public AuthenticationService(UserAccountRepository users, PasswordEncoder passwordEncoder,
            AccessTokenIssuer tokenIssuer, AuditService audit, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokenIssuer = tokenIssuer;
        this.audit = audit;
        this.clock = clock;
        this.decoyHash = passwordEncoder.encode("decoy-value-for-constant-time-checks");
    }

    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public AccessToken login(String email, String rawPassword) {
        Optional<UserAccount> found = users.findByEmail(UserAdministrationService.normalizeEmail(email));
        // Always run one password comparison so unknown accounts are not distinguishable by timing.
        boolean matches = passwordEncoder.matches(rawPassword,
                found.map(UserAccount::getPasswordHash).orElse(decoyHash));
        if (found.isEmpty() || !matches || !found.get().isActive()) {
            audit.record("LOGIN", null, found.map(UserAccount::getId).orElse(null), "FAILURE");
            throw new AuthenticationFailedException();
        }
        UserAccount user = found.get();
        user.recordLogin(clock.instant());
        audit.record("LOGIN", user.getId(), user.getId(), "SUCCESS");
        return tokenIssuer.issue(user);
    }
}

package com.facility.identity.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.facility.identity.domain.AccountStatus;
import com.facility.identity.domain.Role;
import com.facility.identity.domain.UserAccount;
import com.facility.identity.domain.UserAccountRepository;

class UserAdministrationServiceTest {
    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final AuditService audit = mock(AuditService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final UserAdministrationService service = new UserAdministrationService(users, encoder, audit, clock);

    @Test
    void rejectsDuplicateEmailCaseInsensitively() {
        when(users.existsByEmail("a@example.test")).thenReturn(true);
        assertThatThrownBy(() -> service.createUser(null, " A@Example.test ", "A", "long-enough-password", Set.of(Role.TENANT)))
                .isInstanceOf(ConflictException.class);
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void hashesPasswordBeforeSaving() {
        when(encoder.encode("long-enough-password")).thenReturn("hashed");
        UserAccount created = service.createUser(null, "a@example.test", "A", "long-enough-password", Set.of(Role.TENANT));
        org.assertj.core.api.Assertions.assertThat(created.getPasswordHash()).isEqualTo("hashed");
    }

    @Test
    void administratorCannotChangeOwnStatusOrDropOwnAdminRole() {
        UUID self = UUID.randomUUID();
        assertThatThrownBy(() -> service.changeStatus(self, self, AccountStatus.DISABLED))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.replaceRoles(self, self, Set.of(Role.TENANT)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void unknownUserIsNotFound() {
        UUID id = UUID.randomUUID();
        when(users.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getUser(id)).isInstanceOf(NotFoundException.class);
    }
}

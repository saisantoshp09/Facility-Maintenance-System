package com.facility.identity.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Optional first-administrator bootstrap; both values come from the environment, never from source. */
@ConfigurationProperties("identity.bootstrap-admin")
public record BootstrapAdminProperties(String email, String password) {
    public boolean isConfigured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }
}

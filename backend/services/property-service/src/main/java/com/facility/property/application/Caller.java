package com.facility.property.application;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

/** Authenticated caller resolved from the access token. */
public record Caller(UUID userId, boolean systemAdmin) {
    public static Caller from(Jwt jwt) {
        UUID userId;
        try {
            userId = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new InvalidRequestException("Token subject is not a valid user id");
        }
        var roles = jwt.getClaimAsStringList("roles");
        return new Caller(userId, roles != null && roles.contains("SYSTEM_ADMIN"));
    }
}

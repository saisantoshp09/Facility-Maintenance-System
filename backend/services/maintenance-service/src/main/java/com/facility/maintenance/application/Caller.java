package com.facility.maintenance.application;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

/** Authenticated caller; carries the bearer token so it can be forwarded to the property service. */
public record Caller(UUID userId, String bearerToken) {
    public static Caller from(Jwt jwt) {
        try {
            return new Caller(UUID.fromString(jwt.getSubject()), jwt.getTokenValue());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new InvalidRequestException("Token subject is not a valid user id");
        }
    }
}

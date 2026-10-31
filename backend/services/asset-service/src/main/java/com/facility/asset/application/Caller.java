package com.facility.asset.application;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public record Caller(UUID userId, String bearerToken) {
    public static Caller from(Jwt jwt) {
        try {
            return new Caller(UUID.fromString(jwt.getSubject()), jwt.getTokenValue());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new InvalidRequestException("Token subject is not a valid user id");
        }
    }
}

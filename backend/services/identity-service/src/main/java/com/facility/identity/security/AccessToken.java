package com.facility.identity.security;

public record AccessToken(String value, long expiresInSeconds) {
}

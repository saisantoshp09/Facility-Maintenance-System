package com.facility.identity.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.core.io.Resource;

/**
 * @param privateKeyLocation optional PKCS#8 PEM RSA private key; when absent an ephemeral key is generated
 *                           at startup (local development only: tokens become invalid on restart)
 */
@ConfigurationProperties("identity.jwt")
public record JwtProperties(
        @DefaultValue("http://localhost:8082") String issuer,
        @DefaultValue("facility-api") String audience,
        @DefaultValue("15m") Duration accessTokenTtl,
        Resource privateKeyLocation,
        @DefaultValue("10") int bcryptStrength) {
}

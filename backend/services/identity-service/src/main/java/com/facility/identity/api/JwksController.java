package com.facility.identity.api;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

@RestController
public class JwksController {
    private final JWKSet publicKeys;

    public JwksController(RSAKey signingKey) {
        this.publicKeys = new JWKSet(signingKey.toPublicJWK());
    }

    @GetMapping("/oauth2/jwks")
    public Map<String, Object> jwks() {
        return publicKeys.toJSONObject(true);
    }
}

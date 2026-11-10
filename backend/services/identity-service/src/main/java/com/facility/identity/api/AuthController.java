package com.facility.identity.api;

import java.net.URI;
import java.util.Set;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.facility.identity.api.dto.LoginRequest;
import com.facility.identity.api.dto.RegisterRequest;
import com.facility.identity.api.dto.TokenResponse;
import com.facility.identity.api.dto.UserResponse;
import com.facility.identity.application.AuthenticationService;
import com.facility.identity.application.UserAdministrationService;
import com.facility.identity.domain.Role;
import com.facility.identity.domain.UserAccount;
import com.facility.identity.security.AccessToken;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthenticationService authentication;
    private final UserAdministrationService users;

    public AuthController(AuthenticationService authentication, UserAdministrationService users) {
        this.authentication = authentication;
        this.users = users;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        AccessToken token = authentication.login(request.email(), request.password());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new TokenResponse(token.value(), "Bearer", token.expiresInSeconds()));
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserAccount user = users.createUser(null, request.email(), request.displayName(),
                request.password(), Set.of(Role.TENANT));
        return ResponseEntity.created(URI.create("/api/v1/users/" + user.getId()))
                .cacheControl(CacheControl.noStore())
                .body(UserResponse.from(user));
    }
}

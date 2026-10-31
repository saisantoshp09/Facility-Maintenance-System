package com.facility.notification.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Guards /internal/** with a shared secret. Fails closed when no key is configured. */
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {
    public static final String HEADER_NAME = "X-Internal-Api-Key";

    private final byte[] expected;
    private final ApiErrorWriter errorWriter;

    public InternalApiKeyFilter(@Value("${app.internal.api-key:}") String apiKey, ApiErrorWriter errorWriter) {
        this.expected = apiKey == null || apiKey.isBlank() ? null : apiKey.getBytes(StandardCharsets.UTF_8);
        this.errorWriter = errorWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String provided = request.getHeader(HEADER_NAME);
        if (expected == null || provided == null
                || !MessageDigest.isEqual(expected, provided.getBytes(StandardCharsets.UTF_8))) {
            errorWriter.commence(request, response, new BadCredentialsException("Invalid internal API key"));
            return;
        }
        chain.doFilter(request, response);
    }
}

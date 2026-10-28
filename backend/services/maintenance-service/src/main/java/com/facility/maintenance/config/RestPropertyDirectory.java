package com.facility.maintenance.config;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.facility.maintenance.application.Caller;
import com.facility.maintenance.application.DependencyUnavailableException;
import com.facility.maintenance.application.NotFoundException;
import com.facility.maintenance.application.PropertyAccessView;
import com.facility.maintenance.application.PropertyDirectory;

/**
 * REST client for the property service. Only idempotent GET requests are issued, with explicit
 * timeouts and a bounded number of attempts. Any failure other than an authoritative 403/404 fails closed.
 */
@Component
public class RestPropertyDirectory implements PropertyDirectory {
    private final RestClient client;
    private final int maxAttempts;

    public RestPropertyDirectory(@Value("${app.property-service.base-url}") String baseUrl,
            @Value("${app.property-service.connect-timeout}") Duration connectTimeout,
            @Value("${app.property-service.read-timeout}") Duration readTimeout,
            @Value("${app.property-service.max-attempts}") int maxAttempts) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(connectTimeout).build());
        factory.setReadTimeout(readTimeout);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    @Override
    public PropertyAccessView accessOf(Caller caller, UUID propertyId) {
        return fetch(caller, "/api/v1/properties/{p}/access", propertyId);
    }

    @Override
    public PropertyAccessView memberAccess(Caller caller, UUID propertyId, UUID userId) {
        return fetch(caller, "/api/v1/properties/{p}/members/{u}/access", propertyId, userId);
    }

    private PropertyAccessView fetch(Caller caller, String path, Object... vars) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                PropertyAccessView view = client.get().uri(path, vars)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + caller.bearerToken())
                        .retrieve().body(PropertyAccessView.class);
                if (view == null) {
                    throw new DependencyUnavailableException("Empty response from property service", null);
                }
                return view;
            } catch (HttpClientErrorException ex) {
                int status = ex.getStatusCode().value();
                if (status == 404) {
                    throw new NotFoundException("Property not found");
                }
                if (status == 403) {
                    throw new AccessDeniedException("Property management rights are required");
                }
                throw new DependencyUnavailableException("Unexpected response " + status + " from property service", ex);
            } catch (ResourceAccessException | HttpServerErrorException ex) {
                last = new DependencyUnavailableException("Property service call failed", ex);
            }
        }
        throw last;
    }
}

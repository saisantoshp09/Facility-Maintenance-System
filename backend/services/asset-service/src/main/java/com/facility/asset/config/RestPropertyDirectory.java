package com.facility.asset.config;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import org.springframework.web.client.RestClient;
import com.facility.asset.application.Caller;
import com.facility.asset.application.DependencyUnavailableException;
import com.facility.asset.application.NotFoundException;
import com.facility.asset.application.PropertyAccessView;
import com.facility.asset.application.PropertyDirectory;

@Component
public class RestPropertyDirectory implements PropertyDirectory {
    private final RestClient client; private final int maxAttempts;
    public RestPropertyDirectory(@Value("${app.property-service.base-url}") String baseUrl,
            @Value("${app.property-service.connect-timeout}") Duration connectTimeout,
            @Value("${app.property-service.read-timeout}") Duration readTimeout,
            @Value("${app.property-service.max-attempts}") int attempts) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(connectTimeout).build());
        factory.setReadTimeout(readTimeout); client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        maxAttempts = Math.max(1, attempts);
    }
    @Override public PropertyAccessView accessOf(Caller caller, UUID propertyId) {
        RuntimeException last = null;
        for (int i = 0; i < maxAttempts; i++) try {
            PropertyAccessView view = client.get().uri("/api/v1/properties/{p}/access", propertyId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + caller.bearerToken()).retrieve().body(PropertyAccessView.class);
            if (view == null) throw new DependencyUnavailableException("Empty response from property service", null);
            return view;
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 404) throw new NotFoundException("Property not found");
            if (ex.getStatusCode().value() == 403) throw new AccessDeniedException("Property access is denied");
            throw new DependencyUnavailableException("Unexpected response from property service", ex);
        } catch (RestClientException ex) { last = new DependencyUnavailableException("Property service call failed", ex); }
        throw last;
    }
}

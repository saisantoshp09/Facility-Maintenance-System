package com.facility.maintenance.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.facility.maintenance.application.NotificationPublisher;

@Component
public class RestNotificationPublisher implements NotificationPublisher {
    private final HttpClient client;
    private final URI endpoint;
    private final String apiKey;

    public RestNotificationPublisher(@Value("${app.notification-service.base-url:http://localhost:8086}") String baseUrl,
            @Value("${app.notification-service.api-key:}") String apiKey) {
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        this.endpoint = URI.create(baseUrl.replaceAll("/$", "") + "/internal/v1/events");
        this.apiKey = apiKey;
    }

    @Override
    public void publish(String payload) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(3))
                .header("Content-Type", "application/json")
                .header("X-Internal-Api-Key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload)).build();
        HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Notification service returned HTTP " + response.statusCode());
        }
    }
}

package com.facility.gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;

class GatewayErrorHandlerTest {
    private final GatewayErrorHandler handler = new GatewayErrorHandler(new ObjectMapper());

    private MockServerWebExchange run(Throwable ex) {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/properties"));
        handler.handle(exchange, ex).block();
        return exchange;
    }

    @Test
    void connectFailureBecomes503WithEnvelope() {
        MockServerWebExchange ex = run(new RuntimeException("wrapped", new ConnectException("refused")));
        assertEquals(503, ex.getResponse().getStatusCode().value());
        String body = ex.getResponse().getBodyAsString().block();
        assertTrue(body.contains("SERVICE_UNAVAILABLE"));
        assertTrue(!body.contains("refused"));
    }

    @Test
    void timeoutBecomes504() {
        assertEquals(504, run(new TimeoutException("slow")).getResponse().getStatusCode().value());
    }

    @Test
    void notFoundIsPreserved() {
        assertEquals(404, run(new ResponseStatusException(HttpStatus.NOT_FOUND)).getResponse().getStatusCode().value());
    }

    @Test
    void unknownErrorBecomes500WithoutDetails() {
        MockServerWebExchange ex = run(new IllegalStateException("secret detail"));
        assertEquals(500, ex.getResponse().getStatusCode().value());
        assertTrue(!ex.getResponse().getBodyAsString().block().contains("secret detail"));
    }
}

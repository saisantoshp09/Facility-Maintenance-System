package com.facility.gateway.config;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

/** Maps gateway/downstream failures to the standard API error envelope without leaking internals. */
@Component
@Order(-2)
public class GatewayErrorHandler implements WebExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GatewayErrorHandler.class);
    private final ObjectMapper mapper;

    public GatewayErrorHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        HttpStatusCode status;
        String code;
        String message;
        if (hasCause(ex, TimeoutException.class)) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            code = "UPSTREAM_TIMEOUT";
            message = "The upstream service did not respond in time";
        } else if (hasCause(ex, ConnectException.class)
                || (ex instanceof ResponseStatusException rse && rse.getStatusCode().value() == 503)) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            code = "SERVICE_UNAVAILABLE";
            message = "The upstream service is unavailable";
        } else if (ex instanceof ResponseStatusException rse && rse.getStatusCode().is4xxClientError()) {
            status = rse.getStatusCode();
            code = status.value() == 404 ? "NOT_FOUND" : "BAD_REQUEST";
            message = status.value() == 404 ? "Resource not found" : "Request could not be processed";
        } else {
            log.error("Unhandled gateway error for {} {}", exchange.getRequest().getMethod(),
                    exchange.getRequest().getPath(), ex);
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            code = "INTERNAL_ERROR";
            message = "Unexpected error";
        }
        if (status.is5xxServerError() && status != HttpStatus.INTERNAL_SERVER_ERROR) {
            log.warn("Upstream failure for {}: {}", exchange.getRequest().getPath(), ex.getClass().getSimpleName());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("code", code);
        body.put("message", message);
        body.put("path", exchange.getRequest().getPath().value());
        body.put("correlationId", exchange.getResponse().getHeaders().getFirst("X-Correlation-ID"));
        body.put("fieldErrors", List.of());

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] bytes = mapper.writeValueAsBytes(body);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception writeFailure) {
            byte[] fallback = "{\"status\":500,\"code\":\"INTERNAL_ERROR\"}".getBytes(StandardCharsets.UTF_8);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(fallback)));
        }
    }

    private static boolean hasCause(Throwable ex, Class<? extends Throwable> type) {
        for (Throwable t = ex; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (type.isInstance(t)) {
                return true;
            }
        }
        return false;
    }
}

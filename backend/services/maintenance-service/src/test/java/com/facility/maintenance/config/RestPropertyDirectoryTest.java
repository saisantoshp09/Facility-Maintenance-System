package com.facility.maintenance.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.facility.maintenance.application.Caller;
import com.facility.maintenance.application.DependencyUnavailableException;
import com.facility.maintenance.application.NotFoundException;
import com.facility.maintenance.application.PropertyAccessView;
import com.sun.net.httpserver.HttpServer;

class RestPropertyDirectoryTest {
    HttpServer server;
    final AtomicInteger calls = new AtomicInteger();
    final Caller caller = new Caller(UUID.randomUUID(), "token");
    final UUID property = UUID.randomUUID();

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    RestPropertyDirectory start(int status, String body) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            calls.incrementAndGet();
            byte[] bytes = body.getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return new RestPropertyDirectory("http://127.0.0.1:" + server.getAddress().getPort(),
                Duration.ofSeconds(1), Duration.ofSeconds(1), 2);
    }

    @Test
    void parsesAGrantedAnswer() throws Exception {
        RestPropertyDirectory directory = start(200, "{\"propertyId\":\"" + property
                + "\",\"systemAdmin\":false,\"roles\":[\"TECHNICIAN\"],\"unitIds\":[]}");
        PropertyAccessView view = directory.accessOf(caller, property);
        assertTrue(view.isTechnician());
        assertEquals(1, calls.get());
    }

    @Test
    void mapsNotFoundWithoutRetrying() throws Exception {
        RestPropertyDirectory directory = start(404, "{}");
        assertThrows(NotFoundException.class, () -> directory.accessOf(caller, property));
        assertEquals(1, calls.get());
    }

    @Test
    void mapsForbiddenToAccessDenied() throws Exception {
        RestPropertyDirectory directory = start(403, "{}");
        assertThrows(AccessDeniedException.class,
                () -> directory.memberAccess(caller, property, UUID.randomUUID()));
    }

    @Test
    void retriesServerErrorsBoundedlyThenFailsClosed() throws Exception {
        RestPropertyDirectory directory = start(500, "{}");
        assertThrows(DependencyUnavailableException.class, () -> directory.accessOf(caller, property));
        assertEquals(2, calls.get());
    }

    @Test
    void failsClosedWhenUnreachable() {
        RestPropertyDirectory directory = new RestPropertyDirectory("http://127.0.0.1:1",
                Duration.ofMillis(500), Duration.ofMillis(500), 2);
        assertThrows(DependencyUnavailableException.class, () -> directory.accessOf(caller, property));
    }
}

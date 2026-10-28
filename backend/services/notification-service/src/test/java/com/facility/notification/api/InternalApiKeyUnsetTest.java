package com.facility.notification.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.internal.api-key=")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InternalApiKeyUnsetTest {
    @Autowired MockMvc mvc;

    @Test
    void failsClosedWhenKeyNotConfigured() throws Exception {
        String body = "{\"eventId\":\"" + UUID.randomUUID() + "\"}";
        mvc.perform(post("/internal/v1/events").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/internal/v1/events").header("X-Internal-Api-Key", "")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
        mvc.perform(post("/internal/v1/events").header("X-Internal-Api-Key", "anything")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
    }
}

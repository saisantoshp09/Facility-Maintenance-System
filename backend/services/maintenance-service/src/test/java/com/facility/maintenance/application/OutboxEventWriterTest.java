package com.facility.maintenance.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.facility.maintenance.domain.OutboxEvent;
import com.facility.maintenance.domain.OutboxEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class OutboxEventWriterTest {
    private final OutboxEventRepository repository = Mockito.mock(OutboxEventRepository.class);
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final OutboxEventWriter writer = new OutboxEventWriter(repository, mapper,
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void recipientsAreDeduplicatedAndActorExcluded() throws Exception {
        UUID actor = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        writer.write("WORK_ORDER_COMMENT_ADDED", actor, recipient, recipient, UUID.randomUUID(),
                "Work order updated", "A comment was added to a work order");
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        JsonNode payload = mapper.readTree(captor.getValue().getPayload());
        assertEquals(1, payload.get("recipientUserIds").size());
        assertEquals(recipient.toString(), payload.get("recipientUserIds").get(0).asText());
        assertFalse(payload.toString().contains(actor.toString()));
        assertEquals(captor.getValue().getId().toString(), payload.get("eventId").asText());
    }

    @Test
    void noEventIsWrittenWhenAllRecipientsAreActor() {
        UUID actor = UUID.randomUUID();
        writer.write("WORK_ORDER_CREATED", actor, actor, null, UUID.randomUUID(),
                "Work order created", "A work order was created");
        verifyNoInteractions(repository);
    }
}

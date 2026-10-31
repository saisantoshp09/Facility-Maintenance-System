package com.facility.identity.application;

import java.time.Clock;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.facility.identity.domain.AuditEvent;
import com.facility.identity.domain.AuditEventRepository;

@Component
public class AuditService {
    private final AuditEventRepository repository;
    private final Clock clock;

    public AuditService(AuditEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void record(String eventType, UUID actorId, UUID subjectId, String outcome) {
        repository.save(new AuditEvent(eventType, actorId, subjectId, outcome,
                MDC.get("correlationId"), clock.instant()));
    }
}

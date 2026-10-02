package com.dismal.distribuciones.modules.integrations.outbox.service;

import com.dismal.distribuciones.exception.ResourceNotFoundException;
import com.dismal.distribuciones.modules.integrations.outbox.domain.EventOutbox;
import com.dismal.distribuciones.modules.integrations.outbox.domain.EventOutboxStatus;
import com.dismal.distribuciones.modules.integrations.outbox.dto.OutboxResponse;
import com.dismal.distribuciones.modules.integrations.outbox.repository.EventOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxAdminService {

    private final EventOutboxRepository eventOutboxRepository;

    @Transactional(readOnly = true)
    public Page<OutboxResponse> list(EventOutboxStatus status, String eventType, UUID aggregateId, Pageable pageable) {
        Specification<EventOutbox> spec = Specification.where(null);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (eventType != null && !eventType.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("eventType"), eventType));
        }
        if (aggregateId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("aggregateId"), aggregateId));
        }
        return eventOutboxRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OutboxResponse get(UUID id) {
        EventOutbox outbox = eventOutboxRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Outbox event not found with ID: " + id));
        return toResponse(outbox);
    }

    @Transactional
    public OutboxResponse retryOne(UUID id) {
        EventOutbox outbox = eventOutboxRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Outbox event not found with ID: " + id));
        outbox.setStatus(EventOutboxStatus.PENDING);
        outbox.setNextAttemptAt(Instant.now());
        return toResponse(eventOutboxRepository.save(outbox));
    }

    @Transactional
    public int retryFailed(String eventType) {
        Specification<EventOutbox> spec = Specification.where(
                (root, query, cb) -> cb.equal(root.get("status"), EventOutboxStatus.FAILED)
        );
        if (eventType != null && !eventType.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("eventType"), eventType));
        }
        var events = eventOutboxRepository.findAll(spec);
        Instant now = Instant.now();
        for (EventOutbox event : events) {
            event.setNextAttemptAt(now);
            event.setStatus(EventOutboxStatus.PENDING);
        }
        eventOutboxRepository.saveAll(events);
        return events.size();
    }

    private OutboxResponse toResponse(EventOutbox event) {
        return new OutboxResponse(
                event.getId(),
                event.getEventType(),
                event.getAggregateId(),
                event.getPayloadJson(),
                event.getStatus(),
                event.getAttempts(),
                event.getNextAttemptAt(),
                event.getSentAt(),
                event.getLastError(),
                event.getLastResponseCode(),
                event.getLastResponseBody(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}

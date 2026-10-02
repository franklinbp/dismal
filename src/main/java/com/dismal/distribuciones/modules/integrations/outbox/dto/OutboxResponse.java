package com.dismal.distribuciones.modules.integrations.outbox.dto;

import com.dismal.distribuciones.modules.integrations.outbox.domain.EventOutboxStatus;

import java.time.Instant;
import java.util.UUID;

public record OutboxResponse(
        UUID id,
        String eventType,
        UUID aggregateId,
        String payloadJson,
        EventOutboxStatus status,
        Integer attempts,
        Instant nextAttemptAt,
        Instant sentAt,
        String lastError,
        Integer lastResponseCode,
        String lastResponseBody,
        Instant createdAt,
        Instant updatedAt
) {}

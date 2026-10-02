package com.dismal.distribuciones.modules.dashboard.dto;

import com.dismal.distribuciones.modules.integrations.outbox.domain.EventOutboxStatus;

import java.time.Instant;
import java.util.UUID;

public record OutboxDto(
        UUID id,
        String eventType,
        UUID aggregateId,
        EventOutboxStatus status,
        Integer attempts,
        String lastError,
        Instant createdAt,
        Instant nextAttemptAt
) {}

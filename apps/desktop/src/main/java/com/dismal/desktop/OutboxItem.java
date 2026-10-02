package com.dismal.desktop;

import java.time.Instant;

public record OutboxItem(
        String id,
        String eventType,
        String aggregateId,
        String payloadJson,
        String status,
        Integer attempts,
        Instant nextAttemptAt,
        Instant sentAt,
        String lastError,
        Integer lastResponseCode,
        String lastResponseBody,
        Instant createdAt,
        Instant updatedAt
) {}

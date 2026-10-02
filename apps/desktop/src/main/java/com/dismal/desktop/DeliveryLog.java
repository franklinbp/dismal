package com.dismal.desktop;

import java.time.Instant;

public record DeliveryLog(
        String id,
        String outboxEventId,
        String channel,
        String status,
        String providerMessageId,
        String error,
        Instant createdAt,
        Instant updatedAt
) {}

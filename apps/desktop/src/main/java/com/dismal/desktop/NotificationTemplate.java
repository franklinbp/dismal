package com.dismal.desktop;

import java.time.Instant;

public record NotificationTemplate(
        String id,
        String eventType,
        String channel,
        String subject,
        String body,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {}

package com.dismal.distribuciones.modules.integrations.notifications.dto;

import com.dismal.distribuciones.modules.integrations.notifications.domain.NotificationChannel;

import java.time.Instant;
import java.util.UUID;

public record NotificationTemplateResponse(
        UUID id,
        String eventType,
        NotificationChannel channel,
        String subject,
        String body,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {}

package com.dismal.distribuciones.modules.integrations.notifications.dto;

import com.dismal.distribuciones.modules.integrations.notifications.domain.NotificationChannel;
import com.dismal.distribuciones.modules.integrations.notifications.domain.NotificationDeliveryStatus;

import java.time.Instant;
import java.util.UUID;

public record NotificationDeliveryLogResponse(
        UUID id,
        UUID outboxEventId,
        NotificationChannel channel,
        NotificationDeliveryStatus status,
        String providerMessageId,
        String error,
        Instant createdAt,
        Instant updatedAt
) {}

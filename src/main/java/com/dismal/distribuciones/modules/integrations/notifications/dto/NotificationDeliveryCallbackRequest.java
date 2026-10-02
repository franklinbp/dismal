package com.dismal.distribuciones.modules.integrations.notifications.dto;

import com.dismal.distribuciones.modules.integrations.notifications.domain.NotificationChannel;
import com.dismal.distribuciones.modules.integrations.notifications.domain.NotificationDeliveryStatus;

import java.util.UUID;

public record NotificationDeliveryCallbackRequest(
        UUID outboxEventId,
        NotificationChannel channel,
        NotificationDeliveryStatus status,
        String providerMessageId,
        String error
) {}

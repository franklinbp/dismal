package com.dismal.distribuciones.modules.integrations.notifications.dto;

import com.dismal.distribuciones.modules.integrations.notifications.domain.NotificationChannel;

public record NotificationTemplateRequest(
        String eventType,
        NotificationChannel channel,
        String subject,
        String body,
        Boolean enabled
) {}

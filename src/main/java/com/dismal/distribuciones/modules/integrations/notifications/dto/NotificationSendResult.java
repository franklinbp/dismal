package com.dismal.distribuciones.modules.integrations.notifications.dto;

public record NotificationSendResult(
        boolean sent,
        String message
) {}

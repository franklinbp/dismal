package com.dismal.distribuciones.modules.sales.dto;

import com.dismal.distribuciones.modules.integrations.notifications.domain.NotificationChannel;

import java.util.List;

public record ConfirmSaleRequest(
        List<NotificationChannel> deliveryChannels
) {}

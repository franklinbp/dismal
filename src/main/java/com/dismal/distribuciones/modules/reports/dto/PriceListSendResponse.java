package com.dismal.distribuciones.modules.reports.dto;

import java.time.LocalDateTime;

public record PriceListSendResponse(
        LocalDateTime sentAt,
        String whatsappText,
        String whatsappUrl,
        boolean emailSent,
        boolean whatsappSent,
        String emailMessage,
        String whatsappMessage
) {}

package com.dismal.distribuciones.modules.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AccountsReceivableReminderResponse(
        UUID outboxEventId,
        UUID accountsReceivableId,
        UUID clientId,
        LocalDate dueDate,
        BigDecimal balance,
        long daysOverdue,
        String status,
        String message,
        boolean emailSent,
        boolean whatsappSent,
        String emailMessage,
        String whatsappMessage
) {}

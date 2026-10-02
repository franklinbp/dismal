package com.dismal.distribuciones.modules.sales.dto;

import com.dismal.distribuciones.modules.sales.domain.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceSummaryResponse(
        UUID id,
        String invoiceNumber,
        InvoiceStatus status,
        LocalDate issueDate,
        LocalDate dueDate,
        BigDecimal totalAmount,
        UUID saleId,
        String clientName,
        String clientEmail
) {}

package com.dismal.desktop;

public record InvoiceSummary(
        String id,
        String invoiceNumber,
        String status,
        String issueDate,
        String dueDate,
        double totalAmount,
        String saleId,
        String clientName,
        String clientEmail
) {}

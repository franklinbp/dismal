package com.dismal.distribuciones.modules.sales.domain;

public enum InvoiceStatus {
    PENDING, // The invoice has been created but not yet paid.
    PAID,    // The invoice has been paid in full.
    OVERDUE, // The invoice has passed its due date without being paid.
    CANCELLED // The invoice has been voided.
}

package com.dismal.distribuciones.modules.sales.dto;

public record AccountsReceivablePaymentResponse(
        boolean success,
        String message,
        AccountsReceivableClientSummaryResponse summary
) {
}

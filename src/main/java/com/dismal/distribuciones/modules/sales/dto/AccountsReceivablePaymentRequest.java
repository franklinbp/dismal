package com.dismal.distribuciones.modules.sales.dto;

import com.dismal.distribuciones.modules.sales.domain.AccountsReceivableStatus;
import com.dismal.distribuciones.modules.sales.domain.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountsReceivablePaymentRequest(
        BigDecimal amount,
        PaymentMethod method,
        String reference,
        UUID paymentAccountId,
        AccountsReceivableStatus status,
        Boolean notifyClient
) {
    public boolean shouldNotifyClient() {
        return notifyClient == null || notifyClient;
    }
}

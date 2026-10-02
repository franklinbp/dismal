package com.dismal.distribuciones.modules.sales.dto;

import com.dismal.distribuciones.modules.sales.domain.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(
        UUID saleId,
        BigDecimal amount,
        PaymentMethod method,
        String reference,
        UUID paymentAccountId,
        Boolean notifyClient
) {
    public PaymentRequest(UUID saleId, BigDecimal amount, PaymentMethod method, String reference) {
        this(saleId, amount, method, reference, null, true);
    }

    public PaymentRequest(UUID saleId, BigDecimal amount, PaymentMethod method, String reference, UUID paymentAccountId) {
        this(saleId, amount, method, reference, paymentAccountId, true);
    }

    public boolean shouldNotifyClient() {
        return notifyClient == null || notifyClient;
    }
}

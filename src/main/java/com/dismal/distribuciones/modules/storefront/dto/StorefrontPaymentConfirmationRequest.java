package com.dismal.distribuciones.modules.storefront.dto;

import com.dismal.distribuciones.modules.sales.domain.PaymentMethod;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.UUID;

public record StorefrontPaymentConfirmationRequest(
        @NotBlank String provider,
        @NotBlank String reference,
        BigDecimal amount,
        PaymentMethod method,
        UUID paymentAccountId,
        Boolean notifyClient
) {
    public boolean shouldNotifyClient() {
        return notifyClient == null || notifyClient;
    }
}

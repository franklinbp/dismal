package com.dismal.distribuciones.modules.storefront.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record StorefrontPaymentReviewRequest(
        @NotNull Boolean approved,
        UUID paymentAccountId,
        @Size(max = 500) String reviewNotes,
        Boolean notifyClient
) {
    public boolean shouldNotifyClient() {
        return notifyClient == null || notifyClient;
    }
}

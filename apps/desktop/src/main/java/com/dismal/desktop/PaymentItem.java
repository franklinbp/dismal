package com.dismal.desktop;

import java.time.LocalDateTime;

public record PaymentItem(
        String id,
        String saleId,
        double amount,
        String method,
        String reference,
        LocalDateTime createdAt
) {}

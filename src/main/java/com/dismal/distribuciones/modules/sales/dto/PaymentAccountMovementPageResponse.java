package com.dismal.distribuciones.modules.sales.dto;

import java.util.List;

public record PaymentAccountMovementPageResponse(
        List<PaymentAccountMovementResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}

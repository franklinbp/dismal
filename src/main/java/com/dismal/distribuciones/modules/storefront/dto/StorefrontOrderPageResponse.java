package com.dismal.distribuciones.modules.storefront.dto;

import java.util.List;

public record StorefrontOrderPageResponse(
        List<StorefrontAccountOrderResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}

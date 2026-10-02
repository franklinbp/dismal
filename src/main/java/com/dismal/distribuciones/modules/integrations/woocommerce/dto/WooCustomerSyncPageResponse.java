package com.dismal.distribuciones.modules.integrations.woocommerce.dto;

import java.util.List;

public record WooCustomerSyncPageResponse(
        List<WooCustomerSyncItemResponse> content,
        long totalElements,
        int totalPages,
        int size,
        int number,
        boolean last
) {}

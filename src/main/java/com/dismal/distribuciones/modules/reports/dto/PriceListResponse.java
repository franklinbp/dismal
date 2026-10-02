package com.dismal.distribuciones.modules.reports.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PriceListResponse(
        LocalDateTime generatedAt,
        List<PriceListItemResponse> items,
        String text,
        String html
) {}

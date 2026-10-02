package com.dismal.desktop;

import java.util.List;

public record SalesPage(
        List<SaleRow> content,
        long totalElements,
        int totalPages,
        int size,
        int number
) {}

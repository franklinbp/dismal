package com.dismal.distribuciones.modules.sales.dto;

import java.util.List;
import java.util.UUID;

public record CreateQuoteRequest(
        UUID clientId,
        String notes,
        List<QuoteItemRequest> items
) {}

package com.dismal.distribuciones.modules.sales.dto;

import com.dismal.distribuciones.modules.sales.domain.SaleType;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;

import java.util.List;
import java.util.UUID;

public record CreateSaleRequest(
        UUID clientId,
        SaleType saleType,
        StorefrontCountry country,
        List<SaleItemRequest> items
) {
    public CreateSaleRequest(UUID clientId, SaleType saleType, List<SaleItemRequest> items) {
        this(clientId, saleType, null, items);
    }
}

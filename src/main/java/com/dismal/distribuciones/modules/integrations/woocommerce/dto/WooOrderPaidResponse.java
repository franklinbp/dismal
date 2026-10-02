package com.dismal.distribuciones.modules.integrations.woocommerce.dto;

import java.util.List;
import java.util.UUID;

public record WooOrderPaidResponse(
        String externalOrderId,
        UUID saleId,
        String status,
        boolean duplicated,
        boolean newCustomerCreated,
        String customerEmail,
        String generatedPassword,
        List<WooDeliveredLicenseResponse> licenses
) {}

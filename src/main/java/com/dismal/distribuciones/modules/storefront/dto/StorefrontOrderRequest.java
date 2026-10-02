package com.dismal.distribuciones.modules.storefront.dto;

import com.dismal.distribuciones.modules.storefront.domain.StorefrontCheckoutMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record StorefrontOrderRequest(
        String country,
        String customerType,
        StorefrontCheckoutMethod checkoutMethod,
        @Valid @NotNull StorefrontCustomerRequest customer,
        @Valid @NotNull StorefrontShippingRequest shipping,
        @Valid @NotEmpty List<StorefrontOrderItemRequest> items
) {
    public StorefrontOrderRequest(
            String country,
            String customerType,
            StorefrontCheckoutMethod checkoutMethod,
            StorefrontCustomerRequest customer,
            List<StorefrontOrderItemRequest> items
    ) {
        this(country, customerType, checkoutMethod, customer,
                new StorefrontShippingRequest(
                        customer.firstName() + " " + customer.lastName(),
                        "Direccion pendiente de completar",
                        null,
                        "Pendiente",
                        "Pendiente",
                        null,
                        null
                ), items);
    }
}

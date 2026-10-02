package com.dismal.distribuciones.modules.integrations.woocommerce.dto;

import com.dismal.distribuciones.modules.sales.domain.PaymentMethod;
import com.dismal.distribuciones.modules.sales.domain.SaleType;
import com.dismal.distribuciones.modules.security.domain.CustomerType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record WooOrderPaidRequest(
        @NotBlank String orderId,
        @NotBlank String orderNumber,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @Email @NotBlank String email,
        String phone,
        String taxId,
        CustomerType customerType,
        SaleType saleType,
        @NotNull PaymentMethod paymentMethod,
        String paymentReference,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal totalPaid,
        @Valid @NotEmpty List<Item> items
) {
    public record Item(
            @NotNull UUID softwareId,
            @NotNull Integer quantity,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal unitPrice
    ) {}
}

package com.dismal.distribuciones.modules.storefront.domain;

import com.dismal.distribuciones.exception.BadRequestException;

public enum StorefrontCountry {
    EC("USD"),
    PE("PEN");

    private final String currency;

    StorefrontCountry(String currency) {
        this.currency = currency;
    }

    public String currency() {
        return currency;
    }

    public static StorefrontCountry from(String value) {
        if (value == null || value.isBlank() || "EC".equalsIgnoreCase(value.trim())) {
            return EC;
        }
        throw new BadRequestException("Dismal opera exclusivamente en Ecuador.");
    }
}

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
        if (value == null || value.isBlank()) {
            return EC;
        }
        try {
            return StorefrontCountry.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Pais no soportado: " + value);
        }
    }
}

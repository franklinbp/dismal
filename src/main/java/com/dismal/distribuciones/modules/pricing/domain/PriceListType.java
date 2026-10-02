package com.dismal.distribuciones.modules.pricing.domain;

import com.dismal.distribuciones.modules.security.domain.CustomerType;

public enum PriceListType {
    EC_FINAL("EC", CustomerType.FINAL, "Ecuador - Cliente final"),
    EC_DISTRIBUTOR("EC", CustomerType.DISTRIBUTOR, "Ecuador - Distribuidor"),
    PE_FINAL("PE", CustomerType.FINAL, "Peru - Cliente final"),
    PE_DISTRIBUTOR("PE", CustomerType.DISTRIBUTOR, "Peru - Distribuidor");

    private final String countryCode;
    private final CustomerType customerType;
    private final String displayName;

    PriceListType(String countryCode, CustomerType customerType, String displayName) {
        this.countryCode = countryCode;
        this.customerType = customerType;
        this.displayName = displayName;
    }

    public String countryCode() {
        return countryCode;
    }

    public CustomerType customerType() {
        return customerType;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isDefaultBasePrice() {
        return this == EC_FINAL;
    }

    public static PriceListType from(String countryCode, CustomerType customerType) {
        String normalizedCountry = countryCode == null || countryCode.isBlank()
                ? "EC"
                : countryCode.trim().toUpperCase();
        CustomerType normalizedType = customerType == null ? CustomerType.FINAL : customerType;

        return switch (normalizedCountry) {
            case "PE" -> normalizedType == CustomerType.DISTRIBUTOR ? PE_DISTRIBUTOR : PE_FINAL;
            case "EC" -> normalizedType == CustomerType.DISTRIBUTOR ? EC_DISTRIBUTOR : EC_FINAL;
            default -> throw new IllegalArgumentException("Unsupported country code: " + countryCode);
        };
    }
}

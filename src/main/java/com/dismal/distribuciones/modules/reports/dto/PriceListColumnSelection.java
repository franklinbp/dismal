package com.dismal.distribuciones.modules.reports.dto;

public record PriceListColumnSelection(
        boolean includeEcFinalPrice,
        boolean includeEcDistributorPrice,
        boolean includePeFinalPrice,
        boolean includePeDistributorPrice,
        boolean includeStock
) {

    public static PriceListColumnSelection all() {
        return new PriceListColumnSelection(true, true, true, true, true);
    }

    public static PriceListColumnSelection fromNullable(
            Boolean includeEcFinalPrice,
            Boolean includeEcDistributorPrice,
            Boolean includePeFinalPrice,
            Boolean includePeDistributorPrice,
            Boolean includeStock
    ) {
        if (includeEcFinalPrice == null
                && includeEcDistributorPrice == null
                && includePeFinalPrice == null
                && includePeDistributorPrice == null
                && includeStock == null) {
            return all();
        }

        return new PriceListColumnSelection(
                Boolean.TRUE.equals(includeEcFinalPrice),
                Boolean.TRUE.equals(includeEcDistributorPrice),
                Boolean.TRUE.equals(includePeFinalPrice),
                Boolean.TRUE.equals(includePeDistributorPrice),
                Boolean.TRUE.equals(includeStock)
        );
    }

    public boolean hasAnyPrice() {
        return includeEcFinalPrice || includeEcDistributorPrice || includePeFinalPrice || includePeDistributorPrice;
    }
}

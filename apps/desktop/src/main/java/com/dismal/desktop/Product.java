package com.dismal.desktop;

public record Product(
        String id,
        String name,
        String description,
        double price,
        String platform,
        String imageUrl,
        Double ecFinalPrice,
        Double ecDistributorPrice,
        Double peFinalPrice,
        Double peDistributorPrice
) {
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getPrice() {
        return price;
    }

    public String getPlatform() {
        return platform;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Double getEcFinalPrice() {
        return ecFinalPrice != null ? ecFinalPrice : price;
    }

    public Double getEcDistributorPrice() {
        return ecDistributorPrice != null ? ecDistributorPrice : getEcFinalPrice();
    }

    public Double getPeFinalPrice() {
        return peFinalPrice != null ? peFinalPrice : getEcFinalPrice();
    }

    public Double getPeDistributorPrice() {
        return peDistributorPrice != null ? peDistributorPrice : getPeFinalPrice();
    }
}

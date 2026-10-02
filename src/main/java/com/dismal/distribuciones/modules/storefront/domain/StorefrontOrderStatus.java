package com.dismal.distribuciones.modules.storefront.domain;

public enum StorefrontOrderStatus {
    PENDING_PAYMENT,
    PAYMENT_REVIEW,
    PAID,
    PREPARING,
    READY_FOR_DISPATCH,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    FAILED,
    REFUNDED
}

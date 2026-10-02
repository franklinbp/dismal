package com.dismal.distribuciones.modules.storefront.domain;

import com.dismal.distribuciones.modules.security.domain.CustomerType;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.customer.domain.CustomerMarket;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "storefront_orders",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_storefront_orders_order_number", columnNames = "order_number"),
                @UniqueConstraint(
                        name = "uk_storefront_orders_payment_reference",
                        columnNames = {"payment_provider", "payment_reference"}
                )
        }
)
public class StorefrontOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_number", nullable = false, length = 40)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StorefrontCountry country;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private CustomerType customerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StorefrontOrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "checkout_method", nullable = false, length = 32)
    private StorefrontCheckoutMethod checkoutMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_market_id")
    private CustomerMarket customerMarket;

    @Column(nullable = false, length = 180)
    private String email;

    @Column(length = 80)
    private String firstName;

    @Column(length = 80)
    private String lastName;

    @Column(length = 32)
    private String phone;

    @Column(length = 40)
    private String taxId;

    @Column(name = "shipping_recipient", length = 180)
    private String shippingRecipient;

    @Column(name = "shipping_address_line1", length = 220)
    private String shippingAddressLine1;

    @Column(name = "shipping_address_line2", length = 220)
    private String shippingAddressLine2;

    @Column(name = "shipping_city", length = 120)
    private String shippingCity;

    @Column(name = "shipping_region", length = 120)
    private String shippingRegion;

    @Column(name = "shipping_postal_code", length = 32)
    private String shippingPostalCode;

    @Column(name = "shipping_country", length = 2)
    private String shippingCountry;

    @Column(name = "shipping_notes", length = 500)
    private String shippingNotes;

    @Column(name = "shipping_carrier", length = 120)
    private String shippingCarrier;

    @Column(name = "tracking_number", length = 160)
    private String trackingNumber;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal discountTotal;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal total;

    @Column(length = 40)
    private String paymentProvider;

    @Column(length = 160)
    private String paymentReference;

    @Column(name = "payment_account_id")
    private UUID paymentAccountId;

    @Column(name = "payment_claim_bank", length = 120)
    private String paymentClaimBank;

    @Column(name = "payment_claim_reference", length = 160)
    private String paymentClaimReference;

    @Column(name = "payment_claim_payer", length = 160)
    private String paymentClaimPayer;

    @Column(name = "payment_claim_amount", precision = 19, scale = 4)
    private BigDecimal paymentClaimAmount;

    @Column(name = "payment_claimed_at")
    private LocalDateTime paymentClaimedAt;

    @Column(name = "payment_review_notes", length = 500)
    private String paymentReviewNotes;

    @Column(name = "payment_reviewed_at")
    private LocalDateTime paymentReviewedAt;

    @Column(name = "payment_reviewed_by", length = 180)
    private String paymentReviewedBy;

    @Column(name = "sale_id")
    private UUID saleId;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StorefrontOrderItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

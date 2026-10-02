package com.dismal.distribuciones.modules.security.customer.domain;

import com.dismal.distribuciones.modules.security.domain.CustomerType;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "customer_markets",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_customer_markets_user_country",
                columnNames = {"user_id", "country"}
        )
)
public class CustomerMarket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private StorefrontCountry country;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false, length = 24)
    @Builder.Default
    private CustomerType customerType = CustomerType.FINAL;

    @Column(name = "tax_id", length = 40)
    private String taxId;

    @Column(name = "billing_email", length = 180)
    private String billingEmail;

    @Column(name = "has_credit", nullable = false)
    @Builder.Default
    private boolean hasCredit = false;

    @Column(name = "credit_limit", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(name = "credit_used", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal creditUsed = BigDecimal.ZERO;

    @Column(name = "credit_days", nullable = false)
    @Builder.Default
    private Integer creditDays = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "primary_market", nullable = false)
    @Builder.Default
    private boolean primaryMarket = false;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

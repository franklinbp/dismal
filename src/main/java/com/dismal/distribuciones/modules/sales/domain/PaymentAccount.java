package com.dismal.distribuciones.modules.sales.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment_accounts")
public class PaymentAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentAccountType type;

    @Column(nullable = false, length = 8)
    private String currency;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private boolean defaultAccount;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "public_for_storefront", nullable = false)
    private boolean publicForStorefront;

    @Column(name = "bank_name", length = 120)
    private String bankName;

    @Column(name = "account_holder", length = 160)
    private String accountHolder;

    @Column(name = "account_number", length = 80)
    private String accountNumber;

    @Column(name = "account_type", length = 80)
    private String accountType;

    @Column(name = "tax_id", length = 40)
    private String taxId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

package com.dismal.distribuciones.modules.security.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")

public class User implements UserDetails{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String firstname;
    private String lastname;

    private String phone;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type")
    @Builder.Default
    private CustomerType customerType = CustomerType.FINAL;

    // --- Customer & Billing Details ---

    private String taxId; // RUC o CI

    private String billingEmail;

    @Column(columnDefinition = "boolean default false")
    @Builder.Default
    private boolean hasCredit = false;

    @Builder.Default
    @Column(precision = 19, scale = 4)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 19, scale = 4)
    private BigDecimal creditUsed = BigDecimal.ZERO;

    @Builder.Default
    private Integer creditDays = 0;

    @Column(columnDefinition = "boolean default true")
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "security_version", nullable = false)
    @Builder.Default
    private Integer securityVersion = 0;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // isEnabled is now handled by Lombok because we have a field named 'enabled'
}

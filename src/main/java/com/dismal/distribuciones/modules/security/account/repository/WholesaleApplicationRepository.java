package com.dismal.distribuciones.modules.security.account.repository;

import com.dismal.distribuciones.modules.security.account.domain.WholesaleApplication;
import com.dismal.distribuciones.modules.security.account.domain.WholesaleApplicationStatus;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WholesaleApplicationRepository extends JpaRepository<WholesaleApplication, UUID> {
    boolean existsByUserAndStatus(User user, WholesaleApplicationStatus status);
    boolean existsByUserAndCountryAndStatus(
            User user,
            StorefrontCountry country,
            WholesaleApplicationStatus status
    );
    Optional<WholesaleApplication> findFirstByUserOrderByCreatedAtDesc(User user);
    Optional<WholesaleApplication> findFirstByUserAndCountryOrderByCreatedAtDesc(
            User user,
            StorefrontCountry country
    );
    Page<WholesaleApplication> findAllByStatus(WholesaleApplicationStatus status, Pageable pageable);
}

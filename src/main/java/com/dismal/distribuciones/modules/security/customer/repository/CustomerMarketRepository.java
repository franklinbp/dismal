package com.dismal.distribuciones.modules.security.customer.repository;

import com.dismal.distribuciones.modules.security.customer.domain.CustomerMarket;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerMarketRepository extends JpaRepository<CustomerMarket, UUID> {

    Optional<CustomerMarket> findByUserIdAndCountry(UUID userId, StorefrontCountry country);

    List<CustomerMarket> findAllByUserIdOrderByCountryAsc(UUID userId);

    List<CustomerMarket> findAllByCountryOrderByUserEmailAsc(StorefrontCountry country);

    Optional<CustomerMarket> findFirstByUserIdAndPrimaryMarketTrue(UUID userId);

    boolean existsByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select market from CustomerMarket market where market.id = :id")
    Optional<CustomerMarket> findByIdForUpdate(@Param("id") UUID id);
}

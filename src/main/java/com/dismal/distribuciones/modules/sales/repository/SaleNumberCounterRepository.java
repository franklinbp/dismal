package com.dismal.distribuciones.modules.sales.repository;

import com.dismal.distribuciones.modules.sales.domain.SaleNumberCounter;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SaleNumberCounterRepository extends JpaRepository<SaleNumberCounter, StorefrontCountry> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select counter from SaleNumberCounter counter where counter.country = :country")
    Optional<SaleNumberCounter> findByCountryForUpdate(@Param("country") StorefrontCountry country);
}

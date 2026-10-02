package com.dismal.distribuciones.modules.pricing.repository;

import com.dismal.distribuciones.modules.pricing.domain.PriceList;
import com.dismal.distribuciones.modules.pricing.domain.PriceListType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PriceListRepository extends JpaRepository<PriceList, UUID> {
    Optional<PriceList> findByType(PriceListType type);
}

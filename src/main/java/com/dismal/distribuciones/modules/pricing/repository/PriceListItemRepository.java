package com.dismal.distribuciones.modules.pricing.repository;

import com.dismal.distribuciones.modules.pricing.domain.PriceListItem;
import com.dismal.distribuciones.modules.pricing.domain.PriceListType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface PriceListItemRepository extends JpaRepository<PriceListItem, UUID> {

    Optional<PriceListItem> findByPriceListIdAndSoftwareId(UUID priceListId, UUID softwareId);

    @Query("""
            select pli.price
            from PriceListItem pli
            where pli.priceList.type = :type
              and pli.priceList.enabled = true
              and pli.software.id = :softwareId
            """)
    Optional<BigDecimal> findPriceByTypeAndSoftwareId(@Param("type") PriceListType type, @Param("softwareId") UUID softwareId);
}

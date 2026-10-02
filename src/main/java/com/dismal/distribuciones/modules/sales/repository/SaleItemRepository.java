package com.dismal.distribuciones.modules.sales.repository;

import com.dismal.distribuciones.modules.sales.domain.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, UUID> {

    @Query("select si.software.id as softwareId, si.software.name as softwareName, " +
            "sum(si.quantity) as units, coalesce(sum(si.subtotal), 0) as total " +
            "from SaleItem si " +
            "where si.sale.createdAt >= :start and si.sale.createdAt < :end " +
            "group by si.software.id, si.software.name " +
            "order by total desc")
    List<SaleItemAggregate> aggregateBySoftware(@Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    @Query("select coalesce(sum(si.quantity), 0) from SaleItem si " +
            "where si.software.id = :softwareId " +
            "and si.sale.createdAt >= :start " +
            "and si.sale.status <> com.dismal.distribuciones.modules.sales.domain.SaleStatus.CANCELLED")
    long sumUnitsBySoftwareSince(@Param("softwareId") UUID softwareId,
                                 @Param("start") LocalDateTime start);

    @Query("select max(si.sale.createdAt) from SaleItem si " +
            "where si.software.id = :softwareId " +
            "and si.sale.status <> com.dismal.distribuciones.modules.sales.domain.SaleStatus.CANCELLED")
    LocalDateTime findLastSaleAtBySoftware(@Param("softwareId") UUID softwareId);
}

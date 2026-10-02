package com.dismal.distribuciones.modules.sales.repository;

import com.dismal.distribuciones.modules.sales.domain.SaleFulfillment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

@Repository
public interface SaleFulfillmentRepository extends JpaRepository<SaleFulfillment, UUID> {
    boolean existsBySaleId(UUID saleId);

    void deleteBySaleId(UUID saleId);

    @Query("select sf from SaleFulfillment sf " +
            "join fetch sf.license l " +
            "join fetch l.software s " +
            "where sf.sale.id = :saleId " +
            "order by sf.assignedAt asc")
    List<SaleFulfillment> findBySaleIdWithLicense(@Param("saleId") UUID saleId);

    @Query("select sf from SaleFulfillment sf " +
            "join fetch sf.sale s " +
            "join fetch s.client c " +
            "where sf.license.id = :licenseId " +
            "order by sf.assignedAt desc")
    List<SaleFulfillment> findByLicenseIdWithClient(@Param("licenseId") UUID licenseId);

    boolean existsByLicenseId(UUID licenseId);

    @Query("select coalesce(sum(sf.activationCost), 0) from SaleFulfillment sf " +
            "where sf.sale.createdAt >= :start and sf.sale.createdAt < :end " +
            "and sf.sale.status <> com.dismal.distribuciones.modules.sales.domain.SaleStatus.CANCELLED")
    java.math.BigDecimal sumActivationCostBetween(@Param("start") java.time.LocalDateTime start,
                                                  @Param("end") java.time.LocalDateTime end);

    @Query("select coalesce(sum(si.unitPrice - coalesce(sf.activationCost, l.purchasePrice, 0)), 0) " +
            "from SaleFulfillment sf " +
            "join sf.sale s " +
            "join sf.license l " +
            "join SaleItem si on si.sale = s and si.software = l.software " +
            "where s.createdAt >= :start and s.createdAt < :end " +
            "and s.status <> com.dismal.distribuciones.modules.sales.domain.SaleStatus.CANCELLED")
    java.math.BigDecimal sumProfitBetween(@Param("start") java.time.LocalDateTime start,
                                          @Param("end") java.time.LocalDateTime end);

    @Query("select l.software.id, avg(sf.activationCost) " +
            "from SaleFulfillment sf " +
            "join sf.license l " +
            "where sf.activationCost is not null " +
            "group by l.software.id")
    List<Object[]> findAverageActivationCostBySoftware();
}

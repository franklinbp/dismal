package com.dismal.distribuciones.modules.sales.repository;

import com.dismal.distribuciones.modules.sales.domain.Sale;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SaleRepository extends JpaRepository<Sale, UUID>, JpaSpecificationExecutor<Sale> {

    @EntityGraph(attributePaths = {"client", "items", "items.software"})
    Optional<Sale> findWithItemsById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sale s where s.id = :id")
    Optional<Sale> findByIdForUpdate(@Param("id") UUID id);

    @Query("select count(s), coalesce(sum(s.total), 0) from Sale s " +
            "where s.createdAt >= :start and s.createdAt < :end and s.status <> 'CANCELLED'")
    Object[] sumAndCountBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("select coalesce(sum(s.total), 0) from Sale s " +
            "where s.createdAt >= :start and s.createdAt < :end and s.status <> 'CANCELLED'")
    BigDecimal sumTotalBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @EntityGraph(attributePaths = {"client"})
    List<Sale> findTop20ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"client"})
    @Query("select s from Sale s " +
            "where (:from is null or s.createdAt >= :from) " +
            "and (:to is null or s.createdAt < :to) " +
            "and s.status <> 'CANCELLED' " +
            "order by s.createdAt desc")
    Page<Sale> findRecentSales(@Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to,
                               Pageable pageable);

    @Query("select function('date_trunc', :period, s.createdAt) as period, " +
            "coalesce(sum(s.total), 0) as total, count(s) as count " +
            "from Sale s " +
            "where s.createdAt >= :start and s.createdAt < :end and s.status <> 'CANCELLED' " +
            "group by period " +
            "order by period")
    List<SalesAggregatePoint> getSalesByPeriod(@Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end,
                                               @Param("period") String period);

    @Override
    @EntityGraph(attributePaths = {"client"})
    Page<Sale> findAll(Specification<Sale> spec, Pageable pageable);

    @Query("select distinct s.id from Sale s join s.items i where i.software.id = :softwareId")
    List<UUID> findSaleIdsBySoftwareId(@Param("softwareId") UUID softwareId);
}

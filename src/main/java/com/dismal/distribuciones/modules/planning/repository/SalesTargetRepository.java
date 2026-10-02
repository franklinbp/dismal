package com.dismal.distribuciones.modules.planning.repository;

import com.dismal.distribuciones.modules.planning.domain.SalesTarget;
import com.dismal.distribuciones.modules.planning.domain.SalesTargetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;

@Repository
public interface SalesTargetRepository extends JpaRepository<SalesTarget, UUID> {

    @Query("select s from SalesTarget s where s.deadline is not null and s.deadline between :start and :end")
    List<SalesTarget> findByDeadlineBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    List<SalesTarget> findByStatusOrderByPeriodEndAscCreatedAtDesc(SalesTargetStatus status);

    @Query("select s from SalesTarget s " +
            "where s.software.id = :softwareId " +
            "and s.status = :status " +
            "order by s.createdAt desc")
    List<SalesTarget> findBySoftwareIdAndStatus(@Param("softwareId") UUID softwareId,
                                                @Param("status") SalesTargetStatus status);

    @Query("select count(s) from SalesTarget s " +
            "where s.status = com.dismal.distribuciones.modules.planning.domain.SalesTargetStatus.ACTIVE " +
            "and s.salePrice <= s.variableCost")
    long countNonProfitable();

    @Query("select s from SalesTarget s " +
            "where s.status = com.dismal.distribuciones.modules.planning.domain.SalesTargetStatus.ACTIVE " +
            "and (:from is null or s.deadline >= :from) " +
            "and (:to is null or s.deadline <= :to) " +
            "order by (s.salePrice - s.variableCost) desc")
    Page<SalesTarget> findByMargin(@Param("from") LocalDate from,
                                   @Param("to") LocalDate to,
                                   Pageable pageable);

    @Query("select s from SalesTarget s " +
            "where s.status = com.dismal.distribuciones.modules.planning.domain.SalesTargetStatus.ACTIVE " +
            "and (:from is null or s.deadline >= :from) " +
            "and (:to is null or s.deadline <= :to) " +
            "order by (s.metaUnits * (s.salePrice - s.variableCost) - coalesce(s.fixedCostProduct, 0)) desc")
    Page<SalesTarget> findByExpectedProfit(@Param("from") LocalDate from,
                                           @Param("to") LocalDate to,
                                           Pageable pageable);

    @Query("select s from SalesTarget s " +
            "where s.status = com.dismal.distribuciones.modules.planning.domain.SalesTargetStatus.ACTIVE " +
            "and (:from is null or s.deadline >= :from) " +
            "and (:to is null or s.deadline <= :to) " +
            "order by case when s.deadline is null then 1 else 0 end, s.deadline asc")
    Page<SalesTarget> findByDeadline(@Param("from") LocalDate from,
                                     @Param("to") LocalDate to,
                                     Pageable pageable);

    @Modifying
    @Query("update SalesTarget s " +
            "set s.unitsSoldCurrent = s.unitsSoldCurrent + :delta " +
            "where s.software.id = :softwareId " +
            "and s.status = com.dismal.distribuciones.modules.planning.domain.SalesTargetStatus.ACTIVE")
    int incrementUnitsSoldCurrent(@Param("softwareId") UUID softwareId,
                                  @Param("delta") int delta);
}

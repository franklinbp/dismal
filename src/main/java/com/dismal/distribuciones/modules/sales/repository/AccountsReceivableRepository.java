package com.dismal.distribuciones.modules.sales.repository;

import com.dismal.distribuciones.modules.sales.domain.AccountsReceivable;
import com.dismal.distribuciones.modules.sales.domain.AccountsReceivableStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountsReceivableRepository extends JpaRepository<AccountsReceivable, UUID> {
    Optional<AccountsReceivable> findBySaleId(UUID saleId);

    void deleteBySaleId(UUID saleId);

    List<AccountsReceivable> findByClientId(UUID clientId);

    @EntityGraph(attributePaths = {"client", "sale", "sale.items", "sale.items.software"})
    List<AccountsReceivable> findByStatusAndBalanceGreaterThanOrderByDueDateAsc(
            AccountsReceivableStatus status,
            BigDecimal balance
    );

    @EntityGraph(attributePaths = {"client", "sale", "sale.items", "sale.items.software"})
    List<AccountsReceivable> findByClientIdAndStatusAndBalanceGreaterThanOrderByDueDateAsc(
            UUID clientId,
            AccountsReceivableStatus status,
            BigDecimal balance
    );

    @Query("select count(ar), coalesce(sum(ar.balance), 0) from AccountsReceivable ar where ar.status = :status")
    Object[] sumAndCountByStatus(@Param("status") AccountsReceivableStatus status);

    @EntityGraph(attributePaths = {"client"})
    List<AccountsReceivable> findTop20ByStatusOrderByDueDateAsc(AccountsReceivableStatus status);

    @EntityGraph(attributePaths = {"client"})
    @Query("select ar from AccountsReceivable ar " +
            "where ar.status = :status " +
            "and (:from is null or ar.dueDate >= :from) " +
            "and (:to is null or ar.dueDate <= :to) " +
            "order by ar.dueDate asc")
    Page<AccountsReceivable> findByStatusAndDueDateRange(@Param("status") AccountsReceivableStatus status,
                                                         @Param("from") LocalDate from,
                                                         @Param("to") LocalDate to,
                                                         Pageable pageable);

    @Query("select coalesce(sum(ar.balance), 0) from AccountsReceivable ar " +
            "where ar.client.id = :clientId and ar.status in :statuses")
    BigDecimal sumBalanceByClientAndStatusIn(@Param("clientId") UUID clientId,
                                             @Param("statuses") List<AccountsReceivableStatus> statuses);

    @Query("select coalesce(sum(ar.balance), 0) from AccountsReceivable ar " +
            "where ar.sale.customerMarket.id = :marketId and ar.status in :statuses")
    BigDecimal sumBalanceByCustomerMarketAndStatusIn(
            @Param("marketId") UUID marketId,
            @Param("statuses") List<AccountsReceivableStatus> statuses
    );

    @Query("select min(ar.dueDate) from AccountsReceivable ar " +
            "where ar.client.id = :clientId and ar.status = com.dismal.distribuciones.modules.sales.domain.AccountsReceivableStatus.OVERDUE")
    LocalDate findOldestOverdueDate(@Param("clientId") UUID clientId);

    @Query("select min(ar.dueDate) from AccountsReceivable ar " +
            "where ar.sale.customerMarket.id = :marketId " +
            "and ar.status = com.dismal.distribuciones.modules.sales.domain.AccountsReceivableStatus.OVERDUE")
    LocalDate findOldestOverdueDateByCustomerMarket(@Param("marketId") UUID marketId);

    @EntityGraph(attributePaths = {"client"})
    @Query("select ar from AccountsReceivable ar " +
            "where ar.dueDate < :today and ar.balance > 0 and ar.status in :statuses")
    List<AccountsReceivable> findOverdueCandidates(@Param("today") LocalDate today,
                                                   @Param("statuses") List<AccountsReceivableStatus> statuses);
}

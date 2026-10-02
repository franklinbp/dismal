package com.dismal.distribuciones.modules.sales.repository;

import com.dismal.distribuciones.modules.sales.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.sale.id = :saleId")
    BigDecimal sumAmountBySaleId(@Param("saleId") UUID saleId);

    List<Payment> findBySaleIdOrderByCreatedAtAsc(UUID saleId);

    boolean existsBySaleId(UUID saleId);

    void deleteBySaleId(UUID saleId);

    @Query("select p.sale.id, coalesce(sum(p.amount), 0) from Payment p where p.sale.id in :saleIds group by p.sale.id")
    List<Object[]> sumAmountsBySaleIds(@Param("saleIds") List<UUID> saleIds);

    @Query("select coalesce(sum(p.amount), 0) from Payment p " +
            "where p.createdAt >= :start and p.createdAt < :end")
    BigDecimal sumAmountBetween(@Param("start") java.time.LocalDateTime start,
                                @Param("end") java.time.LocalDateTime end);

    @Query("select p.paymentAccount.id, coalesce(sum(p.amount), 0), count(p), max(p.createdAt) " +
            "from Payment p where p.paymentAccount is not null group by p.paymentAccount.id")
    List<Object[]> summarizeByPaymentAccount();

    @Query(
            value = "select p from Payment p " +
                    "join fetch p.paymentAccount a " +
                    "join fetch p.sale s " +
                    "join fetch s.client " +
                    "where (:accountId is null or a.id = :accountId) order by p.createdAt desc",
            countQuery = "select count(p) from Payment p " +
                    "where p.paymentAccount is not null " +
                    "and (:accountId is null or p.paymentAccount.id = :accountId)"
    )
    Page<Payment> findAccountMovements(@Param("accountId") UUID accountId, Pageable pageable);
}

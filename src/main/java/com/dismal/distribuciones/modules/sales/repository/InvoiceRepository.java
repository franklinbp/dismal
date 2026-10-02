package com.dismal.distribuciones.modules.sales.repository;

import com.dismal.distribuciones.modules.sales.domain.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {

    /**
     * Calculates the sum of totalAmount for all invoices with a 'PENDING' status,
     * grouped by the customer's ID. This is highly efficient as it performs the aggregation
     * in a single database query.
     * @return A list of CustomerBalanceView projections, each containing a userId and their total pending balance.
     */
    @Query("SELECT COALESCE(s.client.id, o.customer.id) AS userId, SUM(i.totalAmount) AS totalPending " +
           "FROM Invoice i " +
           "LEFT JOIN i.sale s " +
           "LEFT JOIN i.order o " +
           "WHERE i.status = com.dismal.distribuciones.modules.sales.domain.InvoiceStatus.PENDING " +
           "GROUP BY COALESCE(s.client.id, o.customer.id)")
    List<CustomerBalanceView> findPendingBalances();

    Optional<Invoice> findBySaleId(UUID saleId);

    void deleteBySaleId(UUID saleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Invoice> findTopByInvoiceNumberStartingWithOrderByInvoiceNumberDesc(String prefix);

}

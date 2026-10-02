package com.dismal.distribuciones.modules.sales.repository;

import com.dismal.distribuciones.modules.sales.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Calculates the total sales revenue within a given date range, grouped by a specified time period (day, week, month).
     * This uses a native PostgreSQL function 'date_trunc' for high performance.
     * @param period The time unit to group by (e.g., 'day', 'week', 'month').
     * @param startDate The start of the date range.
     * @param endDate The end of the date range.
     * @return A list of SalesDataPoint projections with the date and total sales for that period.
     */
    @Query("SELECT function('date_trunc', :period, o.purchaseDate) AS date, SUM(o.salePrice) AS totalSales " +
           "FROM Order o " +
           "WHERE o.purchaseDate >= :startDate AND o.purchaseDate <= :endDate " +
           "GROUP BY date " +
           "ORDER BY date")
    List<SalesDataPoint> getSalesByPeriod(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate,
                                          @Param("period") String period);

    /**
     * Calculates the total net profit within a given date range.
     * Profit for each order is calculated as (salePrice - license.purchasePrice).
     * COALESCE is used to ensure 0 is returned if there are no sales, instead of null.
     * @param startDate The start of the date range.
     * @param endDate The end of the date range.
     * @return The total net profit as a BigDecimal.
     */
    @Query("SELECT COALESCE(SUM(o.salePrice - o.assignedLicense.purchasePrice), 0) " +
           "FROM Order o " +
           "WHERE o.purchaseDate >= :startDate AND o.purchaseDate <= :endDate")
    BigDecimal getNetProfitByDateRange(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    long countByPurchasedSoftwareIdAndPurchaseDateAfter(UUID softwareId, LocalDateTime date);
}


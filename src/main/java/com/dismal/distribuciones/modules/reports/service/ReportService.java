package com.dismal.distribuciones.modules.reports.service;

import com.dismal.distribuciones.modules.reports.dto.*;
import com.dismal.distribuciones.modules.planning.domain.SalesTarget;
import com.dismal.distribuciones.modules.planning.repository.SalesTargetRepository;
import com.dismal.distribuciones.modules.sales.repository.OrderRepository;
import com.dismal.distribuciones.modules.sales.repository.SaleItemAggregate;
import com.dismal.distribuciones.modules.sales.repository.SaleItemRepository;
import com.dismal.distribuciones.modules.sales.repository.SaleRepository;
import com.dismal.distribuciones.modules.sales.repository.SaleFulfillmentRepository;
import com.dismal.distribuciones.modules.security.domain.Role;
import com.dismal.distribuciones.modules.security.repository.UserRepository;
import com.dismal.distribuciones.modules.store.repository.SoftwareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final SoftwareRepository softwareRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final SalesTargetRepository salesTargetRepository;
    private final SaleFulfillmentRepository saleFulfillmentRepository;

    /**
     * Generates a sales report grouped by a specified time period within a date range.
     * @param startDate The start date for the report.
     * @param endDate The end date for the report.
     * @param period The grouping period (e.g., "day", "week", "month").
     * @return A SalesReportDTO containing the aggregated sales data.
     */
    @Transactional(readOnly = true)
    public SalesReportDTO generateSalesReport(LocalDateTime startDate, LocalDateTime endDate, String period) {
        List<SalesDataPointDTO> dataPoints = orderRepository.getSalesByPeriod(startDate, endDate, period)
                .stream()
                .map(sdp -> new SalesDataPointDTO(sdp.getDate(), sdp.getTotalSales()))
                .collect(Collectors.toList());

        return new SalesReportDTO(startDate, endDate, period, dataPoints);
    }

    @Transactional(readOnly = true)
    public SalesPeriodReportResponse generateSalesPeriodReport(LocalDate from, LocalDate to, String period) {
        LocalDate resolvedFrom = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        LocalDateTime start = resolvedFrom.atStartOfDay();
        LocalDateTime end = resolvedTo.plusDays(1).atStartOfDay();

        List<SalesPeriodPointResponse> points = saleRepository.getSalesByPeriod(start, end, period).stream()
                .map(point -> new SalesPeriodPointResponse(
                        point.getPeriod().toLocalDate(),
                        point.getTotal() != null ? point.getTotal() : BigDecimal.ZERO,
                        point.getCount() != null ? point.getCount() : 0L
                ))
                .toList();

        BigDecimal totalAmount = points.stream()
                .map(SalesPeriodPointResponse::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalCount = points.stream().mapToLong(SalesPeriodPointResponse::count).sum();

        return new SalesPeriodReportResponse(resolvedFrom, resolvedTo, period, totalAmount, totalCount, points);
    }

    @Transactional(readOnly = true)
    public SalesPeriodReportResponse generateMonthlyReport(int year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        return generateSalesPeriodReport(startDate, endDate, "month");
    }

    @Transactional(readOnly = true)
    public SalesVsTargetReportResponse generateSalesVsTargets(LocalDate from, LocalDate to) {
        LocalDate resolvedFrom = from != null ? from : LocalDate.now().minusMonths(1);
        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        LocalDateTime start = resolvedFrom.atStartOfDay();
        LocalDateTime end = resolvedTo.plusDays(1).atStartOfDay();

        List<SalesTarget> targets = salesTargetRepository.findAll().stream()
                .filter(target -> {
                    if (target.getDeadline() == null) {
                        return true;
                    }
                    LocalDate deadline = target.getDeadline();
                    return !deadline.isBefore(resolvedFrom) && !deadline.isAfter(resolvedTo);
                })
                .toList();

        Map<java.util.UUID, SaleItemAggregate> actualBySoftware = saleItemRepository.aggregateBySoftware(start, end)
                .stream()
                .collect(Collectors.toMap(SaleItemAggregate::getSoftwareId, Function.identity()));

        List<SalesVsTargetItemResponse> items = targets.stream()
                .map(target -> {
                    SaleItemAggregate aggregate = actualBySoftware.get(target.getSoftware().getId());
                    long actualUnits = aggregate != null && aggregate.getUnits() != null ? aggregate.getUnits() : 0L;
                    long variance = actualUnits - target.getMetaUnits();
                    boolean achieved = actualUnits >= target.getMetaUnits();
                    return new SalesVsTargetItemResponse(
                            target.getId(),
                            target.getSoftware().getId(),
                            target.getSoftware().getName(),
                            target.getMetaUnits(),
                            actualUnits,
                            variance,
                            achieved,
                            target.getSalePrice(),
                            target.getDeadline()
                    );
                })
                .toList();

        long achievedTargets = items.stream().filter(SalesVsTargetItemResponse::achieved).count();
        long totalActualUnits = items.stream().mapToLong(SalesVsTargetItemResponse::actualUnits).sum();

        return new SalesVsTargetReportResponse(
                resolvedFrom,
                resolvedTo,
                items.size(),
                achievedTargets,
                totalActualUnits,
                items
        );
    }

    /**
     * Generates a profit report for a given date range.
     * @param startDate The start date for the report.
     * @param endDate The end date for the report.
     * @return A ProfitReportDTO containing the net profit.
     */
    @Transactional(readOnly = true)
    public ProfitReportDTO generateProfitReport(LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal orderProfit = orderRepository.getNetProfitByDateRange(startDate, endDate);
        BigDecimal salesRevenue = saleRepository.sumTotalBetween(startDate, endDate);
        BigDecimal salesCost = saleFulfillmentRepository.sumActivationCostBetween(startDate, endDate);
        BigDecimal netProfit = orderProfit.add(salesRevenue.subtract(salesCost));
        return new ProfitReportDTO(startDate, endDate, netProfit);
    }

    /**
     * Retrieves a list of customers who have registered but never made a purchase.
     * @return A list of InactiveCustomerDTOs.
     */
    @Transactional(readOnly = true)
    public List<InactiveCustomerDTO> getInactiveCustomers() {
        return userRepository.findCustomersWithNoOrders().stream()
                .map(user -> new InactiveCustomerDTO(
                        user.getId(),
                        user.getFirstname() + " " + user.getLastname(),
                        user.getEmail(),
                        user.getPhone()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a list of software products that have available stock but no recent sales.
     * @return A list of ColdStockProductDTOs.
     */
    @Transactional(readOnly = true)
    public List<ColdStockProductDTO> getColdStockAlerts() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return softwareRepository.findInStockSoftwareWithoutRecentSales(thirtyDaysAgo).stream()
                .map(software -> new ColdStockProductDTO(
                        software.getId(),
                        software.getName(),
                        software.getPlatform(),
                        software.getPrice(),
                        software.getImageUrl()
                ))
                .collect(Collectors.toList());
    }
}

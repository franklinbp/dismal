package com.dismal.distribuciones.modules.dashboard.service;

import com.dismal.distribuciones.modules.dashboard.dto.*;
import com.dismal.distribuciones.modules.integrations.outbox.domain.EventOutboxStatus;
import com.dismal.distribuciones.modules.integrations.outbox.repository.EventOutboxRepository;
import com.dismal.distribuciones.modules.planning.repository.SalesTargetRepository;
import com.dismal.distribuciones.modules.planning.service.SalesTargetCalculatorService;
import com.dismal.distribuciones.modules.sales.domain.AccountsReceivable;
import com.dismal.distribuciones.modules.sales.domain.AccountsReceivableStatus;
import com.dismal.distribuciones.modules.sales.domain.Sale;
import com.dismal.distribuciones.modules.sales.repository.AccountsReceivableRepository;
import com.dismal.distribuciones.modules.sales.repository.PaymentRepository;
import com.dismal.distribuciones.modules.sales.repository.SaleRepository;
import com.dismal.distribuciones.modules.sales.repository.SaleFulfillmentRepository;
import com.dismal.distribuciones.modules.sales.repository.SalesAggregatePoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardService {

    private final SaleRepository saleRepository;
    private final PaymentRepository paymentRepository;
    private final AccountsReceivableRepository accountsReceivableRepository;
    private final SalesTargetRepository salesTargetRepository;
    private final SalesTargetCalculatorService calculatorService;
    private final EventOutboxRepository eventOutboxRepository;
    private final SaleFulfillmentRepository saleFulfillmentRepository;

    // A failed optional metric must not mark the whole dashboard request for rollback.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public DashboardSummaryResponse getSummary() {
        try {
            LocalDate today = LocalDate.now();
            LocalDateTime todayStart = today.atStartOfDay();
            LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();

            YearMonth month = YearMonth.now();
            LocalDateTime monthStart = month.atDay(1).atStartOfDay();
            LocalDateTime nextMonthStart = month.plusMonths(1).atDay(1).atStartOfDay();

            Object[] todayTotals = safeSalesTotals(todayStart, tomorrowStart);
            Object[] monthTotals = safeSalesTotals(monthStart, nextMonthStart);
            BigDecimal todayProfit = safeProfitTotal(todayStart, tomorrowStart);
            BigDecimal monthProfit = safeProfitTotal(monthStart, nextMonthStart);

            Object[] openArTotals = safeArTotals(AccountsReceivableStatus.OPEN);
            Object[] overdueArTotals = safeArTotals(AccountsReceivableStatus.OVERDUE);

            long nonProfitableCount = safeNonProfitableCount();
            long outboxFailedCount = safeOutboxFailedCount();

            BigDecimal monthExpectedProfit = calculateMonthExpectedProfit(month);

            return new DashboardSummaryResponse(
                    safeCount(todayTotals),
                    safeSum(todayTotals),
                    todayProfit,
                    safeCount(monthTotals),
                    safeSum(monthTotals),
                    monthProfit,
                    monthExpectedProfit,
                    safeCount(overdueArTotals),
                    safeSum(overdueArTotals),
                    safeCount(openArTotals),
                    safeSum(openArTotals),
                    nonProfitableCount,
                    outboxFailedCount
            );
        } catch (RuntimeException ex) {
            log.warn("Dashboard summary unavailable", ex);
            return DashboardSummaryResponse.empty();
        }
    }

    @Transactional(readOnly = true)
    public Page<RecentSaleDto> getRecentSales(LocalDate from, LocalDate to, int page, int size) {
        Pageable pageable = sanitizePage(page, size);
        try {
            Specification<Sale> spec = Specification.where(
                    (root, query, cb) -> cb.notEqual(root.get("status"), com.dismal.distribuciones.modules.sales.domain.SaleStatus.CANCELLED)
            );
            if (from != null) {
                LocalDateTime fromDateTime = from.atStartOfDay();
                spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), fromDateTime));
            }
            if (to != null) {
                LocalDateTime toDateTime = to.plusDays(1).atStartOfDay();
                spec = spec.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), toDateTime));
            }
            Page<Sale> salesPage = saleRepository.findAll(spec, pageable);
            List<Sale> sales = salesPage.getContent();
            Map<UUID, BigDecimal> paidBySale = getPaidBySaleIds(sales);

            List<RecentSaleDto> dtos = sales.stream()
                    .map(sale -> {
                        BigDecimal paid = paidBySale.getOrDefault(sale.getId(), BigDecimal.ZERO);
                        BigDecimal balance = sale.getTotal().subtract(paid);
                        if (balance.signum() < 0) {
                            balance = BigDecimal.ZERO;
                        }
                        String name = "";
                        String email = "";
                        if (sale.getClient() != null) {
                            name = formatName(sale.getClient().getFirstname(), sale.getClient().getLastname());
                            email = sale.getClient().getEmail();
                        }
                        return new RecentSaleDto(
                                sale.getId(),
                                sale.getCreatedAt(),
                                name,
                                email,
                                sale.getSaleType(),
                                sale.getStatus(),
                                sale.getTotal(),
                                paid,
                                balance
                        );
                    })
                    .toList();
            return new PageImpl<>(dtos, pageable, salesPage.getTotalElements());
        } catch (RuntimeException ex) {
            log.warn("Dashboard recent sales unavailable: {}", ex.getMessage());
            return new PageImpl<>(List.of(), pageable, 0);
        }
    }

    @Transactional(readOnly = true)
    public Page<ArDto> getAr(AccountsReceivableStatus status, LocalDate from, LocalDate to, int page, int size) {
        Pageable pageable = sanitizePage(page, size);
        Page<AccountsReceivable> records;
        try {
            records = accountsReceivableRepository.findByStatusAndDueDateRange(status, from, to, pageable);
        } catch (RuntimeException ex) {
            log.warn("Dashboard AR list unavailable: {}", ex.getMessage());
            return new PageImpl<>(List.of(), pageable, 0);
        }
        List<ArDto> dtos = records.getContent().stream()
                .map(ar -> new ArDto(
                        ar.getId(),
                        ar.getClient() != null ? formatName(ar.getClient().getFirstname(), ar.getClient().getLastname()) : "",
                        ar.getClient() != null ? ar.getClient().getEmail() : "",
                        ar.getDueDate(),
                        ar.getTotal(),
                        ar.getPaid(),
                        ar.getBalance(),
                        ar.getStatus()
                ))
                .toList();
        return new PageImpl<>(dtos, pageable, records.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<SalesTargetDto> getSalesTargets(String sort, LocalDate from, LocalDate to, int page, int size) {
        Pageable pageable = sanitizePage(page, size);
        Page<com.dismal.distribuciones.modules.planning.domain.SalesTarget> targets;
        try {
            if ("deadline".equalsIgnoreCase(sort)) {
                targets = salesTargetRepository.findByDeadline(from, to, pageable);
            } else if ("margin".equalsIgnoreCase(sort)) {
                targets = salesTargetRepository.findByMargin(from, to, pageable);
            } else {
                targets = salesTargetRepository.findByExpectedProfit(from, to, pageable);
            }
        } catch (RuntimeException ex) {
            log.warn("Dashboard sales targets unavailable: {}", ex.getMessage());
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<SalesTargetDto> dtos = targets.getContent().stream()
                .map(target -> {
                    var metrics = calculatorService.calculate(target);
                    return new SalesTargetDto(
                            target.getId(),
                            target.getSoftware().getName(),
                            target.getMetaUnits(),
                            target.getUnitsSoldCurrent(),
                            metrics.getMarginUnit(),
                            metrics.getExpectedProfit(),
                            metrics.getBreakEvenUnits(),
                            target.getDeadline(),
                            metrics.isProfitable(),
                            metrics.isTargetAchieved()
                    );
                })
                .toList();
        return new PageImpl<>(dtos, pageable, targets.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<OutboxDto> getOutbox(EventOutboxStatus status, LocalDate from, LocalDate to, int page, int size) {
        if (status == null) {
            return new PageImpl<>(List.of(), sanitizePage(page, size), 0);
        }
        Pageable pageable = sanitizePage(page, size);
        try {
            Specification<com.dismal.distribuciones.modules.integrations.outbox.domain.EventOutbox> spec =
                    Specification.where((root, query, cb) -> cb.equal(root.get("status"), status));
            if (from != null) {
                var fromInstant = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
                spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), fromInstant));
            }
            if (to != null) {
                var toInstant = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
                spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), toInstant));
            }
            Page<com.dismal.distribuciones.modules.integrations.outbox.domain.EventOutbox> records =
                    eventOutboxRepository.findAll(spec, pageable);
            List<OutboxDto> dtos = records.getContent().stream()
                    .map(outbox -> new OutboxDto(
                            outbox.getId(),
                            outbox.getEventType(),
                            outbox.getAggregateId(),
                            outbox.getStatus(),
                            outbox.getAttempts(),
                            outbox.getLastError(),
                            outbox.getCreatedAt(),
                            outbox.getNextAttemptAt()
                    ))
                    .toList();
            return new PageImpl<>(dtos, pageable, records.getTotalElements());
        } catch (RuntimeException ex) {
            log.warn("Dashboard outbox unavailable: {}", ex.getMessage());
            return new PageImpl<>(List.of(), pageable, 0);
        }
    }

    private BigDecimal calculateMonthExpectedProfit(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        BigDecimal total = BigDecimal.ZERO;
        try {
            var targets = salesTargetRepository.findByDeadlineBetween(start, end);
            if (targets != null) {
                for (var target : targets) {
                    total = total.add(calculatorService.calculate(target).getExpectedProfit());
                }
            }
        } catch (RuntimeException ex) {
            log.warn("Dashboard expected profit unavailable: {}", ex.getMessage());
        }
        return total;
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getMonthlySales(int year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = startDate.plusYears(1);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atStartOfDay();
        List<com.dismal.distribuciones.modules.sales.repository.SalesAggregatePoint> points =
                saleRepository.getSalesByPeriod(start, end, "month");
        Map<String, BigDecimal> totalsByMonth = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        for (int month = 1; month <= 12; month++) {
            totalsByMonth.put(String.format("%04d-%02d", year, month), BigDecimal.ZERO);
        }
        for (com.dismal.distribuciones.modules.sales.repository.SalesAggregatePoint point : points) {
            if (point == null || point.getPeriod() == null) {
                continue;
            }
            String key = fmt.format(point.getPeriod().toLocalDate());
            totalsByMonth.put(key, point.getTotal() != null ? point.getTotal() : BigDecimal.ZERO);
        }
        return totalsByMonth;
    }

    private Map<UUID, BigDecimal> getPaidBySaleIds(List<Sale> sales) {
        Map<UUID, BigDecimal> map = new HashMap<>();
        List<UUID> ids = sales.stream().map(Sale::getId).toList();
        if (ids.isEmpty()) {
            return map;
        }
        List<Object[]> rows;
        try {
            rows = paymentRepository.sumAmountsBySaleIds(ids);
        } catch (RuntimeException ex) {
            log.warn("Dashboard payments unavailable: {}", ex.getMessage());
            return map;
        }
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            UUID saleId = safeUuid(row[0]);
            if (saleId == null) {
                continue;
            }
            BigDecimal sum = safeNumber(row[1]);
            map.put(saleId, sum);
        }
        return map;
    }

    private String formatName(String first, String last) {
        String name = (first != null ? first : "") + (last != null ? " " + last : "");
        return name.trim();
    }

    private Pageable sanitizePage(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        return PageRequest.of(safePage, safeSize);
    }

    private long safeCount(Object[] row) {
        Object[] normalized = normalizeRow(row);
        if (normalized == null || normalized.length == 0 || normalized[0] == null) {
            return 0L;
        }
        return ((Number) normalized[0]).longValue();
    }

    private BigDecimal safeSum(Object[] row) {
        Object[] normalized = normalizeRow(row);
        if (normalized == null || normalized.length < 2 || normalized[1] == null) {
            return BigDecimal.ZERO;
        }
        return safeNumber(normalized[1]);
    }

    private BigDecimal safeNumber(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    private Object[] normalizeRow(Object[] row) {
        if (row == null) {
            return null;
        }
        if (row.length == 1 && row[0] instanceof Object[] nested) {
            return nested;
        }
        return row;
    }

    private UUID safeUuid(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String string) {
            try {
                return UUID.fromString(string);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private Object[] safeSalesTotals(LocalDateTime start, LocalDateTime end) {
        try {
            return saleRepository.sumAndCountBetween(start, end);
        } catch (RuntimeException ex) {
            log.warn("Dashboard sales totals unavailable: {}", ex.getMessage());
            return new Object[] {0L, BigDecimal.ZERO};
        }
    }

    private BigDecimal safeProfitTotal(LocalDateTime start, LocalDateTime end) {
        try {
            BigDecimal total = saleFulfillmentRepository.sumProfitBetween(start, end);
            return total != null ? total : BigDecimal.ZERO;
        } catch (RuntimeException ex) {
            log.warn("Dashboard profit totals unavailable: {}", ex.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private Object[] safeArTotals(AccountsReceivableStatus status) {
        try {
            return accountsReceivableRepository.sumAndCountByStatus(status);
        } catch (RuntimeException ex) {
            log.warn("Dashboard AR totals unavailable: {}", ex.getMessage());
            return new Object[] {0L, BigDecimal.ZERO};
        }
    }

    private long safeNonProfitableCount() {
        try {
            return salesTargetRepository.countNonProfitable();
        } catch (RuntimeException ex) {
            log.warn("Dashboard non-profitable count unavailable: {}", ex.getMessage());
            return 0L;
        }
    }

    private long safeOutboxFailedCount() {
        try {
            return eventOutboxRepository.countByStatus(EventOutboxStatus.FAILED);
        } catch (RuntimeException ex) {
            log.warn("Dashboard outbox count unavailable: {}", ex.getMessage());
            return 0L;
        }
    }
}

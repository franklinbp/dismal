package com.dismal.distribuciones.modules.planning.service;

import com.dismal.distribuciones.exception.ResourceNotFoundException;
import com.dismal.distribuciones.modules.planning.domain.SalesTarget;
import com.dismal.distribuciones.modules.planning.domain.SalesTargetStatus;
import com.dismal.distribuciones.modules.planning.dto.SalesTargetRequest;
import com.dismal.distribuciones.modules.planning.dto.SalesTargetResponse;
import com.dismal.distribuciones.modules.planning.dto.SalesTargetSummaryResponse;
import com.dismal.distribuciones.modules.planning.dto.RealActivationCostResponse;
import com.dismal.distribuciones.modules.planning.repository.SalesTargetRepository;
import com.dismal.distribuciones.modules.security.domain.CustomerType;
import com.dismal.distribuciones.modules.store.repository.LicenseRepository;
import com.dismal.distribuciones.modules.store.domain.Software;
import com.dismal.distribuciones.modules.store.repository.SoftwareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SalesTargetService {

    private final SalesTargetRepository salesTargetRepository;
    private final SoftwareRepository softwareRepository;
    private final SalesTargetCalculatorService calculatorService;
    private final LicenseRepository licenseRepository;

    @Transactional
    public SalesTargetResponse create(SalesTargetRequest request) {
        SalesTarget target = new SalesTarget();
        target.setStatus(SalesTargetStatus.ACTIVE);
        target.setUnitsSoldCurrent(0);
        applyRequest(target, request);
        ensureNoOtherActiveTarget(target.getSoftware().getId(), null);
        SalesTarget saved = salesTargetRepository.save(target);
        return toResponse(saved);
    }

    @Transactional
    public SalesTargetResponse update(UUID id, SalesTargetRequest request) {
        SalesTarget target = salesTargetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales target not found with ID: " + id));
        ensureEditable(target);
        applyRequest(target, request);
        ensureNoOtherActiveTarget(target.getSoftware().getId(), target.getId());
        SalesTarget saved = salesTargetRepository.save(target);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        SalesTarget target = salesTargetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales target not found with ID: " + id));
        salesTargetRepository.delete(target);
    }

    @Transactional(readOnly = true)
    public SalesTargetResponse get(UUID id) {
        SalesTarget target = salesTargetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales target not found with ID: " + id));
        return toResponse(target);
    }

    @Transactional(readOnly = true)
    public List<SalesTargetResponse> list(SalesTargetStatus status) {
        SalesTargetStatus resolvedStatus = status != null ? status : SalesTargetStatus.ACTIVE;
        return salesTargetRepository.findByStatusOrderByPeriodEndAscCreatedAtDesc(resolvedStatus).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SalesTargetResponse close(UUID id) {
        SalesTarget target = salesTargetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales target not found with ID: " + id));
        if (target.getStatus() == SalesTargetStatus.CLOSED) {
            return toResponse(target);
        }
        target.setStatus(SalesTargetStatus.CLOSED);
        target.setClosedAt(Instant.now());
        if (target.getPeriodEnd() == null) {
            target.setPeriodEnd(target.getDeadline() != null ? target.getDeadline() : LocalDate.now());
        }
        if (target.getDeadline() == null) {
            target.setDeadline(target.getPeriodEnd());
        }
        SalesTarget saved = salesTargetRepository.save(target);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public SalesTargetSummaryResponse summary() {
        var targets = salesTargetRepository.findByStatusOrderByPeriodEndAscCreatedAtDesc(SalesTargetStatus.ACTIVE);
        int totalTargets = targets.size();
        int achieved = 0;
        int totalMetaUnits = 0;
        BigDecimal totalTargetRevenue = BigDecimal.ZERO;
        BigDecimal totalContribution = BigDecimal.ZERO;
        BigDecimal totalFixedCost = BigDecimal.ZERO;
        BigDecimal totalExpectedProfit = BigDecimal.ZERO;

        for (SalesTarget target : targets) {
            var metrics = calculatorService.calculate(target);
            totalMetaUnits += target.getMetaUnits();
            totalTargetRevenue = totalTargetRevenue.add(metrics.getTargetRevenue());
            totalContribution = totalContribution.add(metrics.getContributionTotal());
            totalFixedCost = totalFixedCost.add(metrics.getFixedCostApplied());
            totalExpectedProfit = totalExpectedProfit.add(metrics.getExpectedProfit());
            if (metrics.isTargetAchieved()) {
                achieved++;
            }
        }

        int pending = totalTargets - achieved;
        return new SalesTargetSummaryResponse(
                totalTargets,
                achieved,
                pending,
                totalMetaUnits,
                totalTargetRevenue,
                totalContribution,
                totalFixedCost,
                totalExpectedProfit
        );
    }

    @Transactional(readOnly = true)
    public List<RealActivationCostResponse> realActivationCosts() {
        List<Object[]> rows = licenseRepository.sumCostAndActivationsBySoftware();
        return rows.stream()
                .filter(row -> row.length >= 3 && row[0] instanceof UUID)
                .map(row -> {
                    UUID softwareId = (UUID) row[0];
                    BigDecimal totalCost = row[1] instanceof BigDecimal ? (BigDecimal) row[1] : BigDecimal.ZERO;
                    BigDecimal totalActivations = row[2] instanceof Number
                            ? BigDecimal.valueOf(((Number) row[2]).longValue())
                            : BigDecimal.ZERO;
                    BigDecimal avg = null;
                    if (totalActivations.signum() > 0) {
                        avg = totalCost.divide(totalActivations, 4, RoundingMode.HALF_UP);
                    }
                    return new RealActivationCostResponse(softwareId, avg);
                })
                .toList();
    }

    private void applyRequest(SalesTarget target, SalesTargetRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Sales target request is required");
        }
        Software software = softwareRepository.findById(request.softwareId())
                .orElseThrow(() -> new ResourceNotFoundException("Software not found with ID: " + request.softwareId()));

        requireNonNegative(request.metaUnits(), "metaUnits");
        requireNonNegative(request.variableCost(), "variableCost");
        if (request.fixedCostProduct() != null) {
            requireNonNegative(request.fixedCostProduct(), "fixedCostProduct");
        }
        Integer resolvedUnitsSold = request.unitsSoldCurrent() != null
                ? request.unitsSoldCurrent()
                : (target.getUnitsSoldCurrent() != null ? target.getUnitsSoldCurrent() : 0);
        requireNonNegative(resolvedUnitsSold, "unitsSoldCurrent");

        String countryCode = request.countryCode() != null
                ? normalizeCountryCode(request.countryCode())
                : normalizeCountryCode(target.getCountryCode());
        CustomerType customerType = request.customerType() != null
                ? request.customerType()
                : (target.getCustomerType() != null ? target.getCustomerType() : CustomerType.FINAL);
        BigDecimal resolvedSalePrice = request.salePrice() != null
                ? request.salePrice()
                : software.getPrice();
        requireNonNegative(resolvedSalePrice, "salePrice");

        target.setSoftware(software);
        target.setMetaUnits(request.metaUnits());
        target.setCountryCode(countryCode);
        target.setCustomerType(customerType);
        target.setSalePrice(resolvedSalePrice);
        target.setVariableCost(request.variableCost());
        target.setFixedCostProduct(request.fixedCostProduct());
        target.setUnitsSoldCurrent(resolvedUnitsSold);
        LocalDate periodEnd = request.periodEnd() != null
                ? request.periodEnd()
                : (request.deadline() != null ? request.deadline() : inferQuarterEnd(LocalDate.now()));
        LocalDate periodStart = request.periodStart() != null
                ? request.periodStart()
                : inferQuarterStart(periodEnd);
        if (periodEnd.isBefore(periodStart)) {
            throw new IllegalArgumentException("periodEnd must be greater than or equal to periodStart");
        }
        target.setPeriodStart(periodStart);
        target.setPeriodEnd(periodEnd);
        target.setDeadline(request.deadline() != null ? request.deadline() : periodEnd);
        target.setNotes(request.notes());
    }

    private SalesTargetResponse toResponse(SalesTarget target) {
        var metrics = calculatorService.calculate(target);
        return new SalesTargetResponse(
                target.getId(),
                target.getSoftware().getId(),
                target.getSoftware().getName(),
                normalizeCountryCode(target.getCountryCode()),
                target.getCustomerType() != null ? target.getCustomerType() : CustomerType.FINAL,
                target.getMetaUnits(),
                target.getSalePrice(),
                target.getVariableCost(),
                target.getFixedCostProduct(),
                target.getUnitsSoldCurrent(),
                target.getStatus() != null ? target.getStatus() : SalesTargetStatus.ACTIVE,
                target.getPeriodStart(),
                target.getPeriodEnd(),
                target.getClosedAt(),
                target.getDeadline(),
                target.getNotes(),
                metrics.getMarginUnit(),
                metrics.getTargetRevenue(),
                metrics.getVariableCostTotal(),
                metrics.getContributionTotal(),
                metrics.getBreakEvenUnits(),
                metrics.getBreakEvenRevenue(),
                metrics.getExpectedProfit(),
                metrics.isProfitable(),
                metrics.isTargetAchieved(),
                metrics.getFixedCostApplied(),
                target.getCreatedAt(),
                target.getUpdatedAt()
        );
    }

    private void ensureEditable(SalesTarget target) {
        if (target.getStatus() == SalesTargetStatus.CLOSED) {
            throw new IllegalStateException("La meta ya esta cerrada y queda como historial; no se puede editar.");
        }
    }

    private void ensureNoOtherActiveTarget(UUID softwareId, UUID currentTargetId) {
        salesTargetRepository.findBySoftwareIdAndStatus(softwareId, SalesTargetStatus.ACTIVE).stream()
                .findFirst()
                .filter(existing -> currentTargetId == null || !existing.getId().equals(currentTargetId))
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "Ya existe una meta activa para este producto. Cierra la meta anterior antes de crear un nuevo periodo."
                    );
                });
    }

    private LocalDate inferQuarterStart(LocalDate date) {
        int month = date.getMonthValue();
        int quarterStartMonth = ((month - 1) / 3) * 3 + 1;
        return LocalDate.of(date.getYear(), quarterStartMonth, 1);
    }

    private LocalDate inferQuarterEnd(LocalDate date) {
        return inferQuarterStart(date).plusMonths(3).minusDays(1);
    }

    private String normalizeCountryCode(String value) {
        if (value == null || value.isBlank()) {
            return "EC";
        }
        return value.trim().toUpperCase();
    }

    private void requireNonNegative(Integer value, String field) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(field + " must be greater than or equal to 0");
        }
    }

    private void requireNonNegative(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(field + " must be greater than or equal to 0");
        }
    }
}

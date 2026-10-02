package com.dismal.distribuciones.modules.expenses.service;

import com.dismal.distribuciones.exception.ResourceNotFoundException;
import com.dismal.distribuciones.modules.expenses.domain.Expense;
import com.dismal.distribuciones.modules.expenses.dto.ExpenseRequest;
import com.dismal.distribuciones.modules.expenses.dto.ExpenseResponse;
import com.dismal.distribuciones.modules.expenses.dto.ExpenseSummaryResponse;
import com.dismal.distribuciones.modules.expenses.repository.ExpenseRepository;
import com.dismal.distribuciones.modules.sales.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public ExpenseResponse create(ExpenseRequest request) {
        Expense expense = new Expense();
        applyRequest(expense, request);
        return toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseResponse update(UUID id, ExpenseRequest request) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with ID: " + id));
        applyRequest(expense, request);
        return toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public void delete(UUID id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with ID: " + id));
        expenseRepository.delete(expense);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> list() {
        return expenseRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExpenseSummaryResponse getCurrentMonthSummary() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = today.plusMonths(1).withDayOfMonth(1).atStartOfDay();

        BigDecimal payments = paymentRepository.sumAmountBetween(start, end);
        if (payments == null) {
            payments = BigDecimal.ZERO;
        }

        BigDecimal expenses = getCurrentMonthExpensesTotal();

        BigDecimal balance = payments.subtract(expenses);
        String status = balance.signum() >= 0 ? "OK" : "NEGATIVE";
        String recommendation = balance.signum() >= 0
                ? "Vas en buen camino con los pagos del mes."
                : "Necesitas mejorar estrategias de ventas para cubrir gastos.";

        return new ExpenseSummaryResponse(payments, expenses, balance, status, recommendation);
    }

    @Transactional(readOnly = true)
    public BigDecimal getCurrentMonthExpensesTotal() {
        return expenseRepository.findByActiveTrueOrderByDueDayAsc().stream()
                .map(Expense::getMonthlyAmount)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void applyRequest(Expense expense, ExpenseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Expense request is required");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (request.category() == null || request.category().isBlank()) {
            throw new IllegalArgumentException("category is required");
        }
        String type = request.type() == null || request.type().isBlank() ? "DIFERIDO" : request.type().toUpperCase();
        if (!type.equals("MENSUAL") && !type.equals("DIFERIDO")) {
            throw new IllegalArgumentException("type must be MENSUAL or DIFERIDO");
        }
        if (request.totalAmount() == null || request.totalAmount().signum() < 0) {
            throw new IllegalArgumentException("totalAmount must be >= 0");
        }
        Integer months = request.months();
        if ("MENSUAL".equals(type)) {
            months = 1;
        } else if (months == null || months <= 0) {
            throw new IllegalArgumentException("months must be > 0");
        }
        if (request.dueDay() == null || request.dueDay() < 1 || request.dueDay() > 28) {
            throw new IllegalArgumentException("dueDay must be between 1 and 28");
        }
        String priority = request.priority() == null || request.priority().isBlank() ? "MEDIA" : request.priority();
        boolean active = request.active() == null || request.active();

        BigDecimal monthly = "MENSUAL".equals(type)
                ? request.totalAmount()
                : request.totalAmount().divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);

        expense.setName(request.name());
        expense.setCategory(request.category());
        expense.setType(type);
        expense.setTotalAmount(request.totalAmount());
        expense.setMonths(months);
        expense.setMonthlyAmount(monthly);
        expense.setDueDay(request.dueDay());
        expense.setPriority(priority);
        expense.setActive(active);
    }

    private ExpenseResponse toResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getName(),
                expense.getCategory(),
                expense.getType(),
                expense.getTotalAmount(),
                expense.getMonths(),
                expense.getMonthlyAmount(),
                expense.getDueDay(),
                expense.getPriority(),
                expense.isActive(),
                expense.getCreatedAt()
        );
    }
}

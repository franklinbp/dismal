package com.dismal.distribuciones.modules.expenses.repository;

import com.dismal.distribuciones.modules.expenses.domain.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    List<Expense> findByActiveTrueOrderByDueDayAsc();
}

package com.dismal.distribuciones.modules.dashboard.service;

import com.dismal.distribuciones.modules.dashboard.dto.DashboardSummaryResponse;
import com.dismal.distribuciones.modules.integrations.outbox.repository.EventOutboxRepository;
import com.dismal.distribuciones.modules.planning.repository.SalesTargetRepository;
import com.dismal.distribuciones.modules.planning.service.SalesTargetCalculatorService;
import com.dismal.distribuciones.modules.sales.repository.AccountsReceivableRepository;
import com.dismal.distribuciones.modules.sales.repository.PaymentRepository;
import com.dismal.distribuciones.modules.sales.repository.SaleFulfillmentRepository;
import com.dismal.distribuciones.modules.sales.repository.SaleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AccountsReceivableRepository accountsReceivableRepository;

    @Mock
    private SalesTargetRepository salesTargetRepository;

    @Mock
    private SalesTargetCalculatorService calculatorService;

    @Mock
    private EventOutboxRepository eventOutboxRepository;

    @Mock
    private SaleFulfillmentRepository saleFulfillmentRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void summaryDoesNotShareATransactionAcrossOptionalMetrics() throws NoSuchMethodException {
        Transactional transactional = DashboardService.class
                .getMethod("getSummary")
                .getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.NOT_SUPPORTED);
    }

    @Test
    void summaryReturnsAvailableMetricsWhenOneQueryFails() {
        when(saleRepository.sumAndCountBetween(any(), any()))
                .thenThrow(new DataAccessResourceFailureException("metric unavailable"))
                .thenReturn(new Object[] {3L, new BigDecimal("42.50")});
        when(saleFulfillmentRepository.sumProfitBetween(any(), any()))
                .thenReturn(null, new BigDecimal("12.25"));
        when(accountsReceivableRepository.sumAndCountByStatus(any()))
                .thenReturn(new Object[] {0L, BigDecimal.ZERO});
        when(salesTargetRepository.findByDeadlineBetween(any(), any())).thenReturn(List.of());

        DashboardSummaryResponse summary = dashboardService.getSummary();

        assertThat(summary.todaySalesCount()).isZero();
        assertThat(summary.todaySalesAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.todayProfitAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.monthSalesCount()).isEqualTo(3L);
        assertThat(summary.monthSalesAmount()).isEqualByComparingTo("42.50");
        assertThat(summary.monthProfitAmount()).isEqualByComparingTo("12.25");
    }
}

package com.dismal.distribuciones.modules.dashboard.controller;

import com.dismal.distribuciones.modules.integrations.outbox.repository.EventOutboxRepository;
import com.dismal.distribuciones.modules.planning.repository.SalesTargetRepository;
import com.dismal.distribuciones.modules.sales.repository.AccountsReceivableRepository;
import com.dismal.distribuciones.modules.sales.repository.PaymentRepository;
import com.dismal.distribuciones.modules.sales.repository.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AccountsReceivableRepository accountsReceivableRepository;

    @Autowired
    private SalesTargetRepository salesTargetRepository;

    @Autowired
    private EventOutboxRepository eventOutboxRepository;

    @BeforeEach
    void setUp() {
        eventOutboxRepository.deleteAll();
        accountsReceivableRepository.deleteAll();
        paymentRepository.deleteAll();
        saleRepository.deleteAll();
        salesTargetRepository.deleteAll();
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void summaryShouldReturnZerosWhenEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/admin/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todaySalesCount").value(0))
                .andExpect(jsonPath("$.todaySalesAmount").value(0))
                .andExpect(jsonPath("$.monthSalesCount").value(0))
                .andExpect(jsonPath("$.monthSalesAmount").value(0))
                .andExpect(jsonPath("$.overdueArCount").value(0))
                .andExpect(jsonPath("$.overdueArBalance").value(0))
                .andExpect(jsonPath("$.openArCount").value(0))
                .andExpect(jsonPath("$.openArBalance").value(0))
                .andExpect(jsonPath("$.nonProfitableProductsCount").value(0))
                .andExpect(jsonPath("$.outboxFailedCount").value(0));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void recentSalesShouldReturnEmptyPage() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/admin/recent-sales")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void outboxShouldReturnEmptyPage() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/admin/outbox")
                        .param("status", "FAILED")
                        .param("page", "0")
                        .param("size", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}

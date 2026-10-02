package com.dismal.distribuciones.modules.integrations.woocommerce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dismal.distribuciones.modules.integrations.woocommerce.dto.WooOrderPaidRequest;
import com.dismal.distribuciones.modules.integrations.woocommerce.repository.WooOrderSyncRepository;
import com.dismal.distribuciones.modules.sales.domain.PaymentMethod;
import com.dismal.distribuciones.modules.sales.domain.SaleType;
import com.dismal.distribuciones.modules.sales.repository.PaymentRepository;
import com.dismal.distribuciones.modules.sales.repository.SaleRepository;
import com.dismal.distribuciones.modules.security.domain.CustomerType;
import com.dismal.distribuciones.modules.store.domain.License;
import com.dismal.distribuciones.modules.store.domain.Software;
import com.dismal.distribuciones.modules.store.repository.LicenseRepository;
import com.dismal.distribuciones.modules.store.repository.SoftwareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "integrations.woocommerce.api-key=test-key")
class WooCommercePublicControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SoftwareRepository softwareRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private WooOrderSyncRepository wooOrderSyncRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        wooOrderSyncRepository.deleteAll();
        saleRepository.deleteAll();
        licenseRepository.deleteAll();
        softwareRepository.deleteAll();
    }

    @Test
    void paidWooOrderCreatesSaleAndIsIdempotent() throws Exception {
        Software software = softwareRepository.save(Software.builder()
                .name("Windows 11 Pro")
                .price(new BigDecimal("99.00"))
                .build());
        licenseRepository.save(License.builder()
                .software(software)
                .licenseKey("LIC-" + UUID.randomUUID())
                .purchasePrice(new BigDecimal("20.00"))
                .build());

        WooOrderPaidRequest request = new WooOrderPaidRequest(
                "woo-123",
                "123",
                "Ana",
                "Cliente",
                "ana@example.com",
                "0999999999",
                null,
                CustomerType.FINAL,
                SaleType.CASH,
                PaymentMethod.TRANSFER,
                "txn-123",
                new BigDecimal("99.00"),
                List.of(new WooOrderPaidRequest.Item(software.getId(), 1, new BigDecimal("99.00")))
        );

        mockMvc.perform(post("/api/public/integrations/woocommerce/order-paid")
                        .header("X-Dismal-Integration-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalOrderId").value("woo-123"))
                .andExpect(jsonPath("$.duplicated").value(false));

        mockMvc.perform(post("/api/public/integrations/woocommerce/order-paid")
                        .header("X-Dismal-Integration-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicated").value(true));

        assertThat(saleRepository.findAll()).hasSize(1);
        assertThat(paymentRepository.findAll()).hasSize(1);
        assertThat(wooOrderSyncRepository.findAll()).hasSize(1);
    }
}

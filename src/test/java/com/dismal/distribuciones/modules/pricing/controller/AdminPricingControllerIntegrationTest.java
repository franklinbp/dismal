package com.dismal.distribuciones.modules.pricing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dismal.distribuciones.modules.pricing.domain.PriceListType;
import com.dismal.distribuciones.modules.pricing.dto.UpdateProductPriceRequest;
import com.dismal.distribuciones.modules.store.domain.Software;
import com.dismal.distribuciones.modules.store.repository.SoftwareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminPricingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SoftwareRepository softwareRepository;

    @BeforeEach
    void setUp() {
        softwareRepository.deleteAll();
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void adminCanUpdateDistributorPrice() throws Exception {
        Software software = softwareRepository.save(Software.builder()
                .name("Antivirus Pro")
                .price(new BigDecimal("80.00"))
                .build());

        mockMvc.perform(patch("/api/v1/pricing/products/" + software.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateProductPriceRequest(PriceListType.EC_DISTRIBUTOR, new BigDecimal("55.00"))
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ecFinalPrice").value(80.0))
                .andExpect(jsonPath("$.ecDistributorPrice").value(55.0));

        mockMvc.perform(get("/api/v1/pricing/products/" + software.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ecFinalPrice").value(80.0))
                .andExpect(jsonPath("$.ecDistributorPrice").value(55.0));
    }
}

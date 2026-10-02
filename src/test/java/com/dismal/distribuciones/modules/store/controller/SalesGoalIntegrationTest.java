package com.dismal.distribuciones.modules.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dismal.distribuciones.modules.store.domain.SalesGoal;
import com.dismal.distribuciones.modules.store.domain.Software;
import com.dismal.distribuciones.modules.store.repository.SalesGoalRepository;
import com.dismal.distribuciones.modules.store.repository.SoftwareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Roll back transactions after each test
class SalesGoalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SoftwareRepository softwareRepository;

    @Autowired
    private SalesGoalRepository salesGoalRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Software testSoftware;

    @BeforeEach
    void setUp() {
        // Clean up before each test to ensure isolation
        salesGoalRepository.deleteAll();
        softwareRepository.deleteAll();

        // Create a common software product for tests
        testSoftware = Software.builder()
                .name("Test Product")
                .price(new BigDecimal("100.00"))
                .build();
        testSoftware = softwareRepository.save(testSoftware);
    }

    @Test
    @WithMockUser(authorities = "ADMIN") // Simulate an authenticated user, bypassing JWT validation for this test
    void shouldGetGoalsAndVerifyCalculatedFields() throws Exception {
        // Arrange: Create a sales goal to be fetched by the API
        SalesGoal goal = SalesGoal.builder()
                .software(testSoftware)
                .targetUnits(100)
                .variableCost(new BigDecimal("20.00"))
                .fixedCosts(new BigDecimal("1000.00"))
                .deadline(LocalDate.now().plusMonths(6))
                .build();
        salesGoalRepository.save(goal);

        // Manually calculate the expected results for assertion
        BigDecimal contributionMargin = new BigDecimal("100.00").subtract(new BigDecimal("20.00")); // 80.00
        int expectedBreakEvenUnits = new BigDecimal("1000.00").divide(contributionMargin, 0, RoundingMode.CEILING).intValue(); // 1000 / 80 = 12.5 -> 13
        BigDecimal expectedProfit = contributionMargin.multiply(new BigDecimal(100)).subtract(new BigDecimal("1000.00")); // (80 * 100) - 1000 = 7000

        // Act & Assert: Perform the GET request and validate the JSON response
        mockMvc.perform(get("/api/v1/store/goals")
                        .param("softwareId", testSoftware.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(goal.getId().toString()))
                .andExpect(jsonPath("$[0].targetUnits").value(100))
                // Verify the calculated fields are present and correct in the JSON response
                .andExpect(jsonPath("$[0].breakEvenUnits").value(expectedBreakEvenUnits)) // Should be 13
                .andExpect(jsonPath("$[0].expectedProfit").value(expectedProfit.doubleValue())); // Should be 7000.0
    }
}

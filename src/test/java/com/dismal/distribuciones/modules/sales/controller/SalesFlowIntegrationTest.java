package com.dismal.distribuciones.modules.sales.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dismal.distribuciones.modules.integrations.outbox.domain.EventOutbox;
import com.dismal.distribuciones.modules.integrations.outbox.repository.EventOutboxRepository;
import com.dismal.distribuciones.modules.sales.domain.AccountsReceivableStatus;
import com.dismal.distribuciones.modules.sales.domain.PaymentMethod;
import com.dismal.distribuciones.modules.sales.domain.SaleStatus;
import com.dismal.distribuciones.modules.sales.domain.SaleType;
import com.dismal.distribuciones.modules.sales.dto.CreateSaleRequest;
import com.dismal.distribuciones.modules.sales.dto.PaymentRequest;
import com.dismal.distribuciones.modules.sales.dto.SaleItemRequest;
import com.dismal.distribuciones.modules.sales.dto.SaleResponse;
import com.dismal.distribuciones.modules.sales.repository.AccountsReceivableRepository;
import com.dismal.distribuciones.modules.sales.repository.SaleFulfillmentRepository;
import com.dismal.distribuciones.modules.sales.repository.SaleNumberCounterRepository;
import com.dismal.distribuciones.modules.sales.repository.SaleRepository;
import com.dismal.distribuciones.modules.security.domain.CustomerType;
import com.dismal.distribuciones.modules.security.domain.Role;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.repository.UserRepository;
import com.dismal.distribuciones.modules.store.domain.License;
import com.dismal.distribuciones.modules.store.domain.Software;
import com.dismal.distribuciones.modules.store.repository.LicenseRepository;
import com.dismal.distribuciones.modules.store.repository.SoftwareRepository;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SalesFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SoftwareRepository softwareRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private SaleNumberCounterRepository saleNumberCounterRepository;

    @Autowired
    private SaleFulfillmentRepository saleFulfillmentRepository;

    @Autowired
    private AccountsReceivableRepository accountsReceivableRepository;

    @Autowired
    private EventOutboxRepository eventOutboxRepository;

    private int emailCounter = 1;

    @BeforeEach
    void setUp() {
        eventOutboxRepository.deleteAll();
        saleFulfillmentRepository.deleteAll();
        accountsReceivableRepository.deleteAll();
        saleRepository.deleteAll();
        saleNumberCounterRepository.deleteAll();
        licenseRepository.deleteAll();
        softwareRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void cashConfirmAssignsLicense() throws Exception {
        User client = createClient(false, BigDecimal.ZERO, BigDecimal.ZERO, 0);
        Software software = createSoftware(new BigDecimal("100.00"));
        createLicenses(software, 1);

        SaleResponse sale = createSale(client.getId(), SaleType.CASH, software.getId(), new BigDecimal("100.00"), 1);
        SaleResponse confirmed = confirmSale(sale.id());

        assertThat(confirmed.status()).isEqualTo(SaleStatus.CONFIRMED);
        License license = licenseRepository.findAll().get(0);
        assertThat(license.getOwner()).isNotNull();
        assertThat(license.getOwner().getId()).isEqualTo(client.getId());
        assertThat(saleFulfillmentRepository.existsBySaleId(sale.id())).isTrue();
        var ar = accountsReceivableRepository.findBySaleId(sale.id());
        assertThat(ar).isPresent();
        assertThat(ar.get().getStatus()).isEqualTo(AccountsReceivableStatus.OPEN);
        assertThat(ar.get().getBalance()).isEqualByComparingTo("100.0000");

        List<EventOutbox> events = eventOutboxRepository.findAll();
        assertThat(events).noneMatch(event -> "SALE_CONFIRMED".equals(event.getEventType()));
        assertThat(events).anyMatch(event -> "LICENSES_DELIVERED".equals(event.getEventType()));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void confirmingAlreadyConfirmedSaleDoesNotDuplicateLicensesOrOutbox() throws Exception {
        User client = createClient(false, BigDecimal.ZERO, BigDecimal.ZERO, 0);
        Software software = createSoftware(new BigDecimal("100.00"));
        createLicenses(software, 1);

        SaleResponse sale = createSale(client.getId(), SaleType.CASH, software.getId(), new BigDecimal("100.00"), 1);
        confirmSale(sale.id());
        confirmSale(sale.id());

        assertThat(saleFulfillmentRepository.findBySaleIdWithLicense(sale.id())).hasSize(1);
        List<EventOutbox> events = eventOutboxRepository.findAll();
        assertThat(events).filteredOn(event -> "LICENSES_DELIVERED".equals(event.getEventType())).hasSize(1);
        assertThat(events).noneMatch(event -> "SALE_CONFIRMED".equals(event.getEventType()));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void cashPaymentAssignsLicenseAndCreatesOutbox() throws Exception {
        User client = createClient(false, BigDecimal.ZERO, BigDecimal.ZERO, 0);
        Software software = createSoftware(new BigDecimal("120.00"));
        createLicenses(software, 1);

        SaleResponse sale = createSale(client.getId(), SaleType.CASH, software.getId(), new BigDecimal("120.00"), 1);
        confirmSale(sale.id());
        addPayment(sale.id(), new BigDecimal("120.00"));

        License license = licenseRepository.findAll().get(0);
        assertThat(license.getOwner()).isNotNull();
        assertThat(license.getOwner().getId()).isEqualTo(client.getId());
        assertThat(saleFulfillmentRepository.existsBySaleId(sale.id())).isTrue();
        var ar = accountsReceivableRepository.findBySaleId(sale.id());
        assertThat(ar).isPresent();
        assertThat(ar.get().getStatus()).isEqualTo(AccountsReceivableStatus.PAID);
        assertThat(ar.get().getBalance()).isEqualByComparingTo("0.0000");

        List<EventOutbox> events = eventOutboxRepository.findAll();
        assertThat(events).anyMatch(event -> "PAYMENT_RECEIVED".equals(event.getEventType()));
        assertThat(events).anyMatch(event -> "LICENSES_DELIVERED".equals(event.getEventType()));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void creditConfirmAssignsLicenseAndCreatesAr() throws Exception {
        User client = createClient(true, new BigDecimal("500.00"), BigDecimal.ZERO, 15);
        Software software = createSoftware(new BigDecimal("80.00"));
        createLicenses(software, 1);

        SaleResponse sale = createSale(client.getId(), SaleType.CREDIT, software.getId(), new BigDecimal("80.00"), 1);
        SaleResponse confirmed = confirmSale(sale.id());

        assertThat(confirmed.status()).isEqualTo(SaleStatus.CONFIRMED);
        License license = licenseRepository.findAll().get(0);
        assertThat(license.getOwner()).isNotNull();
        assertThat(license.getOwner().getId()).isEqualTo(client.getId());
        assertThat(saleFulfillmentRepository.existsBySaleId(sale.id())).isTrue();

        var ar = accountsReceivableRepository.findBySaleId(sale.id());
        assertThat(ar).isPresent();
        assertThat(ar.get().getStatus()).isEqualTo(AccountsReceivableStatus.OPEN);

        List<EventOutbox> events = eventOutboxRepository.findAll();
        assertThat(events).noneMatch(event -> "SALE_CONFIRMED".equals(event.getEventType()));
        assertThat(events).anyMatch(event -> "LICENSES_DELIVERED".equals(event.getEventType()));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void paidSaleConsumesMultipleActivationsFromSameLicense() throws Exception {
        User client = createClient(false, BigDecimal.ZERO, BigDecimal.ZERO, 0);
        Software software = createSoftware(new BigDecimal("60.00"));
        License license = License.builder()
                .software(software)
                .licenseKey("LIC-" + UUID.randomUUID())
                .purchasePrice(new BigDecimal("15.00"))
                .maxActivations(25)
                .usedActivations(0)
                .build();
        licenseRepository.save(license);

        SaleResponse sale = createSale(client.getId(), SaleType.CASH, software.getId(), new BigDecimal("60.00"), 3);
        confirmSale(sale.id());
        addPayment(sale.id(), new BigDecimal("60.00"));

        License updated = licenseRepository.findAll().get(0);
        assertThat(updated.getUsedActivations()).isEqualTo(3);
        assertThat(saleFulfillmentRepository.findBySaleIdWithLicense(sale.id())).hasSize(3);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void creditConfirmFailsWhenLimitInsufficient() throws Exception {
        User client = createClient(true, new BigDecimal("50.00"), BigDecimal.ZERO, 10);
        Software software = createSoftware(new BigDecimal("120.00"));
        createLicenses(software, 1);

        SaleResponse sale = createSale(client.getId(), SaleType.CREDIT, software.getId(), new BigDecimal("120.00"), 1);

        mockMvc.perform(post("/api/v1/sales/{id}/confirm", sale.id()))
                .andExpect(status().isBadRequest());

        var persisted = saleRepository.findById(sale.id());
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getStatus()).isEqualTo(SaleStatus.DRAFT);
        assertThat(saleFulfillmentRepository.existsBySaleId(sale.id())).isFalse();
        assertThat(eventOutboxRepository.findAll()).isEmpty();
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void distributorCustomerTypeCanUseCurrentSalesFlow() throws Exception {
        User client = createClient(false, BigDecimal.ZERO, BigDecimal.ZERO, 0);
        client.setCustomerType(CustomerType.DISTRIBUTOR);
        userRepository.save(client);

        Software software = createSoftware(new BigDecimal("90.00"));
        createLicenses(software, 1);

        SaleResponse sale = createSale(client.getId(), SaleType.CASH, software.getId(), new BigDecimal("90.00"), 1);
        SaleResponse confirmed = confirmSale(sale.id());

        assertThat(confirmed.status()).isEqualTo(SaleStatus.CONFIRMED);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void distributorSaleWithoutExplicitUnitPriceFallsBackToConfiguredPricing() throws Exception {
        User client = createClient(false, BigDecimal.ZERO, BigDecimal.ZERO, 0);
        client.setCustomerType(CustomerType.DISTRIBUTOR);
        userRepository.save(client);

        Software software = createSoftware(new BigDecimal("150.00"));
        createLicenses(software, 1);

        SaleResponse sale = createSale(client.getId(), SaleType.CASH, software.getId(), null, 1);

        assertThat(sale.total()).isEqualByComparingTo("150.0000");
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void confirmedSaleNumbersAreSequentialAndIndependentPerCountry() throws Exception {
        User client = createClient(false, BigDecimal.ZERO, BigDecimal.ZERO, 0);
        Software software = createSoftware(new BigDecimal("25.00"));
        createLicenses(software, 4);

        SaleResponse ecDraft = createSale(
                client.getId(), SaleType.CASH, StorefrontCountry.EC,
                software.getId(), new BigDecimal("25.00"), 1
        );
        assertThat(ecDraft.saleNumber()).isNull();
        SaleResponse ecFirst = confirmSale(ecDraft.id());
        SaleResponse ecSecond = confirmSale(createSale(
                client.getId(), SaleType.CASH, StorefrontCountry.EC,
                software.getId(), new BigDecimal("25.00"), 1
        ).id());
        SaleResponse peFirst = confirmSale(createSale(
                client.getId(), SaleType.CASH, StorefrontCountry.PE,
                software.getId(), new BigDecimal("25.00"), 1
        ).id());
        SaleResponse peSecond = confirmSale(createSale(
                client.getId(), SaleType.CASH, StorefrontCountry.PE,
                software.getId(), new BigDecimal("25.00"), 1
        ).id());

        assertThat(ecFirst.saleNumber()).isEqualTo("EC-10000");
        assertThat(ecSecond.saleNumber()).isEqualTo("EC-10001");
        assertThat(peFirst.saleNumber()).isEqualTo("PE-20000");
        assertThat(peSecond.saleNumber()).isEqualTo("PE-20001");
        assertThat(eventOutboxRepository.findAll())
                .allMatch(event -> event.getPayloadJson().contains("\"saleNumber\""));
    }

    private User createClient(boolean hasCredit, BigDecimal creditLimit, BigDecimal creditUsed, int creditDays) {
        User client = User.builder()
                .firstname("Client")
                .lastname("Test")
                .email("client" + emailCounter++ + "@example.com")
                .password("password")
                .role(Role.USER)
                .customerType(CustomerType.FINAL)
                .hasCredit(hasCredit)
                .creditLimit(creditLimit)
                .creditUsed(creditUsed)
                .creditDays(creditDays)
                .build();
        return userRepository.save(client);
    }

    private Software createSoftware(BigDecimal price) {
        Software software = Software.builder()
                .name("Software " + UUID.randomUUID())
                .price(price)
                .build();
        return softwareRepository.save(software);
    }

    private void createLicenses(Software software, int count) {
        for (int i = 0; i < count; i++) {
            License license = License.builder()
                    .software(software)
                    .licenseKey("LIC-" + UUID.randomUUID())
                    .purchasePrice(new BigDecimal("30.00"))
                    .build();
            licenseRepository.save(license);
        }
    }

    private SaleResponse createSale(UUID clientId, SaleType saleType, UUID softwareId, BigDecimal unitPrice, int quantity) throws Exception {
        return createSale(clientId, saleType, null, softwareId, unitPrice, quantity);
    }

    private SaleResponse createSale(
            UUID clientId,
            SaleType saleType,
            StorefrontCountry country,
            UUID softwareId,
            BigDecimal unitPrice,
            int quantity
    ) throws Exception {
        CreateSaleRequest request = new CreateSaleRequest(
                clientId,
                saleType,
                country,
                List.of(new SaleItemRequest(softwareId, quantity, unitPrice, null))
        );

        MvcResult result = mockMvc.perform(post("/api/v1/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        if (result.getResolvedException() != null) {
            throw result.getResolvedException();
        }
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return objectMapper.readValue(result.getResponse().getContentAsString(), SaleResponse.class);
    }

    private SaleResponse confirmSale(UUID saleId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/sales/{id}/confirm", saleId))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), SaleResponse.class);
    }

    private SaleResponse addPayment(UUID saleId, BigDecimal amount) throws Exception {
        PaymentRequest request = new PaymentRequest(saleId, amount, PaymentMethod.CASH, "test");
        MvcResult result = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), SaleResponse.class);
    }
}

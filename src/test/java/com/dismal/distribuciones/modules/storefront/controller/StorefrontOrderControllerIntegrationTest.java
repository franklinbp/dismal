package com.dismal.distribuciones.modules.storefront.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dismal.distribuciones.modules.pricing.domain.PriceListType;
import com.dismal.distribuciones.modules.sales.domain.AccountsReceivableStatus;
import com.dismal.distribuciones.modules.sales.domain.PaymentAccount;
import com.dismal.distribuciones.modules.sales.domain.PaymentAccountType;
import com.dismal.distribuciones.modules.sales.domain.PaymentMethod;
import com.dismal.distribuciones.modules.sales.domain.SaleStatus;
import com.dismal.distribuciones.modules.sales.repository.AccountsReceivableRepository;
import com.dismal.distribuciones.modules.sales.repository.PaymentAccountRepository;
import com.dismal.distribuciones.modules.sales.repository.PaymentRepository;
import com.dismal.distribuciones.modules.pricing.service.PricingService;
import com.dismal.distribuciones.modules.sales.repository.SaleFulfillmentRepository;
import com.dismal.distribuciones.modules.sales.repository.SaleRepository;
import com.dismal.distribuciones.modules.security.domain.CustomerType;
import com.dismal.distribuciones.modules.security.domain.Role;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.customer.domain.CustomerMarket;
import com.dismal.distribuciones.modules.security.customer.repository.CustomerMarketRepository;
import com.dismal.distribuciones.modules.security.customer.service.CustomerMarketService;
import com.dismal.distribuciones.modules.security.repository.UserRepository;
import com.dismal.distribuciones.modules.store.domain.License;
import com.dismal.distribuciones.modules.store.domain.Software;
import com.dismal.distribuciones.modules.store.repository.LicenseRepository;
import com.dismal.distribuciones.modules.store.repository.SoftwareRepository;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontOrderStatus;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCheckoutMethod;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;
import com.dismal.distribuciones.modules.storefront.dto.StorefrontCustomerRequest;
import com.dismal.distribuciones.modules.storefront.dto.StorefrontOrderItemRequest;
import com.dismal.distribuciones.modules.storefront.dto.StorefrontOrderRequest;
import com.dismal.distribuciones.modules.storefront.repository.StorefrontOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StorefrontOrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SoftwareRepository softwareRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StorefrontOrderRepository storefrontOrderRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private SaleFulfillmentRepository saleFulfillmentRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentAccountRepository paymentAccountRepository;

    @Autowired
    private AccountsReceivableRepository accountsReceivableRepository;

    @Autowired
    private PricingService pricingService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CustomerMarketRepository customerMarketRepository;

    @Autowired
    private CustomerMarketService customerMarketService;

    @BeforeEach
    void setUp() {
        saleFulfillmentRepository.deleteAll();
        paymentRepository.deleteAll();
        accountsReceivableRepository.deleteAll();
        saleRepository.deleteAll();
        storefrontOrderRepository.deleteAll();
        paymentAccountRepository.deleteAll();
        licenseRepository.deleteAll();
        softwareRepository.deleteAll();
        customerMarketRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createsPendingOrderWithoutCreatingSaleOrAssigningLicense() throws Exception {
        Software software = softwareRepository.save(Software.builder()
                .name("Windows 11 Pro")
                .price(new BigDecimal("99.00"))
                .stockQuantity(100)
                .reservedQuantity(0)
                .platform("Windows")
                .build());
        License license = licenseRepository.save(License.builder()
                .software(software)
                .licenseKey("LIC-" + UUID.randomUUID())
                .purchasePrice(new BigDecimal("20.00"))
                .build());

        pricingService.upsertPrice(software.getId(), PriceListType.PE_FINAL, new BigDecimal("149.90"));

        StorefrontOrderRequest request = new StorefrontOrderRequest(
                "PE",
                "final",
                StorefrontCheckoutMethod.BANK_TRANSFER,
                new StorefrontCustomerRequest("Ana", "Cliente", "ana@example.com", "+51999999999", null, "CompraSegura123"),
                List.of(new StorefrontOrderItemRequest(software.getId(), 2))
        );

        mockMvc.perform(post("/api/public/storefront/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(StorefrontOrderStatus.PENDING_PAYMENT.name()))
                .andExpect(jsonPath("$.country").value("PE"))
                .andExpect(jsonPath("$.currency").value("PEN"))
                .andExpect(jsonPath("$.customerType").value(CustomerType.FINAL.name()))
                .andExpect(jsonPath("$.total").value(299.80));

        assertThat(storefrontOrderRepository.findAll()).hasSize(1);
        assertThat(userRepository.findByEmail("ana@example.com")).isPresent();
        assertThat(saleRepository.findAll()).isEmpty();
        assertThat(saleFulfillmentRepository.findAll()).isEmpty();
        assertThat(licenseRepository.findById(license.getId()).orElseThrow().getOwner()).isNull();
    }

    @Test
    void downgradesAnonymousDistributorRequestToFinalPrice() throws Exception {
        Software software = softwareRepository.save(Software.builder()
                .name("Microsoft 365")
                .price(new BigDecimal("100.00"))
                .stockQuantity(100)
                .reservedQuantity(0)
                .platform("Office")
                .build());
        pricingService.upsertPrice(software.getId(), PriceListType.EC_FINAL, new BigDecimal("100.00"));
        pricingService.upsertPrice(software.getId(), PriceListType.EC_DISTRIBUTOR, new BigDecimal("70.00"));

        StorefrontOrderRequest request = new StorefrontOrderRequest(
                "EC",
                "wholesale",
                StorefrontCheckoutMethod.BANK_TRANSFER,
                new StorefrontCustomerRequest("Nuevo", "Cliente", "nuevo@example.com", "+593999999999", null, "CompraSegura123"),
                List.of(new StorefrontOrderItemRequest(software.getId(), 1))
        );

        mockMvc.perform(post("/api/public/storefront/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerType").value(CustomerType.FINAL.name()))
                .andExpect(jsonPath("$.total").value(100.00));
    }

    @Test
    @WithMockUser(username = "dist@example.com", authorities = "USER")
    void usesDistributorPriceForApprovedDistributorCustomer() throws Exception {
        Software software = softwareRepository.save(Software.builder()
                .name("Kaspersky Standard")
                .price(new BigDecimal("50.00"))
                .stockQuantity(100)
                .reservedQuantity(0)
                .platform("Antivirus")
                .build());
        pricingService.upsertPrice(software.getId(), PriceListType.EC_FINAL, new BigDecimal("50.00"));
        pricingService.upsertPrice(software.getId(), PriceListType.EC_DISTRIBUTOR, new BigDecimal("35.00"));

        userRepository.save(User.builder()
                .firstname("Distribuidor")
                .lastname("Aprobado")
                .email("dist@example.com")
                .password(passwordEncoder.encode("password"))
                .role(Role.USER)
                .customerType(CustomerType.DISTRIBUTOR)
                .enabled(true)
                .build());

        StorefrontOrderRequest request = new StorefrontOrderRequest(
                "EC",
                "wholesale",
                StorefrontCheckoutMethod.BANK_TRANSFER,
                new StorefrontCustomerRequest("Distribuidor", "Aprobado", "dist@example.com", "+593999999999", null, null),
                List.of(new StorefrontOrderItemRequest(software.getId(), 1))
        );

        mockMvc.perform(post("/api/public/storefront/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerType").value(CustomerType.DISTRIBUTOR.name()))
                .andExpect(jsonPath("$.total").value(35.00));
    }

    @Test
    @WithMockUser(username = "admin@example.com", authorities = "ADMIN")
    void confirmPaymentCreatesPaidSaleAndAssignsLicense() throws Exception {
        Software software = softwareRepository.save(Software.builder()
                .name("Windows 11 Home")
                .price(new BigDecimal("80.00"))
                .stockQuantity(100)
                .reservedQuantity(0)
                .platform("Windows")
                .build());
        License license = licenseRepository.save(License.builder()
                .software(software)
                .licenseKey("LIC-" + UUID.randomUUID())
                .purchasePrice(new BigDecimal("10.00"))
                .build());
        pricingService.upsertPrice(software.getId(), PriceListType.EC_FINAL, new BigDecimal("80.00"));

        StorefrontOrderRequest orderRequest = new StorefrontOrderRequest(
                "EC",
                "final",
                StorefrontCheckoutMethod.BANK_TRANSFER,
                new StorefrontCustomerRequest("Pago", "Confirmado", "pago@example.com", "+593999999999", null, "CompraSegura123"),
                List.of(new StorefrontOrderItemRequest(software.getId(), 1))
        );

        mockMvc.perform(post("/api/public/storefront/orders")
                        .with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(StorefrontOrderStatus.PENDING_PAYMENT.name()));

        String orderNumber = storefrontOrderRepository.findAll().get(0).getOrderNumber();

        mockMvc.perform(post("/api/v1/storefront/orders/" + orderNumber + "/confirm-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "provider", "manual",
                                "reference", "TEST-" + UUID.randomUUID(),
                                "method", PaymentMethod.TRANSFER.name()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(StorefrontOrderStatus.PREPARING.name()));

        var savedOrder = storefrontOrderRepository.findByOrderNumber(orderNumber).orElseThrow();
        assertThat(savedOrder.getSaleId()).isNotNull();
        assertThat(saleRepository.findById(savedOrder.getSaleId()).orElseThrow().getStatus()).isEqualTo(SaleStatus.PAID);
        assertThat(paymentRepository.findBySaleIdOrderByCreatedAtAsc(savedOrder.getSaleId())).hasSize(1);
        assertThat(saleFulfillmentRepository.findBySaleIdWithLicense(savedOrder.getSaleId())).hasSize(1);
        assertThat(licenseRepository.findById(license.getId()).orElseThrow().getOwner()).isNotNull();

        mockMvc.perform(get("/api/v1/storefront/account/orders")
                        .with(user("pago@example.com").authorities(() -> "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].order.orderNumber").value(orderNumber))
                .andExpect(jsonPath("$.content[0].licenses").isEmpty());

        User customer = userRepository.findByEmail("pago@example.com").orElseThrow();
        customer.setEmailVerified(true);
        userRepository.saveAndFlush(customer);

        mockMvc.perform(get("/api/v1/storefront/account/orders")
                        .with(user("pago@example.com").authorities(() -> "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].licenses[0].licenseKey").isNotEmpty());
    }

    @Test
    void approvedCreditCustomerCompletesOrderWithoutBankPayment() throws Exception {
        Software software = softwareRepository.save(Software.builder()
                .name("Microsoft 365 Empresa")
                .price(new BigDecimal("75.00"))
                .stockQuantity(100)
                .reservedQuantity(0)
                .platform("Office")
                .build());
        License license = licenseRepository.save(License.builder()
                .software(software)
                .licenseKey("LIC-" + UUID.randomUUID())
                .purchasePrice(new BigDecimal("18.00"))
                .build());
        pricingService.upsertPrice(software.getId(), PriceListType.EC_FINAL, new BigDecimal("75.00"));

        userRepository.save(User.builder()
                .firstname("Cliente")
                .lastname("Credito")
                .email("credito@example.com")
                .password(passwordEncoder.encode("password"))
                .role(Role.USER)
                .customerType(CustomerType.FINAL)
                .enabled(true)
                .emailVerified(true)
                .hasCredit(true)
                .creditLimit(new BigDecimal("200.00"))
                .creditUsed(new BigDecimal("25.00"))
                .creditDays(15)
                .build());

        StorefrontOrderRequest request = new StorefrontOrderRequest(
                "EC",
                "final",
                StorefrontCheckoutMethod.CUSTOMER_CREDIT,
                new StorefrontCustomerRequest("Cliente", "Credito", "credito@example.com", "+593999999999", null, null),
                List.of(new StorefrontOrderItemRequest(software.getId(), 1))
        );

        mockMvc.perform(post("/api/public/storefront/orders")
                        .with(user("credito@example.com").authorities(() -> "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(StorefrontOrderStatus.PREPARING.name()))
                .andExpect(jsonPath("$.checkoutMethod").value(StorefrontCheckoutMethod.CUSTOMER_CREDIT.name()))
                .andExpect(jsonPath("$.paymentRequired").value(false));

        var order = storefrontOrderRepository.findAll().get(0);
        assertThat(order.getSaleId()).isNotNull();
        assertThat(saleRepository.findById(order.getSaleId()).orElseThrow().getStatus()).isEqualTo(SaleStatus.CONFIRMED);
        assertThat(paymentRepository.findBySaleIdOrderByCreatedAtAsc(order.getSaleId())).isEmpty();
        var creditReceivable = accountsReceivableRepository.findBySaleId(order.getSaleId()).orElseThrow();
        assertThat(creditReceivable.getStatus()).isEqualTo(AccountsReceivableStatus.OPEN);
        assertThat(creditReceivable.getBalance()).isEqualByComparingTo("75.00");
        assertThat(userRepository.findByEmail("credito@example.com").orElseThrow().getCreditUsed())
                .isEqualByComparingTo("100.00");
        assertThat(licenseRepository.findById(license.getId()).orElseThrow().getOwner()).isNotNull();
    }

    @Test
    void customerClaimAndAdminApprovalCreateOnePaidSale() throws Exception {
        Software software = softwareRepository.save(Software.builder()
                .name("Windows 11 Transferencia")
                .price(new BigDecimal("90.00"))
                .stockQuantity(100)
                .reservedQuantity(0)
                .platform("Windows")
                .build());
        License license = licenseRepository.save(License.builder()
                .software(software)
                .licenseKey("LIC-" + UUID.randomUUID())
                .purchasePrice(new BigDecimal("20.00"))
                .build());
        pricingService.upsertPrice(software.getId(), PriceListType.EC_FINAL, new BigDecimal("90.00"));

        PaymentAccount account = paymentAccountRepository.save(PaymentAccount.builder()
                .name("Banco Ecuador web")
                .type(PaymentAccountType.BANK)
                .currency("USD")
                .active(true)
                .defaultAccount(false)
                .countryCode("EC")
                .publicForStorefront(true)
                .bankName("Banco Ecuador")
                .accountHolder("Dismal")
                .accountNumber("1234567890")
                .accountType("Ahorros")
                .build());

        StorefrontOrderRequest orderRequest = new StorefrontOrderRequest(
                "EC",
                "final",
                StorefrontCheckoutMethod.BANK_TRANSFER,
                new StorefrontCustomerRequest("Pago", "Web", "transfer@example.com", "+593999999999", null, "CompraSegura123"),
                List.of(new StorefrontOrderItemRequest(software.getId(), 1))
        );

        mockMvc.perform(post("/api/public/storefront/orders")
                        .with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(StorefrontOrderStatus.PENDING_PAYMENT.name()));

        String orderNumber = storefrontOrderRepository.findAll().get(0).getOrderNumber();
        mockMvc.perform(post("/api/v1/storefront/account/orders/" + orderNumber + "/payment-claim")
                        .with(user("transfer@example.com").authorities(() -> "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "paymentAccountId", account.getId(),
                                "payerName", "Pago Web",
                                "sourceBank", "Banco Origen",
                                "reference", "trx-001",
                                "amount", new BigDecimal("90.00")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(StorefrontOrderStatus.PAYMENT_REVIEW.name()))
                .andExpect(jsonPath("$.paymentClaimReference").value("TRX-001"));

        var approval = java.util.Map.of(
                "approved", true,
                "paymentAccountId", account.getId(),
                "reviewNotes", "Ingreso confirmado en banca web",
                "notifyClient", false
        );
        mockMvc.perform(post("/api/v1/storefront/orders/" + orderNumber + "/review-payment")
                        .with(user("admin@example.com").authorities(() -> "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approval)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(StorefrontOrderStatus.PREPARING.name()));

        var savedOrder = storefrontOrderRepository.findByOrderNumber(orderNumber).orElseThrow();
        assertThat(saleRepository.findById(savedOrder.getSaleId()).orElseThrow().getStatus()).isEqualTo(SaleStatus.PAID);
        assertThat(paymentRepository.findBySaleIdOrderByCreatedAtAsc(savedOrder.getSaleId())).hasSize(1);
        assertThat(accountsReceivableRepository.findBySaleId(savedOrder.getSaleId()).orElseThrow().getStatus())
                .isEqualTo(AccountsReceivableStatus.PAID);
        assertThat(licenseRepository.findById(license.getId()).orElseThrow().getOwner()).isNotNull();

        mockMvc.perform(post("/api/v1/storefront/orders/" + orderNumber + "/review-payment")
                        .with(user("admin@example.com").authorities(() -> "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approval)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(StorefrontOrderStatus.PREPARING.name()));
        assertThat(paymentRepository.findBySaleIdOrderByCreatedAtAsc(savedOrder.getSaleId())).hasSize(1);
    }

    @Test
    void customerCanCancelOwnPendingOrderWithoutDeletingHistory() throws Exception {
        Software software = softwareRepository.save(Software.builder()
                .name("Windows 11 Cancelable")
                .price(new BigDecimal("45.00"))
                .stockQuantity(100)
                .reservedQuantity(0)
                .platform("Windows")
                .build());
        pricingService.upsertPrice(software.getId(), PriceListType.EC_FINAL, new BigDecimal("45.00"));

        StorefrontOrderRequest request = new StorefrontOrderRequest(
                "EC",
                "final",
                StorefrontCheckoutMethod.BANK_TRANSFER,
                new StorefrontCustomerRequest(
                        "Cliente",
                        "Cancelable",
                        "cancelar@example.com",
                        "+593999999999",
                        null,
                        "CompraSegura123"
                ),
                List.of(new StorefrontOrderItemRequest(software.getId(), 1))
        );

        mockMvc.perform(post("/api/public/storefront/orders")
                        .with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(StorefrontOrderStatus.PENDING_PAYMENT.name()));

        String orderNumber = storefrontOrderRepository.findAll().get(0).getOrderNumber();
        mockMvc.perform(post("/api/v1/storefront/account/orders/" + orderNumber + "/cancel")
                        .with(user("cancelar@example.com").authorities(() -> "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(StorefrontOrderStatus.CANCELLED.name()))
                .andExpect(jsonPath("$.paymentRequired").value(false));

        var cancelled = storefrontOrderRepository.findByOrderNumber(orderNumber).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(StorefrontOrderStatus.CANCELLED);
        assertThat(cancelled.getItems()).hasSize(1);
        assertThat(cancelled.getSaleId()).isNull();

        mockMvc.perform(post("/api/v1/storefront/account/orders/" + orderNumber + "/cancel")
                        .with(user("cancelar@example.com").authorities(() -> "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(StorefrontOrderStatus.CANCELLED.name()));
    }

    @Test
    void customerProfileReturnsUpdatedBillingData() throws Exception {
        userRepository.save(User.builder()
                .firstname("Cliente")
                .lastname("Perfil")
                .email("perfil@example.com")
                .billingEmail("anterior@example.com")
                .password(passwordEncoder.encode("password"))
                .role(Role.USER)
                .customerType(CustomerType.FINAL)
                .enabled(true)
                .build());

        mockMvc.perform(put("/api/v1/account/profile")
                        .with(user("perfil@example.com").authorities(() -> "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "firstName", "Cliente",
                                "lastName", "Actualizado",
                                "phone", "+593999999999",
                                "taxId", "0102030405",
                                "billingEmail", "facturacion@example.com"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Actualizado"))
                .andExpect(jsonPath("$.taxId").value("0102030405"))
                .andExpect(jsonPath("$.billingEmail").value("facturacion@example.com"));

        assertThat(userRepository.findByEmail("perfil@example.com").orElseThrow().getBillingEmail())
                .isEqualTo("facturacion@example.com");
    }

    @Test
    void isolatesDistributorPricingAndCreditBetweenEcuadorAndPeru() throws Exception {
        Software software = softwareRepository.save(Software.builder()
                .name("Microsoft 365 Multimercado")
                .price(new BigDecimal("100.00"))
                .stockQuantity(100)
                .reservedQuantity(0)
                .platform("Office")
                .build());
        licenseRepository.save(License.builder()
                .software(software)
                .licenseKey("LIC-" + UUID.randomUUID())
                .purchasePrice(new BigDecimal("15.00"))
                .build());
        pricingService.upsertPrice(software.getId(), PriceListType.EC_FINAL, new BigDecimal("50.00"));
        pricingService.upsertPrice(software.getId(), PriceListType.EC_DISTRIBUTOR, new BigDecimal("30.00"));
        pricingService.upsertPrice(software.getId(), PriceListType.PE_FINAL, new BigDecimal("140.00"));
        pricingService.upsertPrice(software.getId(), PriceListType.PE_DISTRIBUTOR, new BigDecimal("95.00"));

        User customer = userRepository.save(User.builder()
                .firstname("Cliente")
                .lastname("Dos Paises")
                .email("mercados@example.com")
                .password(passwordEncoder.encode("password"))
                .role(Role.USER)
                .customerType(CustomerType.FINAL)
                .enabled(true)
                .emailVerified(true)
                .build());

        CustomerMarket ecuador = customerMarketService.updateCommercialProfile(
                customer,
                StorefrontCountry.EC,
                CustomerType.DISTRIBUTOR,
                "1790012345001",
                customer.getEmail(),
                true,
                new BigDecimal("200.00"),
                15,
                true
        );
        CustomerMarket peru = customerMarketService.updateCommercialProfile(
                customer,
                StorefrontCountry.PE,
                CustomerType.FINAL,
                "20123456789",
                customer.getEmail(),
                false,
                BigDecimal.ZERO,
                0,
                true
        );

        StorefrontOrderRequest ecuadorRequest = new StorefrontOrderRequest(
                "EC",
                "distributor",
                StorefrontCheckoutMethod.CUSTOMER_CREDIT,
                new StorefrontCustomerRequest(
                        "Cliente", "Dos Paises", customer.getEmail(), "+593999999999", null, null
                ),
                List.of(new StorefrontOrderItemRequest(software.getId(), 1))
        );
        mockMvc.perform(post("/api/public/storefront/orders")
                        .with(user(customer.getEmail()).authorities(() -> "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ecuadorRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.country").value("EC"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.customerType").value("DISTRIBUTOR"))
                .andExpect(jsonPath("$.total").value(30.00));

        StorefrontOrderRequest peruCreditRequest = new StorefrontOrderRequest(
                "PE",
                "distributor",
                StorefrontCheckoutMethod.CUSTOMER_CREDIT,
                new StorefrontCustomerRequest(
                        "Cliente", "Dos Paises", customer.getEmail(), "+51999999999", null, null
                ),
                List.of(new StorefrontOrderItemRequest(software.getId(), 1))
        );
        mockMvc.perform(post("/api/public/storefront/orders")
                        .with(user(customer.getEmail()).authorities(() -> "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(peruCreditRequest)))
                .andExpect(status().isBadRequest());

        StorefrontOrderRequest peruTransferRequest = new StorefrontOrderRequest(
                "PE",
                "distributor",
                StorefrontCheckoutMethod.BANK_TRANSFER,
                peruCreditRequest.customer(),
                peruCreditRequest.items()
        );
        mockMvc.perform(post("/api/public/storefront/orders")
                        .with(user(customer.getEmail()).authorities(() -> "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(peruTransferRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.country").value("PE"))
                .andExpect(jsonPath("$.currency").value("PEN"))
                .andExpect(jsonPath("$.customerType").value("FINAL"))
                .andExpect(jsonPath("$.total").value(140.00));

        CustomerMarket updatedEcuador = customerMarketRepository.findById(ecuador.getId()).orElseThrow();
        CustomerMarket updatedPeru = customerMarketRepository.findById(peru.getId()).orElseThrow();
        assertThat(updatedEcuador.getCreditUsed()).isEqualByComparingTo("30.00");
        assertThat(updatedPeru.getCreditUsed()).isEqualByComparingTo("0.00");

        mockMvc.perform(get("/api/v1/users/me")
                        .param("country", "EC")
                        .with(user(customer.getEmail()).authorities(() -> "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("EC"))
                .andExpect(jsonPath("$.customerType").value("DISTRIBUTOR"))
                .andExpect(jsonPath("$.creditUsed").value(30.00));

        mockMvc.perform(get("/api/v1/users/me")
                        .param("country", "PE")
                        .with(user(customer.getEmail()).authorities(() -> "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("PE"))
                .andExpect(jsonPath("$.customerType").value("FINAL"))
                .andExpect(jsonPath("$.hasCredit").value(false))
                .andExpect(jsonPath("$.creditUsed").value(0.00));

        mockMvc.perform(get("/api/v1/storefront/account/orders")
                        .param("country", "PE")
                        .with(user(customer.getEmail()).authorities(() -> "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].order.country").value("PE"));
    }

    @Test
    void legacyDirectPurchaseEndpointCannotDeliverLicensesWithoutPayment() throws Exception {
        mockMvc.perform(post("/api/v1/sales/purchase")
                        .with(user("client@example.com").authorities(() -> "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "softwareId", UUID.randomUUID()
                        ))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/sales/purchase")
                        .with(user("admin@example.com").authorities(() -> "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "softwareId", UUID.randomUUID()
                        ))))
                .andExpect(status().isGone());
    }
}

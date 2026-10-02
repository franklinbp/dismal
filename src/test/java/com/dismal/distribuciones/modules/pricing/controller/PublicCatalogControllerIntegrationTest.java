package com.dismal.distribuciones.modules.pricing.controller;

import com.dismal.distribuciones.modules.pricing.domain.PriceList;
import com.dismal.distribuciones.modules.pricing.domain.PriceListItem;
import com.dismal.distribuciones.modules.pricing.domain.PriceListType;
import com.dismal.distribuciones.modules.pricing.repository.PriceListItemRepository;
import com.dismal.distribuciones.modules.pricing.repository.PriceListRepository;
import com.dismal.distribuciones.modules.security.domain.CustomerType;
import com.dismal.distribuciones.modules.security.domain.Role;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.repository.UserRepository;
import com.dismal.distribuciones.modules.store.domain.Software;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicCatalogControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SoftwareRepository softwareRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PriceListRepository priceListRepository;

    @Autowired
    private PriceListItemRepository priceListItemRepository;

    @BeforeEach
    void setUp() {
        priceListItemRepository.deleteAll();
        priceListRepository.deleteAll();
        softwareRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void anonymousUserGetsPublicPrice() throws Exception {
        Software software = softwareRepository.save(Software.builder()
                .name("Windows Pro")
                .price(new BigDecimal("120.00"))
                .build());
        seedPrice(PriceListType.EC_FINAL, software, new BigDecimal("120.00"));

        mockMvc.perform(get("/api/public/products/" + software.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicPrice").value(120.0))
                .andExpect(jsonPath("$.effectivePrice").value(120.0))
                .andExpect(jsonPath("$.finalPrice").value(120.0))
                .andExpect(jsonPath("$.wholesalePrice").isEmpty())
                .andExpect(jsonPath("$.priceType").value("EC_FINAL"));
    }

    @Test
    @WithMockUser(username = "dist@example.com", authorities = "USER")
    void distributorGetsDistributorPrice() throws Exception {
        userRepository.save(User.builder()
                .firstname("Dist")
                .lastname("User")
                .email("dist@example.com")
                .password("secret")
                .role(Role.USER)
                .customerType(CustomerType.DISTRIBUTOR)
                .enabled(true)
                .build());

        Software software = softwareRepository.save(Software.builder()
                .name("Office Pro")
                .price(new BigDecimal("200.00"))
                .build());

        seedPrice(PriceListType.EC_FINAL, software, new BigDecimal("200.00"));
        seedPrice(PriceListType.EC_DISTRIBUTOR, software, new BigDecimal("150.00"));

        mockMvc.perform(get("/api/public/products/" + software.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicPrice").value(200.0))
                .andExpect(jsonPath("$.effectivePrice").value(150.0))
                .andExpect(jsonPath("$.finalPrice").value(200.0))
                .andExpect(jsonPath("$.wholesalePrice").value(150.0))
                .andExpect(jsonPath("$.priceType").value("EC_DISTRIBUTOR"));
    }

    @Test
    void publicCatalogUsesCountrySpecificPrices() throws Exception {
        Software software = softwareRepository.save(Software.builder()
                .name("Antivirus Pro")
                .price(new BigDecimal("25.00"))
                .build());

        seedPrice(PriceListType.PE_FINAL, software, new BigDecimal("99.90"));
        seedPrice(PriceListType.PE_DISTRIBUTOR, software, new BigDecimal("74.90"));

        mockMvc.perform(get("/api/public/products/" + software.getId()).param("country", "PE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effectivePrice").value(99.90))
                .andExpect(jsonPath("$.finalPrice").value(99.90))
                .andExpect(jsonPath("$.wholesalePrice").isEmpty())
                .andExpect(jsonPath("$.priceType").value("PE_FINAL"));
    }

    private void seedPrice(PriceListType type, Software software, BigDecimal price) {
        PriceList priceList = priceListRepository.save(PriceList.builder()
                .type(type)
                .name(type.name())
                .enabled(true)
                .build());

        priceListItemRepository.save(PriceListItem.builder()
                .priceList(priceList)
                .software(software)
                .price(price)
                .build());
    }
}

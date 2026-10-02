package com.dismal.distribuciones.modules.security.controller;

import com.dismal.distribuciones.exception.ResourceNotFoundException;
import com.dismal.distribuciones.modules.security.domain.CustomerType;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.customer.domain.CustomerMarket;
import com.dismal.distribuciones.modules.security.customer.service.CustomerMarketService;
import com.dismal.distribuciones.modules.security.dto.UserMeDTO;
import com.dismal.distribuciones.modules.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final CustomerMarketService customerMarketService;

    @GetMapping("/me")
    public ResponseEntity<UserMeDTO> me(
            @RequestParam(defaultValue = "EC") StorefrontCountry country
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401).build();
        }
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        CustomerMarket market = customerMarketService.getOrCreate(user, country);
        CustomerType customerType = market.getCustomerType() != null
                ? market.getCustomerType() : CustomerType.FINAL;
        BigDecimal creditLimit = market.getCreditLimit() != null ? market.getCreditLimit() : BigDecimal.ZERO;
        BigDecimal creditUsed = market.getCreditUsed() != null ? market.getCreditUsed() : BigDecimal.ZERO;
        BigDecimal creditAvailable = creditLimit.subtract(creditUsed).max(BigDecimal.ZERO);
        return ResponseEntity.ok(new UserMeDTO(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                customerType,
                market.getCountry(),
                market.getCurrency(),
                market.isActive(),
                user.getFirstname(),
                user.getLastname(),
                user.getPhone(),
                market.getTaxId(),
                market.getBillingEmail(),
                user.isEmailVerified(),
                market.isHasCredit(),
                creditLimit,
                creditUsed,
                creditAvailable,
                market.getCreditDays()
        ));
    }
}

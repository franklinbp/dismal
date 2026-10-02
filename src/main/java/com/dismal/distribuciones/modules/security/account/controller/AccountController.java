package com.dismal.distribuciones.modules.security.account.controller;

import com.dismal.distribuciones.modules.security.account.dto.AccountMessageResponse;
import com.dismal.distribuciones.modules.security.account.dto.ChangePasswordRequest;
import com.dismal.distribuciones.modules.security.account.dto.UpdateProfileRequest;
import com.dismal.distribuciones.modules.security.account.dto.WholesaleApplicationRequest;
import com.dismal.distribuciones.modules.security.account.dto.WholesaleApplicationResponse;
import com.dismal.distribuciones.modules.security.account.service.AccountIdentityService;
import com.dismal.distribuciones.modules.security.account.service.ClientAddressResolver;
import com.dismal.distribuciones.modules.security.account.service.WholesaleApplicationService;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.customer.domain.CustomerMarket;
import com.dismal.distribuciones.modules.security.customer.service.CustomerMarketService;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;
import com.dismal.distribuciones.modules.security.dto.UserMeDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountIdentityService accountIdentityService;
    private final WholesaleApplicationService wholesaleApplicationService;
    private final CustomerMarketService customerMarketService;

    @PutMapping("/profile")
    public ResponseEntity<UserMeDTO> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        User user = accountIdentityService.updateProfile(authentication.getName(), request);
        StorefrontCountry country = request.country() != null ? request.country() : StorefrontCountry.EC;
        return ResponseEntity.ok(toUserMe(user, customerMarketService.getOrCreate(user, country)));
    }

    @PutMapping("/password")
    public ResponseEntity<AccountMessageResponse> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        accountIdentityService.changePassword(authentication.getName(),
                request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(new AccountMessageResponse(
                "Contraseña actualizada. Inicia sesión nuevamente en tus dispositivos."));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<AccountMessageResponse> resendVerification(
            Authentication authentication,
            @RequestParam(defaultValue = "EC") String country,
            HttpServletRequest request
    ) {
        accountIdentityService.resendVerification(authentication.getName(), country,
                ClientAddressResolver.resolve(request));
        return ResponseEntity.accepted().body(new AccountMessageResponse(
                "Si tu correo aún no estaba verificado, enviamos un nuevo enlace."));
    }

    @PostMapping("/wholesale-applications")
    public ResponseEntity<WholesaleApplicationResponse> submitWholesale(
            Authentication authentication,
            @Valid @RequestBody WholesaleApplicationRequest request
    ) {
        return ResponseEntity.ok(wholesaleApplicationService.submit(authentication.getName(), request));
    }

    @GetMapping("/wholesale-applications/latest")
    public ResponseEntity<WholesaleApplicationResponse> latestWholesale(
            Authentication authentication,
            @RequestParam(defaultValue = "EC") StorefrontCountry country
    ) {
        WholesaleApplicationResponse response = wholesaleApplicationService.latestForUser(
                authentication.getName(), country
        );
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.noContent().build();
    }

    private UserMeDTO toUserMe(User user, CustomerMarket market) {
        BigDecimal creditLimit = market.getCreditLimit() != null ? market.getCreditLimit() : BigDecimal.ZERO;
        BigDecimal creditUsed = market.getCreditUsed() != null ? market.getCreditUsed() : BigDecimal.ZERO;
        return new UserMeDTO(
                user.getId(), user.getEmail(), user.getRole(), market.getCustomerType(),
                market.getCountry(), market.getCurrency(), market.isActive(),
                user.getFirstname(), user.getLastname(), user.getPhone(), market.getTaxId(),
                market.getBillingEmail(),
                user.isEmailVerified(), market.isHasCredit(), creditLimit, creditUsed,
                creditLimit.subtract(creditUsed).max(BigDecimal.ZERO), market.getCreditDays()
        );
    }
}

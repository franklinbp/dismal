package com.dismal.distribuciones.modules.storefront.controller;

import com.dismal.distribuciones.modules.sales.service.PaymentAccountService;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;
import com.dismal.distribuciones.modules.storefront.dto.StorefrontPaymentAccountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/storefront/payment-accounts")
@RequiredArgsConstructor
public class StorefrontPaymentAccountController {

    private final PaymentAccountService paymentAccountService;

    @GetMapping
    public ResponseEntity<List<StorefrontPaymentAccountResponse>> list(
            @RequestParam(defaultValue = "EC") String country
    ) {
        return ResponseEntity.ok(paymentAccountService.listStorefrontAccounts(StorefrontCountry.from(country)));
    }
}

package com.dismal.distribuciones.modules.security.account.controller;

import com.dismal.distribuciones.modules.security.account.dto.AccountMessageResponse;
import com.dismal.distribuciones.modules.security.account.dto.ForgotPasswordRequest;
import com.dismal.distribuciones.modules.security.account.dto.ResetPasswordRequest;
import com.dismal.distribuciones.modules.security.account.dto.VerifyEmailRequest;
import com.dismal.distribuciones.modules.security.account.service.AccountIdentityService;
import com.dismal.distribuciones.modules.security.account.service.ClientAddressResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/account")
@RequiredArgsConstructor
public class PublicAccountController {

    private final AccountIdentityService accountIdentityService;

    @PostMapping("/forgot-password")
    public ResponseEntity<AccountMessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest servletRequest
    ) {
        String message = accountIdentityService.requestPasswordReset(
                request.email(), request.country(), ClientAddressResolver.resolve(servletRequest));
        return ResponseEntity.accepted().body(new AccountMessageResponse(message));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AccountMessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        accountIdentityService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(new AccountMessageResponse("Contraseña actualizada. Ya puedes iniciar sesión."));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AccountMessageResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        accountIdentityService.verifyEmail(request.token());
        return ResponseEntity.ok(new AccountMessageResponse("Correo verificado correctamente."));
    }
}

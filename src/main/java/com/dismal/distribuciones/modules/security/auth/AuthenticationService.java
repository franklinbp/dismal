package com.dismal.distribuciones.modules.security.auth;

import com.dismal.distribuciones.config.JwtService;
import com.dismal.distribuciones.exception.ConflictException;
import com.dismal.distribuciones.modules.integrations.crm.event.CustomerChangedCrmSyncEvent;
import com.dismal.distribuciones.modules.security.domain.CustomerType;
import com.dismal.distribuciones.modules.security.domain.Role;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.repository.UserRepository;
import com.dismal.distribuciones.modules.security.account.service.AccountIdentityService;
import com.dismal.distribuciones.modules.security.account.service.AuthenticationThrottleService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ApplicationEventPublisher eventPublisher;
    private final AccountIdentityService accountIdentityService;
    private final AuthenticationThrottleService throttleService;

    public AuthenticationResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (repository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Email already registered");
        }
        var user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .customerType(CustomerType.FINAL)
                .emailVerified(false)
                .build();

        User saved = repository.save(user);
        eventPublisher.publishEvent(new CustomerChangedCrmSyncEvent(this, saved.getId()));
        accountIdentityService.issueVerification(saved, request.getCountry());
        var jwtToken = jwtService.generateToken(saved);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request, String clientAddress) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        throttleService.assertLoginAllowed(normalizedEmail, clientAddress);
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );
        } catch (AuthenticationException ex) {
            throttleService.recordLoginFailure(normalizedEmail, clientAddress);
            throw ex;
        }
        throttleService.clearLoginFailures(normalizedEmail, clientAddress);
        var user = repository.findByEmail(normalizedEmail)
                .orElseThrow();
        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

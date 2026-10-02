package com.dismal.distribuciones.modules.security.account.service;

import com.dismal.distribuciones.modules.integrations.notifications.service.EmailNotificationService;
import com.dismal.distribuciones.modules.security.account.domain.AccountActionToken;
import com.dismal.distribuciones.modules.security.account.domain.AccountTokenType;
import com.dismal.distribuciones.modules.security.account.repository.AccountActionTokenRepository;
import com.dismal.distribuciones.modules.security.domain.Role;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.repository.UserRepository;
import com.dismal.distribuciones.modules.security.customer.service.CustomerMarketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountIdentityServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountActionTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailNotificationService emailNotificationService;

    @Mock
    private CustomerMarketService customerMarketService;

    private AccountIdentityService service;

    @BeforeEach
    void setUp() {
        service = new AccountIdentityService(
                userRepository,
                tokenRepository,
                passwordEncoder,
                emailNotificationService,
                new AuthenticationThrottleService(),
                customerMarketService
        );
    }

    @Test
    void resetPasswordVerifiesEmailAndRevokesExistingSessions() {
        User user = User.builder()
                .email("client@example.com")
                .password("old-hash")
                .role(Role.CUSTOMER)
                .emailVerified(false)
                .securityVersion(4)
                .build();
        AccountActionToken token = AccountActionToken.builder()
                .user(user)
                .type(AccountTokenType.PASSWORD_RESET)
                .tokenHash("stored-hash")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(tokenRepository.findByTokenHashAndType(anyString(), eq(AccountTokenType.PASSWORD_RESET)))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewSecurePassword123")).thenReturn("new-hash");

        service.resetPassword("raw-token", "NewSecurePassword123");

        assertThat(user.getPassword()).isEqualTo("new-hash");
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getEmailVerifiedAt()).isNotNull();
        assertThat(user.getSecurityVersion()).isEqualTo(5);
        assertThat(token.getConsumedAt()).isNotNull();
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
        verify(tokenRepository).consumeActiveTokens(
                eq(user), eq(AccountTokenType.PASSWORD_RESET), any(LocalDateTime.class));
    }
}

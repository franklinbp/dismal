package com.dismal.distribuciones.modules.security.account.service;

import com.dismal.distribuciones.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationThrottleServiceTest {

    @Test
    void blocksLoginAfterFiveFailuresAndAllowsItAfterClear() {
        AuthenticationThrottleService service = new AuthenticationThrottleService();

        for (int attempt = 0; attempt < 5; attempt++) {
            service.recordLoginFailure("client@example.com", "203.0.113.10");
        }

        assertThatThrownBy(() -> service.assertLoginAllowed("client@example.com", "203.0.113.10"))
                .isInstanceOf(TooManyRequestsException.class);

        service.clearLoginFailures("client@example.com", "203.0.113.10");

        assertThatCode(() -> service.assertLoginAllowed("client@example.com", "203.0.113.10"))
                .doesNotThrowAnyException();
    }
}

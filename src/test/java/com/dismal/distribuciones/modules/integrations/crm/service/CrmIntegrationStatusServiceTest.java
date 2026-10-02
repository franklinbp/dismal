package com.dismal.distribuciones.modules.integrations.crm.service;

import com.dismal.distribuciones.modules.integrations.crm.dto.CrmIntegrationStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrmIntegrationStatusServiceTest {

    @Mock
    private DismalCrmGateway gateway;

    private CrmIntegrationStatusService service;

    @BeforeEach
    void setUp() {
        service = new CrmIntegrationStatusService(gateway);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "whatsappEnabled", true);
        when(gateway.getPublicEndpoint()).thenReturn("https://crm.dismal.vip");
    }

    @Test
    void reportsDisabledIntegration() {
        ReflectionTestUtils.setField(service, "enabled", false);

        CrmIntegrationStatusResponse status = service.getStatus();

        assertThat(status.enabled()).isFalse();
        assertThat(status.reachable()).isFalse();
    }

    @Test
    void reportsMissingPrivateConfiguration() {
        when(gateway.isConfigured()).thenReturn(false);

        CrmIntegrationStatusResponse status = service.getStatus();

        assertThat(status.configured()).isFalse();
        assertThat(status.message()).contains("Falta configurar");
    }

    @Test
    void reportsAuthenticatedConnection() {
        when(gateway.isConfigured()).thenReturn(true);

        CrmIntegrationStatusResponse status = service.getStatus();

        assertThat(status.reachable()).isTrue();
        assertThat(status.endpoint()).isEqualTo("https://crm.dismal.vip");
    }

    @Test
    void reportsRejectedOrUnavailableCrm() {
        when(gateway.isConfigured()).thenReturn(true);
        doThrow(new IllegalStateException("rejected")).when(gateway).verifyConnection();

        CrmIntegrationStatusResponse status = service.getStatus();

        assertThat(status.reachable()).isFalse();
        assertThat(status.message()).contains("no pudo validar");
    }
}

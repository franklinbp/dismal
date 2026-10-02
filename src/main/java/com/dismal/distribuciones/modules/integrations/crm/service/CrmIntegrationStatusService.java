package com.dismal.distribuciones.modules.integrations.crm.service;

import com.dismal.distribuciones.modules.integrations.crm.dto.CrmIntegrationStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CrmIntegrationStatusService {

    private final DismalCrmGateway gateway;

    @Value("${integrations.dismal-crm.enabled:false}")
    private boolean enabled;

    @Value("${integrations.dismal-crm.whatsapp-enabled:false}")
    private boolean whatsappEnabled;

    public CrmIntegrationStatusResponse getStatus() {
        boolean configured = gateway.isConfigured();
        String endpoint = gateway.getPublicEndpoint();

        if (!enabled) {
            return new CrmIntegrationStatusResponse(
                    false,
                    configured,
                    whatsappEnabled,
                    false,
                    endpoint,
                    "La integracion con DismalCRM esta deshabilitada en el servidor."
            );
        }
        if (!configured) {
            return new CrmIntegrationStatusResponse(
                    true,
                    false,
                    whatsappEnabled,
                    false,
                    endpoint,
                    "Falta configurar la URL o el token privado de DismalCRM."
            );
        }

        try {
            gateway.verifyConnection();
            return new CrmIntegrationStatusResponse(
                    true,
                    true,
                    whatsappEnabled,
                    true,
                    endpoint,
                    "Conexion autenticada con DismalCRM."
            );
        } catch (Exception exception) {
            return new CrmIntegrationStatusResponse(
                    true,
                    true,
                    whatsappEnabled,
                    false,
                    endpoint,
                    "DismalCRM no pudo validar la conexion o el token configurado."
            );
        }
    }
}

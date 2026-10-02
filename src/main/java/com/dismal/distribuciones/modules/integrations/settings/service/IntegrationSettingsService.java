package com.dismal.distribuciones.modules.integrations.settings.service;

import com.dismal.distribuciones.exception.BadRequestException;
import com.dismal.distribuciones.modules.integrations.settings.domain.IntegrationSettings;
import com.dismal.distribuciones.modules.integrations.settings.dto.IntegrationSettingsRequest;
import com.dismal.distribuciones.modules.integrations.settings.dto.IntegrationSettingsResponse;
import com.dismal.distribuciones.modules.integrations.settings.dto.IntegrationSettingsTestEmailRequest;
import com.dismal.distribuciones.modules.integrations.settings.repository.IntegrationSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IntegrationSettingsService {

    private final IntegrationSettingsRepository repository;

    @Value("${integrations.n8n.webhook-url:}")
    private String defaultWebhookUrl;

    @Value("${integrations.n8n.secret:}")
    private String defaultSecret;

    @Value("${outbox.dispatch.enabled:true}")
    private boolean defaultDispatchEnabled;

    @Value("${outbox.dispatch.rate-ms:5000}")
    private long defaultDispatchRateMs;

    @Value("${outbox.dispatch.max-attempts:10}")
    private int defaultDispatchMaxAttempts;

    @Value("${app.email.smtp.enabled:false}")
    private boolean defaultSmtpEnabled;

    @Value("${app.email.smtp.host:}")
    private String defaultSmtpHost;

    @Value("${app.email.smtp.port:0}")
    private int defaultSmtpPort;

    @Value("${app.email.smtp.user:}")
    private String defaultSmtpUser;

    @Value("${app.email.smtp.password:}")
    private String defaultSmtpPassword;

    @Value("${app.email.smtp.tls:true}")
    private boolean defaultSmtpTls;

    @Value("${app.email.smtp.from-email:}")
    private String defaultSmtpFromEmail;

    @Value("${app.email.smtp.from-name:}")
    private String defaultSmtpFromName;

    @Value("${integrations.dismal-crm.whatsapp-enabled:false}")
    private boolean defaultCrmWhatsappEnabled;

    @Value("${integrations.dismal-crm.whatsapp-id:}")
    private String defaultCrmWhatsappDefaultId;

    @Value("${integrations.dismal-crm.whatsapp-id-by-country:}")
    private String defaultCrmWhatsappIdByCountry;

    @Transactional(readOnly = true)
    public IntegrationSettings getSettingsEntity() {
        return repository.findTopByOrderByUpdatedAtDesc()
                .orElseGet(this::defaultSettings);
    }

    @Transactional(readOnly = true)
    public IntegrationSettingsResponse getSettings() {
        IntegrationSettings settings = getSettingsEntity();
        return toResponse(settings);
    }

    @Transactional
    public IntegrationSettingsResponse updateSettings(IntegrationSettingsRequest request) {
        if (request == null) {
            throw new BadRequestException("Settings payload required");
        }

        IntegrationSettings settings = repository.findTopByOrderByUpdatedAtDesc()
                .orElseGet(this::defaultSettings);

        if (request.webhookUrl() != null) {
            boolean nextDispatchEnabled = request.dispatchEnabled() != null
                    ? request.dispatchEnabled()
                    : settings.isDispatchEnabled();
            if (request.webhookUrl().isBlank() && nextDispatchEnabled) {
                throw new BadRequestException("Webhook URL cannot be blank when dispatch is enabled");
            }
            settings.setWebhookUrl(request.webhookUrl().trim());
        }

        if (request.secret() != null) {
            String secret = request.secret().trim();
            settings.setSecret(secret.isEmpty() ? null : secret);
        }

        if (request.dispatchEnabled() != null) {
            settings.setDispatchEnabled(request.dispatchEnabled());
        }
        if (request.dispatchRateMs() != null) {
            if (request.dispatchRateMs() <= 0) {
                throw new BadRequestException("dispatchRateMs must be greater than zero");
            }
            settings.setDispatchRateMs(request.dispatchRateMs());
        }
        if (request.dispatchMaxAttempts() != null) {
            if (request.dispatchMaxAttempts() <= 0) {
                throw new BadRequestException("dispatchMaxAttempts must be greater than zero");
            }
            settings.setDispatchMaxAttempts(request.dispatchMaxAttempts());
        }

        if (request.smtpEnabled() != null) {
            settings.setSmtpEnabled(request.smtpEnabled());
        }
        if (request.smtpHost() != null) {
            settings.setSmtpHost(request.smtpHost().trim());
        }
        if (request.smtpPort() != null) {
            if (request.smtpPort() <= 0) {
                throw new BadRequestException("smtpPort must be greater than zero");
            }
            settings.setSmtpPort(request.smtpPort());
        }
        if (request.smtpUser() != null) {
            settings.setSmtpUser(request.smtpUser().trim());
        }
        if (request.smtpPassword() != null) {
            String password = request.smtpPassword().trim();
            settings.setSmtpPassword(password.isEmpty() ? null : password);
        }
        if (request.smtpTls() != null) {
            settings.setSmtpTls(request.smtpTls());
        }
        if (request.smtpFromEmail() != null) {
            settings.setSmtpFromEmail(request.smtpFromEmail().trim());
        }
        if (request.smtpFromName() != null) {
            settings.setSmtpFromName(request.smtpFromName().trim());
        }
        if (request.crmWhatsappEnabled() != null) {
            settings.setCrmWhatsappEnabled(request.crmWhatsappEnabled());
        }
        if (request.crmWhatsappDefaultId() != null) {
            settings.setCrmWhatsappDefaultId(blankToNull(request.crmWhatsappDefaultId()));
        }
        if (request.crmWhatsappIdByCountry() != null) {
            settings.setCrmWhatsappIdByCountry(blankToNull(request.crmWhatsappIdByCountry()));
        }

        if (Boolean.TRUE.equals(settings.getCrmWhatsappEnabled())) {
            boolean hasDefaultId = settings.getCrmWhatsappDefaultId() != null
                    && !settings.getCrmWhatsappDefaultId().isBlank();
            boolean hasCountryMap = settings.getCrmWhatsappIdByCountry() != null
                    && !settings.getCrmWhatsappIdByCountry().isBlank();
            if (!hasDefaultId && !hasCountryMap) {
                throw new BadRequestException("Configure al menos un WhatsApp ID para DismalCRM");
            }
        }

        if (settings.isSmtpEnabled()) {
            if (settings.getSmtpHost() == null || settings.getSmtpHost().isBlank()) {
                throw new BadRequestException("smtpHost is required when SMTP is enabled");
            }
            if (settings.getSmtpPort() == null || settings.getSmtpPort() <= 0) {
                throw new BadRequestException("smtpPort is required when SMTP is enabled");
            }
            if (settings.getSmtpFromEmail() == null || settings.getSmtpFromEmail().isBlank()) {
                throw new BadRequestException("smtpFromEmail is required when SMTP is enabled");
            }
        }

        IntegrationSettings saved = repository.save(settings);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public IntegrationSettings resolveSmtpSettingsForTest(IntegrationSettingsTestEmailRequest request) {
        IntegrationSettings base = getSettingsEntity();
        IntegrationSettings settings = IntegrationSettings.builder()
                .id(base.getId())
                .webhookUrl(base.getWebhookUrl())
                .secret(base.getSecret())
                .dispatchEnabled(base.isDispatchEnabled())
                .dispatchRateMs(base.getDispatchRateMs())
                .dispatchMaxAttempts(base.getDispatchMaxAttempts())
                .smtpEnabled(base.isSmtpEnabled())
                .smtpHost(base.getSmtpHost())
                .smtpPort(base.getSmtpPort())
                .smtpUser(base.getSmtpUser())
                .smtpPassword(base.getSmtpPassword())
                .smtpTls(base.isSmtpTls())
                .smtpFromEmail(base.getSmtpFromEmail())
                .smtpFromName(base.getSmtpFromName())
                .crmWhatsappEnabled(resolveCrmWhatsappEnabled(base))
                .crmWhatsappDefaultId(base.getCrmWhatsappDefaultId())
                .crmWhatsappIdByCountry(base.getCrmWhatsappIdByCountry())
                .createdAt(base.getCreatedAt())
                .updatedAt(base.getUpdatedAt())
                .build();

        if (request == null) {
            validateSmtpSettings(settings);
            return settings;
        }

        if (request.smtpEnabled() != null) {
            settings.setSmtpEnabled(request.smtpEnabled());
        }
        if (request.smtpHost() != null) {
            settings.setSmtpHost(request.smtpHost().trim());
        }
        if (request.smtpPort() != null) {
            if (request.smtpPort() <= 0) {
                throw new BadRequestException("smtpPort must be greater than zero");
            }
            settings.setSmtpPort(request.smtpPort());
        }
        if (request.smtpUser() != null) {
            settings.setSmtpUser(request.smtpUser().trim());
        }
        if (request.smtpPassword() != null) {
            String password = request.smtpPassword().trim();
            if (!password.isEmpty()) {
                settings.setSmtpPassword(password);
            }
        }
        if (request.smtpTls() != null) {
            settings.setSmtpTls(request.smtpTls());
        }
        if (request.smtpFromEmail() != null) {
            settings.setSmtpFromEmail(request.smtpFromEmail().trim());
        }
        if (request.smtpFromName() != null) {
            settings.setSmtpFromName(request.smtpFromName().trim());
        }

        validateSmtpSettings(settings);
        return settings;
    }

    private IntegrationSettings defaultSettings() {
        return IntegrationSettings.builder()
                .webhookUrl(defaultWebhookUrl == null ? "" : defaultWebhookUrl)
                .secret(defaultSecret == null || defaultSecret.isBlank() ? null : defaultSecret)
                .dispatchEnabled(defaultDispatchEnabled)
                .dispatchRateMs(defaultDispatchRateMs)
                .dispatchMaxAttempts(defaultDispatchMaxAttempts)
                .smtpEnabled(defaultSmtpEnabled)
                .smtpHost(defaultSmtpHost == null ? "" : defaultSmtpHost)
                .smtpPort(defaultSmtpPort > 0 ? defaultSmtpPort : null)
                .smtpUser(defaultSmtpUser == null ? "" : defaultSmtpUser)
                .smtpPassword(defaultSmtpPassword == null || defaultSmtpPassword.isBlank() ? null : defaultSmtpPassword)
                .smtpTls(defaultSmtpTls)
                .smtpFromEmail(defaultSmtpFromEmail == null ? "" : defaultSmtpFromEmail)
                .smtpFromName(defaultSmtpFromName == null ? "" : defaultSmtpFromName)
                .crmWhatsappEnabled(defaultCrmWhatsappEnabled)
                .crmWhatsappDefaultId(defaultCrmWhatsappDefaultId == null || defaultCrmWhatsappDefaultId.isBlank()
                        ? null
                        : defaultCrmWhatsappDefaultId)
                .crmWhatsappIdByCountry(defaultCrmWhatsappIdByCountry == null || defaultCrmWhatsappIdByCountry.isBlank()
                        ? null
                        : defaultCrmWhatsappIdByCountry)
                .build();
    }

    private void validateSmtpSettings(IntegrationSettings settings) {
        if (!settings.isSmtpEnabled()) {
            throw new BadRequestException("SMTP is disabled.");
        }
        if (settings.getSmtpHost() == null || settings.getSmtpHost().isBlank()) {
            throw new BadRequestException("smtpHost is required when SMTP is enabled");
        }
        if (settings.getSmtpPort() == null || settings.getSmtpPort() <= 0) {
            throw new BadRequestException("smtpPort is required when SMTP is enabled");
        }
        if (settings.getSmtpFromEmail() == null || settings.getSmtpFromEmail().isBlank()) {
            throw new BadRequestException("smtpFromEmail is required when SMTP is enabled");
        }
    }

    private IntegrationSettingsResponse toResponse(IntegrationSettings settings) {
        boolean hasSecret = settings.getSecret() != null && !settings.getSecret().isBlank();
        boolean hasSmtpPassword = settings.getSmtpPassword() != null && !settings.getSmtpPassword().isBlank();
        return new IntegrationSettingsResponse(
                settings.getId(),
                settings.getWebhookUrl(),
                settings.isDispatchEnabled(),
                settings.getDispatchRateMs(),
                settings.getDispatchMaxAttempts(),
                hasSecret,
                settings.isSmtpEnabled(),
                settings.getSmtpHost(),
                settings.getSmtpPort(),
                settings.getSmtpUser(),
                settings.isSmtpTls(),
                settings.getSmtpFromEmail(),
                settings.getSmtpFromName(),
                hasSmtpPassword,
                resolveCrmWhatsappEnabled(settings),
                settings.getCrmWhatsappDefaultId(),
                settings.getCrmWhatsappIdByCountry(),
                settings.getUpdatedAt()
        );
    }

    public boolean resolveCrmWhatsappEnabled(IntegrationSettings settings) {
        if (settings != null && settings.getCrmWhatsappEnabled() != null) {
            return settings.getCrmWhatsappEnabled();
        }
        return defaultCrmWhatsappEnabled;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

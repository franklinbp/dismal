package com.dismal.distribuciones.modules.integrations.settings.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "integration_settings")
public class IntegrationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String webhookUrl;

    @Column(columnDefinition = "TEXT")
    private String secret;

    @Column(nullable = false)
    private boolean dispatchEnabled;

    @Column(nullable = false)
    private long dispatchRateMs;

    @Column(nullable = false)
    private int dispatchMaxAttempts;

    @Column(nullable = false)
    private boolean smtpEnabled;

    private String smtpHost;

    private Integer smtpPort;

    private String smtpUser;

    @Column(columnDefinition = "TEXT")
    private String smtpPassword;

    @Column(nullable = false)
    private boolean smtpTls;

    private String smtpFromEmail;

    private String smtpFromName;

    private Boolean crmWhatsappEnabled;

    private String crmWhatsappDefaultId;

    @Column(columnDefinition = "TEXT")
    private String crmWhatsappIdByCountry;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}

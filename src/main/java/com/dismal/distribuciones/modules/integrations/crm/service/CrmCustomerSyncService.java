package com.dismal.distribuciones.modules.integrations.crm.service;

import com.dismal.distribuciones.modules.integrations.crm.domain.CrmCustomerSync;
import com.dismal.distribuciones.modules.integrations.crm.domain.CrmCustomerSyncStatus;
import com.dismal.distribuciones.modules.integrations.crm.dto.CrmCustomerSyncResponse;
import com.dismal.distribuciones.modules.integrations.crm.repository.CrmCustomerSyncRepository;
import com.dismal.distribuciones.modules.security.domain.Role;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrmCustomerSyncService {

    private final UserRepository userRepository;
    private final CrmCustomerSyncRepository crmCustomerSyncRepository;
    private final DismalCrmGateway gateway;

    @Value("${integrations.dismal-crm.enabled:false}")
    private boolean enabled;

    @Transactional
    public CrmCustomerSyncResponse syncAllCustomers() {
        if (!enabled) {
            return new CrmCustomerSyncResponse(false, 0, 0, 0, 0, 0, 0, List.of("DismalCRM sync is disabled."));
        }

        List<String> errors = new ArrayList<>();
        SyncCounters counters = new SyncCounters();

        List<User> eligibleCustomers = userRepository.findByRoleInAndEnabledTrue(List.of(Role.USER, Role.CUSTOMER))
                .stream()
                .filter(this::hasDeliverablePhone)
                .toList();

        for (User user : eligibleCustomers) {
            counters.processed++;
            try {
                syncEligibleCustomer(user, counters);
            } catch (Exception ex) {
                log.warn("Failed to sync customer {} to DismalCRM: {}", user.getId(), ex.getMessage());
                registerFailure(user.getId(), ex.getMessage());
                counters.failed++;
                errors.add(user.getEmail() + ": " + ex.getMessage());
            }
        }

        markIneligibleCustomersAsDisabled(counters);

        return new CrmCustomerSyncResponse(
                true,
                counters.processed,
                counters.created,
                counters.updated,
                counters.skipped,
                counters.disabled,
                counters.failed,
                errors
        );
    }

    @Transactional
    public void syncCustomer(UUID userId) {
        if (!enabled || userId == null) {
            return;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            markSyncDisabled(userId);
            return;
        }

        boolean eligible = user.isEnabled()
                && (user.getRole() == Role.USER || user.getRole() == Role.CUSTOMER)
                && hasDeliverablePhone(user);

        if (!eligible) {
            markSyncDisabled(userId);
            return;
        }

        try {
            syncEligibleCustomer(user, new SyncCounters());
        } catch (Exception ex) {
            log.warn("Failed to sync customer {} to DismalCRM: {}", userId, ex.getMessage());
            registerFailure(userId, ex.getMessage());
        }
    }

    private void syncEligibleCustomer(User user, SyncCounters counters) {
        String name = buildName(user);
        String payloadHash = hashPayload(user, name);
        CrmCustomerSync sync = crmCustomerSyncRepository.findByUserId(user.getId())
                .orElseGet(() -> CrmCustomerSync.builder()
                        .userId(user.getId())
                        .syncStatus(CrmCustomerSyncStatus.PENDING)
                        .build());

        if (sync.getCrmCampaignClientId() != null
                && sync.getSyncStatus() == CrmCustomerSyncStatus.SYNCED
                && payloadHash.equals(sync.getPayloadHash())) {
            sync.setLastSyncedAt(LocalDateTime.now());
            crmCustomerSyncRepository.save(sync);
            counters.skipped++;
            return;
        }

        DismalCrmGateway.UpsertCampaignClientRequest request = new DismalCrmGateway.UpsertCampaignClientRequest(
                name,
                null,
                user.getPhone(),
                user.getEmail(),
                user.getCustomerType() != null ? user.getCustomerType().name() : null
        );

        boolean created = false;
        Integer remoteId = sync.getCrmCampaignClientId();
        if (remoteId == null) {
            DismalCrmGateway.RemoteCampaignClient existing = gateway.findByPhone(user.getPhone());
            if (existing == null && user.getEmail() != null && !user.getEmail().isBlank()) {
                existing = gateway.findByEmail(user.getEmail());
            }
            if (existing != null) {
                remoteId = existing.id();
            }
        }

        DismalCrmGateway.RemoteCampaignClient remote;
        if (remoteId == null) {
            remote = gateway.createCampaignClient(request);
            created = true;
        } else {
            remote = gateway.updateCampaignClient(remoteId, request);
        }

        sync.setCrmCampaignClientId(remote != null ? remote.id() : remoteId);
        sync.setPayloadHash(payloadHash);
        sync.setSyncStatus(CrmCustomerSyncStatus.SYNCED);
        sync.setLastError(null);
        sync.setLastSyncedAt(LocalDateTime.now());
        crmCustomerSyncRepository.save(sync);

        if (created) {
            counters.created++;
        } else {
            counters.updated++;
        }
    }

    private void markIneligibleCustomersAsDisabled(SyncCounters counters) {
        List<CrmCustomerSync> syncRecords = crmCustomerSyncRepository.findAll();

        for (CrmCustomerSync sync : syncRecords) {
            User user = userRepository.findById(sync.getUserId()).orElse(null);
            boolean stillEligible = user != null
                    && user.isEnabled()
                    && (user.getRole() == Role.USER || user.getRole() == Role.CUSTOMER)
                    && hasDeliverablePhone(user);

            if (stillEligible || sync.getSyncStatus() == CrmCustomerSyncStatus.DISABLED) {
                continue;
            }

            sync.setSyncStatus(CrmCustomerSyncStatus.DISABLED);
            sync.setLastError(null);
            sync.setLastSyncedAt(LocalDateTime.now());
            crmCustomerSyncRepository.save(sync);
            counters.disabled++;
        }
    }

    private void markSyncDisabled(UUID userId) {
        CrmCustomerSync sync = crmCustomerSyncRepository.findByUserId(userId).orElse(null);
        if (sync == null) {
            return;
        }
        sync.setSyncStatus(CrmCustomerSyncStatus.DISABLED);
        sync.setLastError(null);
        sync.setLastSyncedAt(LocalDateTime.now());
        crmCustomerSyncRepository.save(sync);
    }

    private void registerFailure(UUID userId, String message) {
        CrmCustomerSync sync = crmCustomerSyncRepository.findByUserId(userId)
                .orElseGet(() -> CrmCustomerSync.builder()
                        .userId(userId)
                        .build());
        sync.setSyncStatus(CrmCustomerSyncStatus.FAILED);
        sync.setLastError(message);
        sync.setLastSyncedAt(LocalDateTime.now());
        crmCustomerSyncRepository.save(sync);
    }

    private boolean hasDeliverablePhone(User user) {
        return user.getPhone() != null && !user.getPhone().isBlank();
    }

    private String buildName(User user) {
        String firstname = user.getFirstname() != null ? user.getFirstname().trim() : "";
        String lastname = user.getLastname() != null ? user.getLastname().trim() : "";
        String fullName = (firstname + " " + lastname).trim();
        return fullName.isBlank() ? user.getEmail() : fullName;
    }

    private String hashPayload(User user, String name) {
        String value = String.join("|",
                safe(name),
                safe(user.getPhone()),
                safe(user.getEmail()),
                user.getCustomerType() != null ? user.getCustomerType().name() : "");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to hash CRM sync payload", ex);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class SyncCounters {
        private int processed;
        private int created;
        private int updated;
        private int skipped;
        private int disabled;
        private int failed;
    }
}

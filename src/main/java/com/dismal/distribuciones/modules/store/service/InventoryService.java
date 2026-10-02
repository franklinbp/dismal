package com.dismal.distribuciones.modules.store.service;

import com.dismal.distribuciones.exception.BadRequestException;
import com.dismal.distribuciones.exception.ConflictException;
import com.dismal.distribuciones.exception.ResourceNotFoundException;
import com.dismal.distribuciones.modules.store.domain.License;
import com.dismal.distribuciones.modules.store.domain.LicenseStatus;
import com.dismal.distribuciones.modules.store.domain.Software;
import com.dismal.distribuciones.modules.store.dto.BulkLicenseRequest;
import com.dismal.distribuciones.modules.store.dto.CreateLicenseRequest;
import com.dismal.distribuciones.modules.store.dto.LicenseAssignmentResponse;
import com.dismal.distribuciones.modules.store.dto.LicenseResponse;
import com.dismal.distribuciones.modules.store.dto.LicenseUpdateRequest;
import com.dismal.distribuciones.modules.store.dto.StockSummaryResponse;
import com.dismal.distribuciones.modules.sales.domain.SaleFulfillment;
import com.dismal.distribuciones.modules.sales.repository.SaleFulfillmentRepository;
import com.dismal.distribuciones.modules.store.repository.LicenseRepository;
import com.dismal.distribuciones.modules.store.repository.SoftwareRepository;
import com.dismal.distribuciones.modules.store.repository.StockSummaryView;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final LicenseRepository licenseRepository;
    private final SoftwareRepository softwareRepository;
    private final UserRepository userRepository;
    private final SaleFulfillmentRepository saleFulfillmentRepository;

    @Transactional(readOnly = true)
    public Page<LicenseResponse> listLicenses(UUID softwareId, LicenseStatus status, int page, int size) {
        Specification<License> spec = Specification.where(null);
        if (softwareId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("software").get("id"), softwareId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        return licenseRepository.findAll(spec, pageRequest)
                .map(this::toLicenseResponse);
    }

    @Transactional
    public LicenseResponse createLicense(CreateLicenseRequest request) {
        validateCreateRequest(request);
        Software software = softwareRepository.findById(request.softwareId())
                .orElseThrow(() -> new ResourceNotFoundException("Software not found with ID: " + request.softwareId()));

        License license = License.builder()
                .licenseKey(request.licenseKey())
                .purchasePrice(request.purchasePrice())
                .maxActivations(request.maxActivations() != null ? request.maxActivations() : 1)
                .status(LicenseStatus.ACTIVE)
                .software(software)
                .build();

        return toLicenseResponse(licenseRepository.save(license));
    }

    @Transactional
    public List<LicenseResponse> createLicensesBulk(BulkLicenseRequest request) {
        if (request == null || request.licenses() == null || request.licenses().isEmpty()) {
            throw new BadRequestException("Bulk licenses payload is required.");
        }
        return request.licenses().stream()
                .map(this::createLicense)
                .toList();
    }

    @Transactional
    public LicenseResponse updateLicense(UUID licenseId, LicenseUpdateRequest request) {
        License license = licenseRepository.findById(licenseId)
                .orElseThrow(() -> new ResourceNotFoundException("License not found with ID: " + licenseId));

        if (request.status() != null) {
            license.setStatus(request.status());
        }
        if (request.available() != null) {
            if (Boolean.TRUE.equals(request.available())) {
                license.setStatus(LicenseStatus.ACTIVE);
            } else {
                license.setStatus(LicenseStatus.INACTIVE);
            }
        }
        return toLicenseResponse(licenseRepository.save(license));
    }

    @Transactional
    public void deleteLicense(UUID licenseId) {
        License license = licenseRepository.findById(licenseId)
                .orElseThrow(() -> new ResourceNotFoundException("License not found with ID: " + licenseId));
        if (license.getUsedActivations() != null && license.getUsedActivations() > 0) {
            throw new ConflictException("No se puede eliminar una licencia con activaciones usadas.");
        }
        licenseRepository.delete(license);
    }

    @Transactional(readOnly = true)
    public List<StockSummaryResponse> getStockSummary() {
        List<StockSummaryView> rows = licenseRepository.getStockSummary();
        return rows.stream()
                .map(row -> new StockSummaryResponse(
                        row.getSoftwareId(),
                        row.getSoftwareName(),
                        row.getAvailableCount() != null ? row.getAvailableCount() : 0L,
                        row.getAssignedCount() != null ? row.getAssignedCount() : 0L
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LicenseAssignmentResponse> listLicenseAssignments(UUID licenseId) {
        if (licenseId == null || !licenseRepository.existsById(licenseId)) {
            throw new ResourceNotFoundException("License not found with ID: " + licenseId);
        }
        List<SaleFulfillment> fulfillments = saleFulfillmentRepository.findByLicenseIdWithClient(licenseId);
        return fulfillments.stream()
                .map(fulfillment -> {
                    User client = fulfillment.getSale().getClient();
                    String clientName = (client.getFirstname() + " " + client.getLastname()).trim();
                    return new LicenseAssignmentResponse(
                            fulfillment.getSale().getId(),
                            client.getId(),
                            clientName.isBlank() ? "-" : clientName,
                            client.getEmail(),
                            fulfillment.getAssignedAt()
                    );
                })
                .toList();
    }

    private void validateCreateRequest(CreateLicenseRequest request) {
        if (request == null) {
            throw new BadRequestException("License payload is required.");
        }
        if (request.softwareId() == null) {
            throw new BadRequestException("softwareId is required.");
        }
        if (request.licenseKey() == null || request.licenseKey().isBlank()) {
            throw new BadRequestException("licenseKey is required.");
        }
        if (request.purchasePrice() == null) {
            throw new BadRequestException("purchasePrice is required.");
        }
        if (request.purchasePrice().signum() < 0) {
            throw new BadRequestException("purchasePrice cannot be negative.");
        }
        if (request.maxActivations() != null && request.maxActivations() <= 0) {
            throw new BadRequestException("maxActivations must be greater than zero.");
        }
    }

    private LicenseResponse toLicenseResponse(License license) {
        User owner = null;
        if (license.getOwner() != null) {
            owner = userRepository.findById(license.getOwner().getId()).orElse(null);
        }
        boolean available = license.getStatus() == LicenseStatus.ACTIVE
                && license.getUsedActivations() < license.getMaxActivations();
        return new LicenseResponse(
                license.getId(),
                license.getLicenseKey(),
                license.getStatus(),
                available,
                license.getUsedActivations(),
                license.getMaxActivations(),
                license.getPurchasePrice(),
                license.getSoftware().getId(),
                license.getSoftware().getName(),
                owner != null ? owner.getId() : null,
                owner != null ? owner.getEmail() : null
        );
    }
}

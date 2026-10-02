package com.dismal.distribuciones.modules.security.account.service;

import com.dismal.distribuciones.exception.BadRequestException;
import com.dismal.distribuciones.exception.ResourceNotFoundException;
import com.dismal.distribuciones.modules.integrations.crm.event.CustomerChangedCrmSyncEvent;
import com.dismal.distribuciones.modules.security.account.domain.WholesaleApplication;
import com.dismal.distribuciones.modules.security.account.domain.WholesaleApplicationStatus;
import com.dismal.distribuciones.modules.security.account.dto.WholesaleApplicationRequest;
import com.dismal.distribuciones.modules.security.account.dto.WholesaleApplicationResponse;
import com.dismal.distribuciones.modules.security.account.dto.WholesaleApplicationReviewRequest;
import com.dismal.distribuciones.modules.security.account.repository.WholesaleApplicationRepository;
import com.dismal.distribuciones.modules.security.domain.CustomerType;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.customer.domain.CustomerMarket;
import com.dismal.distribuciones.modules.security.customer.service.CustomerMarketService;
import com.dismal.distribuciones.modules.security.repository.UserRepository;
import com.dismal.distribuciones.modules.security.util.PhoneNormalizer;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WholesaleApplicationService {

    private final WholesaleApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final AccountIdentityService accountIdentityService;
    private final CustomerMarketService customerMarketService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public WholesaleApplicationResponse submit(String email, WholesaleApplicationRequest request) {
        User user = accountIdentityService.requireUser(email);
        StorefrontCountry country = parseCountry(request.country());
        CustomerMarket market = customerMarketService.requireActive(
                customerMarketService.getOrCreate(user, country)
        );
        if (!user.isEmailVerified()) {
            throw new BadRequestException("Verifica tu correo antes de solicitar una cuenta mayorista.");
        }
        if (market.getCustomerType() == CustomerType.DISTRIBUTOR) {
            throw new BadRequestException("Tu cuenta ya está aprobada como mayorista.");
        }
        if (applicationRepository.existsByUserAndCountryAndStatus(
                user, country, WholesaleApplicationStatus.PENDING
        )) {
            throw new BadRequestException("Ya tienes una solicitud mayorista pendiente.");
        }
        String phone = normalizePhone(request.phone(), country);
        WholesaleApplication application = WholesaleApplication.builder()
                .user(user)
                .country(country)
                .businessName(request.businessName().trim())
                .taxId(request.taxId().trim())
                .phone(phone)
                .website(clean(request.website()))
                .notes(clean(request.notes()))
                .status(WholesaleApplicationStatus.PENDING)
                .build();
        user.setPhone(phone);
        userRepository.save(user);
        customerMarketService.updateBillingProfile(
                user,
                country,
                request.taxId().trim(),
                market.getBillingEmail()
        );
        eventPublisher.publishEvent(new CustomerChangedCrmSyncEvent(this, user.getId()));
        return toResponse(applicationRepository.save(application));
    }

    @Transactional(readOnly = true)
    public WholesaleApplicationResponse latestForUser(String email, StorefrontCountry country) {
        User user = accountIdentityService.requireUser(email);
        return applicationRepository.findFirstByUserAndCountryOrderByCreatedAtDesc(user, country)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<WholesaleApplicationResponse> list(WholesaleApplicationStatus status, int page, int size) {
        PageRequest request = PageRequest.of(Math.max(page, 0), Math.min(100, Math.max(size, 1)),
                Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<WholesaleApplication> applications = status == null
                ? applicationRepository.findAll(request)
                : applicationRepository.findAllByStatus(status, request);
        return applications.map(this::toResponse);
    }

    @Transactional
    public WholesaleApplicationResponse review(UUID id, WholesaleApplicationReviewRequest request, String reviewerEmail) {
        if (request.status() == WholesaleApplicationStatus.PENDING) {
            throw new BadRequestException("La revisión debe aprobar o rechazar la solicitud.");
        }
        WholesaleApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud mayorista no encontrada."));
        if (application.getStatus() != WholesaleApplicationStatus.PENDING) {
            throw new BadRequestException("La solicitud ya fue revisada.");
        }
        User reviewer = accountIdentityService.requireUser(reviewerEmail);
        application.setStatus(request.status());
        application.setReviewedBy(reviewer);
        application.setReviewNotes(clean(request.reviewNotes()));
        application.setReviewedAt(LocalDateTime.now());
        User customer = application.getUser();
        if (request.status() == WholesaleApplicationStatus.APPROVED) {
            CustomerMarket market = customerMarketService.getOrCreate(customer, application.getCountry());
            customerMarketService.updateCommercialProfile(
                    customer,
                    application.getCountry(),
                    CustomerType.DISTRIBUTOR,
                    application.getTaxId(),
                    market.getBillingEmail(),
                    market.isHasCredit(),
                    market.getCreditLimit(),
                    market.getCreditDays(),
                    true
            );
            eventPublisher.publishEvent(new CustomerChangedCrmSyncEvent(this, customer.getId()));
        }
        return toResponse(applicationRepository.save(application));
    }

    private WholesaleApplicationResponse toResponse(WholesaleApplication application) {
        User user = application.getUser();
        String customerName = ((user.getFirstname() != null ? user.getFirstname() : "") + " " +
                (user.getLastname() != null ? user.getLastname() : "")).trim();
        return new WholesaleApplicationResponse(
                application.getId(),
                user.getId(),
                user.getEmail(),
                customerName,
                application.getCountry(),
                application.getBusinessName(),
                application.getTaxId(),
                application.getPhone(),
                application.getWebsite(),
                application.getNotes(),
                application.getStatus(),
                application.getReviewedBy() != null ? application.getReviewedBy().getId() : null,
                application.getReviewNotes(),
                application.getReviewedAt(),
                application.getCreatedAt()
        );
    }

    private StorefrontCountry parseCountry(String country) {
        try {
            return StorefrontCountry.valueOf(country.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new BadRequestException("País no soportado.");
        }
    }

    private String normalizePhone(String phone, StorefrontCountry country) {
        String value = phone.trim();
        String digits = value.replaceAll("[^0-9+]", "");
        if (!digits.startsWith("+")) {
            String onlyDigits = digits.replaceAll("[^0-9]", "");
            if (country == StorefrontCountry.PE && onlyDigits.length() == 9) {
                value = "+51" + onlyDigits;
            } else if (country == StorefrontCountry.EC && onlyDigits.length() == 10 && onlyDigits.startsWith("0")) {
                value = "+593" + onlyDigits.substring(1);
            }
        }
        return PhoneNormalizer.normalize(value);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

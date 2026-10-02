package com.dismal.distribuciones.modules.marketing.service;

import com.dismal.distribuciones.exception.BadRequestException;
import com.dismal.distribuciones.exception.ResourceNotFoundException;
import com.dismal.distribuciones.modules.integrations.outbox.service.OutboxService;
import com.dismal.distribuciones.modules.marketing.domain.CampaignChannel;
import com.dismal.distribuciones.modules.marketing.domain.Campaign;
import com.dismal.distribuciones.modules.marketing.domain.CampaignStatus;
import com.dismal.distribuciones.modules.marketing.dto.CampaignCreateRequest;
import com.dismal.distribuciones.modules.marketing.dto.CampaignResponse;
import com.dismal.distribuciones.modules.marketing.dto.CampaignScheduleRequest;
import com.dismal.distribuciones.modules.marketing.dto.CampaignSendTestRequest;
import com.dismal.distribuciones.modules.marketing.dto.CampaignUpdateRequest;
import com.dismal.distribuciones.modules.marketing.repository.CampaignRepository;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.repository.UserRepository;
import com.dismal.distribuciones.modules.store.domain.Software;
import com.dismal.distribuciones.modules.store.repository.SoftwareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final SoftwareRepository softwareRepository;
    private final UserRepository userRepository;
    private final CampaignPublisherService campaignPublisherService;
    private final OutboxService outboxService;

    /**
     * Creates and saves a new campaign based on the request DTO.
     * The campaign is initially saved with a SCHEDULED status.
     * @param request The DTO containing the campaign details.
     * @return The persisted Campaign entity.
     */
    @Transactional
    public CampaignResponse createCampaign(CampaignCreateRequest request) {
        Software product = resolveProduct(request.productId());
        LocalDateTime scheduledAt = request.scheduledAt() != null ? request.scheduledAt() : LocalDateTime.now();
        Campaign campaign = Campaign.builder()
                .title(request.title())
                .messageBody(request.messageBody())
                .imageUrl(request.imageUrl())
                .targetRole(request.targetRole())
                .channel(request.channel())
                .product(product)
                .scheduledAt(scheduledAt)
                .status(CampaignStatus.DRAFT)
                .build();

        return toResponse(campaignRepository.save(campaign));
    }

    /**
     * Retrieves all campaigns.
     * @return A list of all Campaign entities.
     */
    public Page<CampaignResponse> listCampaigns(CampaignStatus status, int page, int size) {
        Specification<Campaign> spec = Specification.where(null);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        return campaignRepository.findAll(spec, pageRequest)
                .map(this::toResponse);
    }

    /**
     * Updates an existing campaign.
     * @param id The UUID of the campaign to update.
     * @param request The DTO containing the updated campaign details.
     * @return The updated Campaign entity.
     */
    @Transactional
    public CampaignResponse updateCampaign(UUID id, CampaignUpdateRequest request) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));

        Software product = resolveProduct(request.productId());

        campaign.setTitle(request.title());
        campaign.setMessageBody(request.messageBody());
        campaign.setImageUrl(request.imageUrl());
        campaign.setTargetRole(request.targetRole());
        campaign.setChannel(request.channel());
        campaign.setProduct(product);
        campaign.setScheduledAt(request.scheduledAt());
        campaign.setStatus(request.status());

        return toResponse(campaignRepository.save(campaign));
    }

    /**
     * Logically deletes a campaign by setting its status to CANCELLED.
     * @param id The UUID of the campaign to delete.
     */
    @Transactional
    public void deleteCampaign(UUID id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));
        campaign.setStatus(CampaignStatus.CANCELLED);
        campaignRepository.save(campaign);
    }

    @Transactional
    public CampaignResponse scheduleCampaign(UUID id, CampaignScheduleRequest request) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));
        LocalDateTime scheduledAt = request != null && request.scheduledAt() != null
                ? request.scheduledAt()
                : LocalDateTime.now();
        campaign.setScheduledAt(scheduledAt);
        campaign.setStatus(CampaignStatus.SCHEDULED);
        Campaign saved = campaignRepository.save(campaign);

        List<User> recipients = campaignPublisherService.getEligibleCustomers(campaign.getTargetRole());
        outboxService.enqueueCampaignScheduledEvent(saved, resolveChannel(saved), recipients);
        return toResponse(saved);
    }

    @Transactional
    public void sendTest(UUID id, CampaignSendTestRequest request) {
        if (request == null || request.clientId() == null) {
            throw new BadRequestException("clientId is required for test sends.");
        }
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));
        User client = userRepository.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + request.clientId()));
        CampaignChannel channel = request.channel() != null ? request.channel() : resolveChannel(campaign);
        outboxService.enqueueCampaignSentEvent(campaign, channel, List.of(client));
    }

    private Software resolveProduct(UUID productId) {
        if (productId == null) {
            return null;
        }
        return softwareRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Software not found with ID: " + productId));
    }

    private CampaignChannel resolveChannel(Campaign campaign) {
        return campaign.getChannel() != null ? campaign.getChannel() : CampaignChannel.EMAIL;
    }

    private CampaignResponse toResponse(Campaign campaign) {
        Software product = campaign.getProduct();
        return new CampaignResponse(
                campaign.getId(),
                campaign.getTitle(),
                campaign.getMessageBody(),
                campaign.getImageUrl(),
                campaign.getTargetRole(),
                campaign.getChannel(),
                product != null ? product.getId() : null,
                product != null ? product.getName() : null,
                campaign.getScheduledAt(),
                campaign.getStatus(),
                campaign.getCreatedAt(),
                campaign.getUpdatedAt()
        );
    }
}

package com.dismal.distribuciones.modules.marketing.service;

import com.dismal.distribuciones.modules.marketing.domain.Campaign;
import com.dismal.distribuciones.modules.marketing.domain.CampaignStatus;
import com.dismal.distribuciones.modules.marketing.repository.CampaignRepository;
import com.dismal.distribuciones.modules.integrations.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignExecutionService {

    private final CampaignRepository campaignRepository;
    private final CampaignPublisherService campaignPublisherService;
    private final OutboxService outboxService;

    /**
     * This scheduled task runs every 15 minutes to find and send due campaigns.
     * It is transactional, so if any part of the process fails for a campaign,
     * the status update will be rolled back, allowing it to be retried.
     */
    @Scheduled(fixedRate = 900000) // 15 minutes = 900,000 milliseconds
    @Transactional
    public void processAndSendScheduledCampaigns() {
        log.info("Running scheduled task to find and send campaigns...");

        List<Campaign> campaignsToSend = campaignRepository.findPendingCampaignsToSend(
                CampaignStatus.SCHEDULED,
                LocalDateTime.now()
        );

        if (campaignsToSend.isEmpty()) {
            log.info("No pending campaigns to send at this time.");
            return;
        }

        log.info("Found {} campaigns to send.", campaignsToSend.size());

        for (Campaign campaign : campaignsToSend) {
            try {
                var customers = campaignPublisherService.getEligibleCustomers(campaign.getTargetRole());
                outboxService.enqueueCampaignSentEvent(campaign, campaign.getChannel(), customers);

                // Mark as SENT after enqueuing to prevent re-sending in the next run
                campaign.setStatus(CampaignStatus.SENT);
                campaignRepository.save(campaign);
                log.info("Campaign '{}' successfully processed and marked as SENT.", campaign.getTitle());

            } catch (Exception e) {
                log.error("Failed to process campaign '{}' (ID: {}). Error: {}",
                        campaign.getTitle(), campaign.getId(), e.getMessage(), e);
                campaign.setStatus(CampaignStatus.FAILED);
                campaignRepository.save(campaign);
            }
        }
    }
}

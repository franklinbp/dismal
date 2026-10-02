package com.dismal.distribuciones.modules.marketing.domain;

public enum CampaignStatus {
    DRAFT,      // The campaign is being created and is not yet ready to be sent.
    SCHEDULED,  // The campaign is approved and scheduled for a future time.
    SENT,       // The campaign has been successfully sent.
    FAILED,     // The campaign failed to be dispatched.
    CANCELLED   // The campaign was cancelled before it could be sent.
}

package com.dismal.app.data.user

data class CrmIntegrationStatus(
    val enabled: Boolean,
    val configured: Boolean,
    val whatsappEnabled: Boolean,
    val reachable: Boolean,
    val endpoint: String,
    val message: String,
)

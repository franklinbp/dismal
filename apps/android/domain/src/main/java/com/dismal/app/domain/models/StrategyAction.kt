package com.dismal.app.domain.models

data class StrategyAction(
    val id: String,
    val productId: String?,
    val productName: String?,
    val source: String,
    val priority: String,
    val status: String,
    val recommendedChannel: String?,
    val title: String,
    val description: String?,
    val assignedToName: String?,
    val dueDate: String?,
)

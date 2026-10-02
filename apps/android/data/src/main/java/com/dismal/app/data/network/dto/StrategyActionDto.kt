package com.dismal.app.data.network.dto

import com.dismal.app.domain.models.StrategyAction
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StrategyActionDto(
    @Json(name = "id") val id: String,
    @Json(name = "productId") val productId: String? = null,
    @Json(name = "productName") val productName: String? = null,
    @Json(name = "source") val source: String,
    @Json(name = "priority") val priority: String,
    @Json(name = "status") val status: String,
    @Json(name = "recommendedChannel") val recommendedChannel: String? = null,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "assignedToName") val assignedToName: String? = null,
    @Json(name = "dueDate") val dueDate: String? = null,
)

fun StrategyActionDto.asDomainModel() =
    StrategyAction(
        id = id,
        productId = productId,
        productName = productName,
        source = source,
        priority = priority,
        status = status,
        recommendedChannel = recommendedChannel,
        title = title,
        description = description,
        assignedToName = assignedToName,
        dueDate = dueDate,
    )

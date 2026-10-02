package com.dismal.app.data.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ConfirmSaleRequestDto(
    @Json(name = "deliveryChannels") val deliveryChannels: List<String> = emptyList(),
)


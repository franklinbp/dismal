package com.dismal.app.data.repository

import com.dismal.app.data.common.AppResult
import com.dismal.app.data.common.safeApiCall
import com.dismal.app.data.network.ReportsApi
import com.dismal.app.domain.models.PriceListSendRequest
import com.dismal.app.domain.models.PriceListSendResult

class PriceListRepository(
    private val reportsApi: ReportsApi,
) {
    suspend fun sendPriceList(request: PriceListSendRequest): AppResult<PriceListSendResult> {
        return safeApiCall(
            errorPrefix = "No se pudo enviar la lista",
            emptyBodyMessage = "Respuesta vacia al enviar la lista",
            call = { reportsApi.sendPriceList(request) },
        ) { body ->
            body!!
        }
    }
}

package com.dismal.app.data.repository

import com.dismal.app.data.common.AppResult
import com.dismal.app.data.common.safeApiCall
import com.dismal.app.data.network.MarketingIntelligenceApi
import com.dismal.app.data.network.dto.StrategyActionStatusRequestDto
import com.dismal.app.data.network.dto.asDomainModel
import com.dismal.app.domain.models.MarketingAnalysis
import com.dismal.app.domain.models.StrategyAction

class MarketingIntelligenceRepository(
    private val marketingIntelligenceApi: MarketingIntelligenceApi,
) {
    suspend fun getAnalysis(): AppResult<List<MarketingAnalysis>> {
        return safeApiCall(
            errorPrefix = "No se pudo cargar la inteligencia comercial",
            call = { marketingIntelligenceApi.getAnalysis() },
        ) { body ->
            body.orEmpty().map { it.asDomainModel() }
        }
    }

    suspend fun getOpenActions(): AppResult<List<StrategyAction>> {
        val pendingResult =
            safeApiCall(
                errorPrefix = "No se pudo cargar acciones estrategicas",
                call = { marketingIntelligenceApi.getStrategyActions(status = "PENDIENTE") },
            ) { body ->
                body?.content.orEmpty().map { it.asDomainModel() }
            }
        if (pendingResult is AppResult.Error) {
            return pendingResult
        }

        val progressResult =
            safeApiCall(
                errorPrefix = "No se pudo cargar acciones estrategicas",
                call = { marketingIntelligenceApi.getStrategyActions(status = "EN_PROGRESO") },
            ) { body ->
                body?.content.orEmpty().map { it.asDomainModel() }
            }
        if (progressResult is AppResult.Error) {
            return progressResult
        }

        return AppResult.Success(
            (pendingResult as AppResult.Success).data + (progressResult as AppResult.Success).data,
        )
    }

    suspend fun updateActionStatus(
        actionId: String,
        status: String,
        resultNotes: String? = null,
    ): AppResult<StrategyAction> {
        return safeApiCall(
            errorPrefix = "No se pudo actualizar la accion estrategica",
            emptyBodyMessage = "El servidor no devolvio la accion actualizada",
            call = {
                marketingIntelligenceApi.updateStrategyActionStatus(
                    id = actionId,
                    request = StrategyActionStatusRequestDto(status = status, resultNotes = resultNotes),
                )
            },
        ) { body ->
            body!!.asDomainModel()
        }
    }
}

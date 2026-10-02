package com.dismal.app.data.repository

import com.dismal.app.data.common.AppResult
import com.dismal.app.data.common.safeApiCall
import com.dismal.app.data.network.DashboardApi
import com.dismal.app.data.network.dto.asDomainModel
import com.dismal.app.domain.models.DashboardSummary
import com.dismal.app.domain.models.DashboardTarget

class DashboardRepository(
    private val dashboardApi: DashboardApi,
) {
    suspend fun getSummary(): AppResult<DashboardSummary> {
        return safeApiCall(
            errorPrefix = "No se pudo cargar el resumen",
            emptyBodyMessage = "Respuesta vacia del dashboard",
            call = { dashboardApi.getSummary() },
        ) { body ->
            body!!.asDomainModel()
        }
    }

    suspend fun getTopTargets(size: Int = 10): AppResult<List<DashboardTarget>> {
        return safeApiCall(
            errorPrefix = "No se pudieron cargar las metas",
            call = { dashboardApi.getSalesTargets(size = size) },
        ) { body ->
            body?.content.orEmpty().map { it.asDomainModel() }
        }
    }
}

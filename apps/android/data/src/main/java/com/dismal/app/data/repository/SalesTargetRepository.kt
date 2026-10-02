package com.dismal.app.data.repository

import com.dismal.app.data.common.AppResult
import com.dismal.app.data.common.safeApiCall
import com.dismal.app.data.db.SalesTargetDao
import com.dismal.app.data.db.asDatabaseModel
import com.dismal.app.data.db.asDomainModel
import com.dismal.app.data.network.SalesTargetApi
import com.dismal.app.data.network.dto.asDomainModel
import com.dismal.app.domain.models.SalesTarget
import com.dismal.app.domain.models.SalesTargetSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SalesTargetRepository(
    private val salesTargetApi: SalesTargetApi,
    private val salesTargetDao: SalesTargetDao,
) {
    val targets: Flow<List<SalesTarget>> =
        salesTargetDao.getAllTargets().map { targets ->
            targets.map { it.asDomainModel() }
        }

    suspend fun refreshTargets(): AppResult<Unit> {
        return when (
            val result =
                safeApiCall(
                    errorPrefix = "Error al obtener metas",
                    call = { salesTargetApi.getSalesTargets() },
                ) { body ->
                    body.orEmpty().map { it.asDomainModel().asDatabaseModel() }
                }
        ) {
            is AppResult.Success -> {
                salesTargetDao.clearTargets()
                if (result.data.isNotEmpty()) {
                    salesTargetDao.insertTargets(result.data)
                }
                AppResult.Success(Unit)
            }
            is AppResult.Error -> result
        }
    }

    suspend fun getSummary(): AppResult<SalesTargetSummary> {
        return safeApiCall(
            errorPrefix = "Error al obtener resumen de metas",
            emptyBodyMessage = "Respuesta vacia del resumen de metas",
            call = { salesTargetApi.getSummary() },
        ) { body ->
            body!!.asDomainModel()
        }
    }
}

package com.dismal.app.data.repository

import com.dismal.app.data.common.AppResult
import com.dismal.app.data.common.formatHttpError
import com.dismal.app.data.common.retryWithBackoff
import com.dismal.app.data.db.LicenseDao
import com.dismal.app.data.db.asDatabaseModel
import com.dismal.app.data.db.asDomainModel
import com.dismal.app.data.network.InventoryApi
import com.dismal.app.data.network.dto.asDomainModel
import com.dismal.app.domain.models.License
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InventoryRepository(
    private val inventoryApi: InventoryApi,
    private val licenseDao: LicenseDao,
) {
    val licenses: Flow<List<License>> =
        licenseDao.getAllLicenses().map { licenses ->
            licenses.map { it.asDomainModel() }
        }

    suspend fun refreshLicenses(): AppResult<Unit> {
        return try {
            val collected = mutableListOf<License>()
            var page = 0
            val pageSize = 50
            while (true) {
                val response =
                    retryWithBackoff {
                        val attempt = inventoryApi.getLicenses(page = page, size = pageSize)
                        if (!attempt.isSuccessful) {
                            throw IllegalStateException("HTTP ${attempt.code()}")
                        }
                        attempt
                    }
                val payload = response.body()
                val batch = payload?.content ?: emptyList()
                if (batch.isEmpty()) break
                collected.addAll(batch.map { it.asDomainModel() })
                val totalPages = payload?.totalPages ?: 0
                if (page + 1 >= totalPages) break
                page += 1
            }
            licenseDao.clearLicenses()
            if (collected.isNotEmpty()) {
                licenseDao.insertLicenses(collected.map { it.asDatabaseModel() })
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error("Error al obtener licencias${e.message?.let { " ($it)" } ?: ""}", e)
        }
    }

    suspend fun getAvailableActivationCount(softwareId: String): AppResult<Int> {
        if (softwareId.isBlank()) {
            return AppResult.Error("Producto invalido para validar inventario.")
        }
        return try {
            var availableActivations = 0
            var page = 0
            val pageSize = 100
            while (true) {
                val response =
                    retryWithBackoff {
                        inventoryApi.getLicenses(
                            softwareId = softwareId,
                            status = "ACTIVE",
                            page = page,
                            size = pageSize,
                        )
                    }
                if (!response.isSuccessful) {
                    return AppResult.Error(formatHttpError("No se pudo validar el inventario", response))
                }
                val payload = response.body()
                val batch = payload?.content ?: emptyList()
                availableActivations +=
                    batch.sumOf { license ->
                        if (license.available) {
                            (license.maxActivations - license.usedActivations).coerceAtLeast(0)
                        } else {
                            0
                        }
                    }
                val totalPages = payload?.totalPages ?: 0
                if (batch.isEmpty() || page + 1 >= totalPages) break
                page += 1
            }
            AppResult.Success(availableActivations)
        } catch (e: Exception) {
            AppResult.Error("No se pudo validar el inventario${e.message?.let { " ($it)" } ?: ""}", e)
        }
    }
}

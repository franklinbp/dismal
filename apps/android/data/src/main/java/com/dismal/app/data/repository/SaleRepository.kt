package com.dismal.app.data.repository

import com.dismal.app.data.common.AppResult
import com.dismal.app.data.common.formatHttpError
import com.dismal.app.data.common.retryWithBackoff
import com.dismal.app.data.db.Converters
import com.dismal.app.data.db.SaleDao
import com.dismal.app.data.db.asDatabaseModel
import com.dismal.app.data.db.asDomainModel
import com.dismal.app.data.network.PaymentsApi
import com.dismal.app.data.network.SalesApi
import com.dismal.app.data.network.dto.ConfirmSaleRequestDto
import com.dismal.app.domain.models.CreateSaleRequest
import com.dismal.app.domain.models.PaymentRequest
import com.dismal.app.domain.models.Sale
import com.dismal.app.domain.models.SaleItem
import com.dismal.app.domain.models.SaleItemRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class SaleRepository(
    private val saleDao: SaleDao,
    private val salesApi: SalesApi,
    private val paymentsApi: PaymentsApi,
    private val syncOutboxRepository: SyncOutboxRepository,
) {
    val sales: Flow<List<Sale>> =
        saleDao.getAllSales().map {
            it.map { entity ->
                entity.asDomainModel()
            }
        }

    suspend fun refreshSales(): AppResult<Unit> {
        return try {
            val response = retryWithBackoff { salesApi.getSales() }
            if (!response.isSuccessful) {
                return AppResult.Error(formatHttpError("Error al obtener ventas", response))
            }
            val networkSales = response.body()?.content ?: emptyList()
            val pendingIds = saleDao.getPendingSales().map { it.id }.toSet()
            val unsyncedIds = saleDao.getUnsyncedSaleIds().toSet()
            val conflictIds = networkSales.map { it.id }.filter { it in pendingIds }
            for (conflictId in conflictIds) {
                val localSale = saleDao.getSaleById(conflictId)
                val localUpdatedAt = localSale?.updatedAt?.let { parseTimestamp(it) } ?: 0L
                val remoteUpdatedAt = networkSales.find { it.id == conflictId }?.updatedAt?.let { parseTimestamp(it) } ?: 0L
                if (localUpdatedAt > 0L && remoteUpdatedAt > 0L && remoteUpdatedAt > localUpdatedAt) {
                    saleDao.updateSyncState(
                        conflictId,
                        com.dismal.app.data.db.SyncStatus.CONFLICT.value,
                        "Conflicto con la nube",
                    )
                }
            }
            val filtered = networkSales.filterNot { it.id in unsyncedIds }
            if (filtered.isNotEmpty()) {
                saleDao.insertSales(filtered.map { sale -> sale.asDatabaseModel(isSynced = true) })
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error("Error al obtener ventas${e.message?.let { " ($it)" } ?: ""}", e)
        }
    }

    suspend fun addSaleRemote(
        clientId: String,
        softwareId: String,
        quantity: Int,
        unitPrice: Double,
        saleType: String,
        country: String? = null,
    ): AppResult<Unit> {
        return when (
            val result = createSaleRemote(clientId, softwareId, quantity, unitPrice, saleType, country)
        ) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Error -> AppResult.Error(result.message, result.cause)
        }
    }

    suspend fun createSaleRemote(
        clientId: String,
        softwareId: String,
        quantity: Int,
        unitPrice: Double,
        saleType: String,
        country: String? = null,
    ): AppResult<Sale> {
        val request =
            CreateSaleRequest(
                clientId = clientId,
                saleType = saleType,
                country = country,
                items =
                    listOf(
                        SaleItemRequest(
                            softwareId = softwareId,
                            quantity = quantity,
                            unitPrice = unitPrice,
                        ),
                    ),
            )
        return try {
            val response = retryWithBackoff { salesApi.createSale(request) }
            if (!response.isSuccessful) {
                return AppResult.Error(formatHttpError("No se pudo crear la venta", response))
            }
            val sale =
                response.body()
                    ?: return AppResult.Error("Respuesta vacia al crear venta")
            saleDao.insertSales(listOf(sale.asDatabaseModel(isSynced = true)))
            AppResult.Success(sale)
        } catch (e: Exception) {
            AppResult.Error("Error al añadir venta${e.message?.let { " ($it)" } ?: ""}", e)
        }
    }

    suspend fun confirmSaleRemote(
        saleId: String,
        deliveryChannels: List<String> = emptyList(),
    ): AppResult<Sale> {
        return try {
            val response =
                retryWithBackoff {
                    salesApi.confirmSale(saleId, ConfirmSaleRequestDto(deliveryChannels))
                }
            if (!response.isSuccessful) {
                return AppResult.Error(formatHttpError("No se pudo confirmar la venta", response))
            }
            val sale =
                response.body()
                    ?: return AppResult.Error("Respuesta vacia al confirmar venta")
            saleDao.insertSales(listOf(sale.asDatabaseModel(isSynced = true)))
            AppResult.Success(sale)
        } catch (e: Exception) {
            AppResult.Error("Error al confirmar venta${e.message?.let { " ($it)" } ?: ""}", e)
        }
    }

    suspend fun createAndConfirmSaleRemote(
        clientId: String,
        softwareId: String,
        quantity: Int,
        unitPrice: Double,
        saleType: String,
        country: String? = null,
        deliveryChannels: List<String> = emptyList(),
        registerFullPayment: Boolean = false,
        paymentMethod: String = "CASH",
        paymentReference: String? = "MOBILE_APP",
    ): AppResult<Sale> {
        return when (
            val created =
                createSaleRemote(
                    clientId = clientId,
                    softwareId = softwareId,
                    quantity = quantity,
                    unitPrice = unitPrice,
                    saleType = saleType,
                    country = country,
                )
        ) {
            is AppResult.Success -> {
                when (val confirmed = confirmSaleRemote(created.data.id, deliveryChannels)) {
                    is AppResult.Success -> {
                        if (registerFullPayment) {
                            registerFullPaymentRemote(
                                sale = confirmed.data,
                                method = paymentMethod,
                                reference = paymentReference,
                            )
                        } else {
                            confirmed
                        }
                    }
                    is AppResult.Error ->
                        AppResult.Error(
                            formatConfirmationFailure(confirmed.message, quantity),
                            confirmed.cause,
                        )
                }
            }
            is AppResult.Error -> AppResult.Error(created.message, created.cause)
        }
    }

    private fun formatConfirmationFailure(
        message: String,
        quantity: Int,
    ): String {
        val detail = extractServerMessage(message)
        return if (detail.contains("Not enough activations", ignoreCase = true)) {
            "La venta ya fue creada como borrador, pero no pudo confirmarse. No hay activaciones suficientes para entregar $quantity unidad(es). Agrega inventario al producto y vuelve a confirmar la venta desde el sistema."
        } else {
            "La venta ya fue creada como borrador, pero no pudo confirmarse. $detail"
        }
    }

    private fun extractServerMessage(message: String): String {
        val jsonMessage = Regex(""""message"\s*:\s*"([^"]+)"""").find(message)?.groupValues?.getOrNull(1)
        val detail = jsonMessage ?: message.substringAfter("):", message)
        return detail
            .replace("\\\"", "\"")
            .trim()
            .trimEnd('.')
            .ifBlank { "Revisa la venta e intentalo nuevamente." }
    }

    private suspend fun registerFullPaymentRemote(
        sale: Sale,
        method: String,
        reference: String?,
    ): AppResult<Sale> {
        val amount = if (sale.balance > 0.0) sale.balance else sale.total
        if (amount <= 0.0) return AppResult.Success(sale)
        return try {
            val response =
                retryWithBackoff {
                    paymentsApi.addPayment(
                        PaymentRequest(
                            saleId = sale.id,
                            amount = amount,
                            method = method,
                            reference = reference,
                        ),
                    )
                }
            if (!response.isSuccessful) {
                return AppResult.Error(formatHttpError("Venta confirmada, pero no se pudo registrar el pago", response))
            }
            val paidSale =
                response.body()
                    ?: return AppResult.Error("Venta confirmada, pero el servidor no devolvió el pago registrado")
            saleDao.insertSales(listOf(paidSale.asDatabaseModel(isSynced = true)))
            AppResult.Success(paidSale)
        } catch (e: Exception) {
            AppResult.Error("Venta confirmada, pero no se pudo registrar el pago${e.message?.let { " ($it)" } ?: ""}", e)
        }
    }

    suspend fun registerPaymentRemote(
        saleId: String,
        amount: Double,
        method: String = "CASH",
        reference: String? = "MOBILE_COLLECTION",
    ): AppResult<Sale> {
        if (amount <= 0.0) {
            return AppResult.Error("El monto debe ser mayor a cero.")
        }
        return try {
            val response =
                retryWithBackoff {
                    paymentsApi.addPayment(
                        PaymentRequest(
                            saleId = saleId,
                            amount = amount,
                            method = method,
                            reference = reference,
                        ),
                    )
                }
            if (!response.isSuccessful) {
                return AppResult.Error(formatHttpError("No se pudo registrar el cobro", response))
            }
            val sale =
                response.body()
                    ?: return AppResult.Error("Respuesta vacia al registrar cobro")
            saleDao.insertSales(listOf(sale.asDatabaseModel(isSynced = true)))
            AppResult.Success(sale)
        } catch (e: Exception) {
            AppResult.Error("No se pudo registrar el cobro${e.message?.let { " ($it)" } ?: ""}", e)
        }
    }

    suspend fun addSaleLocal(
        clientId: String,
        softwareId: String,
        quantity: Int,
        unitPrice: Double,
        saleType: String,
        country: String = "EC",
    ) {
        val total = unitPrice * quantity
        val now = System.currentTimeMillis().toString()
        val sale =
            Sale(
                id = UUID.randomUUID().toString(),
                clientId = clientId,
                saleType = saleType,
                country = country,
                status = "DRAFT",
                total = total,
                paid = 0.0,
                balance = total,
                createdAt = now,
                updatedAt = now,
                items =
                    listOf(
                        SaleItem(
                            id = null,
                            softwareId = softwareId,
                            softwareName = null,
                            quantity = quantity,
                            unitPrice = unitPrice,
                            subtotal = total,
                        ),
                    ),
            )
        saleDao.insertSales(
            listOf(
                sale.asDatabaseModel(
                    isSynced = false,
                    syncStatus = com.dismal.app.data.db.SyncStatus.PENDING.value,
                ),
            ),
        )
        syncOutboxRepository.enqueueSaleCreate(sale.id)
    }

    suspend fun syncSales(): AppResult<Unit> {
        return try {
            val pendingSales = saleDao.getPendingSales()
            if (pendingSales.isEmpty()) return AppResult.Success(Unit)
            for (sale in pendingSales) {
                syncSaleById(sale.id)
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error("Error al sincronizar ventas${e.message?.let { " ($it)" } ?: ""}", e)
        }
    }

    suspend fun syncSaleById(saleId: String): AppResult<Unit> {
        val sale =
            saleDao.getSaleById(saleId)
                ?: return AppResult.Error("Venta no encontrada para sincronizar.")
        val items = Converters.toSaleItemList(sale.itemsJson) ?: emptyList()
        val firstItem = items.firstOrNull()
        val softwareId = firstItem?.softwareId
        if (softwareId.isNullOrBlank()) {
            saleDao.updateSyncState(
                sale.id,
                com.dismal.app.data.db.SyncStatus.ERROR.value,
                "Venta pendiente sin softwareId válido.",
            )
            return AppResult.Error("Venta pendiente sin softwareId válido.")
        }
        val quantity = firstItem.quantity
        val unitPrice = firstItem.unitPrice
        val request =
            CreateSaleRequest(
                clientId = sale.clientId,
                saleType = sale.saleType,
                country = sale.country,
                items =
                    listOf(
                        SaleItemRequest(
                            softwareId = softwareId,
                            quantity = quantity,
                            unitPrice = unitPrice,
                        ),
                    ),
            )
        return try {
            val response =
                retryWithBackoff {
                    val attempt = salesApi.createSale(request)
                    if (!attempt.isSuccessful && attempt.code() !in listOf(400, 409)) {
                        throw IllegalStateException("HTTP ${attempt.code()}")
                    }
                    attempt
                }
            if (response.isSuccessful) {
                response.body()?.let { created ->
                    saleDao.insertSales(
                        listOf(
                            created.asDatabaseModel(
                                isSynced = true,
                                syncStatus = com.dismal.app.data.db.SyncStatus.SYNCED.value,
                            ),
                        ),
                    )
                    if (created.id != sale.id) {
                        saleDao.deleteSales(listOf(sale.id))
                    } else {
                        saleDao.markSalesSynced(listOf(sale.id))
                        saleDao.updateSyncState(
                            sale.id,
                            com.dismal.app.data.db.SyncStatus.SYNCED.value,
                            null,
                        )
                    }
                }
                syncOutboxRepository.deleteByEntity(com.dismal.app.data.db.SyncOutboxType.SALE, sale.id)
                AppResult.Success(Unit)
            } else if (response.code() == 409) {
                val errorBody = response.errorBody()?.string()?.trim()
                saleDao.updateSyncState(
                    sale.id,
                    com.dismal.app.data.db.SyncStatus.CONFLICT.value,
                    errorBody?.take(200) ?: "Conflicto con la nube",
                )
                AppResult.Error("Conflicto con la nube", IllegalStateException(errorBody ?: "HTTP 409"))
            } else {
                val errorBody = response.errorBody()?.string()?.trim()
                saleDao.updateSyncState(
                    sale.id,
                    com.dismal.app.data.db.SyncStatus.ERROR.value,
                    errorBody?.take(200),
                )
                AppResult.Error("Error al sincronizar venta", IllegalStateException(errorBody ?: "HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            saleDao.updateSyncState(
                sale.id,
                com.dismal.app.data.db.SyncStatus.ERROR.value,
                e.message?.take(200),
            )
            AppResult.Error("Error al sincronizar venta${e.message?.let { " ($it)" } ?: ""}", e)
        }
    }

    suspend fun hasSyncErrors(): Boolean {
        return saleDao.countSyncErrors() > 0
    }

    suspend fun countSyncErrors(): Int {
        return saleDao.countSyncErrors()
    }

    suspend fun cancelSale(saleId: String) {
        saleDao.deleteSales(listOf(saleId))
        syncOutboxRepository.deleteByEntity(com.dismal.app.data.db.SyncOutboxType.SALE, saleId)
    }

    suspend fun resolveConflictUseServer(saleId: String) {
        saleDao.deleteSales(listOf(saleId))
        syncOutboxRepository.deleteByEntity(com.dismal.app.data.db.SyncOutboxType.SALE, saleId)
    }

    private fun parseTimestamp(value: String): Long {
        value.toLongOrNull()?.let { return it }
        val formats =
            listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
            )
        for (pattern in formats) {
            val format =
                SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
            val parsed = runCatching { format.parse(value) }.getOrNull()
            if (parsed != null) {
                return parsed.time
            }
        }
        return 0L
    }
}

package com.dismal.app.data.repository

import android.util.Log
import com.dismal.app.data.auth.SessionManager
import com.dismal.app.data.common.AppResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository
    @Inject
    constructor(
        private val storeRepository: StoreRepository,
        private val customerRepository: CustomerRepository,
        private val saleRepository: SaleRepository,
        private val inventoryRepository: InventoryRepository,
        private val salesTargetRepository: SalesTargetRepository,
        private val syncOutboxRepository: SyncOutboxRepository,
        private val sessionManager: SessionManager,
    ) {
        suspend fun syncAll(): AppResult<Unit> {
            return try {
                if (!sessionManager.hasToken()) {
                    val pendingCount = syncOutboxRepository.countPending()
                    val errorCount = syncOutboxRepository.countErrors()
                    sessionManager.setLastSync("NO_AUTH", "Inicia sesion para sincronizar.", pendingCount, errorCount)
                    return AppResult.Error("Inicia sesion para sincronizar.")
                }
                val outboxPushResult = pushOutbox()

                val productPullResult = storeRepository.refreshProducts()
                if (productPullResult is AppResult.Error) {
                    return productPullResult
                }

                val licensePullResult = inventoryRepository.refreshLicenses()
                if (licensePullResult is AppResult.Error) {
                    return licensePullResult
                }

                val customerPullResult = customerRepository.refreshCustomers()
                if (customerPullResult is AppResult.Error) {
                    return customerPullResult
                }

                val targetPullResult = salesTargetRepository.refreshTargets()
                if (targetPullResult is AppResult.Error) {
                    return targetPullResult
                }

                val salePullResult = saleRepository.refreshSales()
                if (salePullResult is AppResult.Error) {
                    return salePullResult
                }

                val hasSaleErrors = saleRepository.hasSyncErrors()
                val hasCustomerErrors = customerRepository.hasSyncErrors()
                val pendingCount = syncOutboxRepository.countPending()
                val errorCount = syncOutboxRepository.countErrors() + saleRepository.countSyncErrors() + customerRepository.countSyncErrors()
                Log.i(TAG, "sync done pending=$pendingCount errors=$errorCount")

                if (hasSaleErrors || hasCustomerErrors || outboxPushResult is AppResult.Error) {
                    sessionManager.setLastSync("ERROR", "Operaciones con errores", pendingCount, errorCount)
                    return AppResult.Error("Hay operaciones con errores de sincronizacion. Revisa el listado.")
                }

                sessionManager.setLastSync("OK", "Sincronizacion completa", pendingCount, errorCount)
                AppResult.Success(Unit)
            } catch (e: Exception) {
                sessionManager.setLastSync("ERROR", e.message, 0, 0)
                AppResult.Error("Error during synchronization: ${e.message}", e)
            }
        }

        private suspend fun pushOutbox(): AppResult<Unit> {
            val pending = syncOutboxRepository.getPending()
            if (pending.isEmpty()) {
                val customerFallback = customerRepository.syncCustomers()
                if (customerFallback is AppResult.Error) {
                    return customerFallback
                }
                val saleFallback = saleRepository.syncSales()
                if (saleFallback is AppResult.Error) {
                    return saleFallback
                }
                return AppResult.Success(Unit)
            }
            var hasErrors = false
            for (entry in pending) {
                val result =
                    when (entry.entityType) {
                        com.dismal.app.data.db.SyncOutboxType.SALE -> saleRepository.syncSaleById(entry.entityId)
                        com.dismal.app.data.db.SyncOutboxType.CUSTOMER -> customerRepository.syncCustomerById(entry.entityId)
                        else -> AppResult.Error("Tipo de outbox no soportado: ${entry.entityType}")
                    }
                when (result) {
                    is AppResult.Success -> syncOutboxRepository.markSent(entry.id)
                    is AppResult.Error -> {
                        hasErrors = true
                        syncOutboxRepository.markError(entry.id, result.message)
                    }
                }
            }
            syncOutboxRepository.cleanupSent()
            return if (hasErrors) {
                AppResult.Error("Hay operaciones pendientes con errores de sincronizacion.")
            } else {
                AppResult.Success(Unit)
            }
        }

        private companion object {
            private const val TAG = "SyncRepository"
        }
    }

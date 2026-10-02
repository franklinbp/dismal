package com.dismal.app.data.repository

import com.dismal.app.data.common.AppResult
import com.dismal.app.data.common.retryWithBackoff
import com.dismal.app.data.db.CustomerDao
import com.dismal.app.data.db.CustomerEntity
import com.dismal.app.data.db.asDatabaseModel
import com.dismal.app.data.db.asDomainModel
import com.dismal.app.data.network.CustomerApi
import com.dismal.app.data.network.dto.AdminUserCreateRequestDto
import com.dismal.app.data.network.dto.asCustomer
import com.dismal.app.domain.models.Customer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class CustomerRepository(
    private val customerDao: CustomerDao,
    private val customerApi: CustomerApi,
    private val syncOutboxRepository: SyncOutboxRepository,
) {
    val customers: Flow<List<Customer>> =
        customerDao.getAllCustomers().map {
            it.map { entity ->
                entity.asDomainModel()
            }
        }

    suspend fun refreshCustomers(): AppResult<Unit> {
        return try {
            val adminCustomers = mutableListOf<com.dismal.app.data.network.dto.AdminUserResponseDto>()
            var currentPage = 0
            var totalPages = 1
            do {
                val response =
                    retryWithBackoff {
                        val attempt = customerApi.getAdminClients(page = currentPage, size = 100)
                        if (!attempt.isSuccessful) {
                            throw IllegalStateException("HTTP ${attempt.code()}")
                        }
                        attempt
                    }
                val page = response.body()
                adminCustomers += page?.content ?: emptyList()
                totalPages = page?.totalPages ?: 1
                currentPage += 1
            } while (currentPage < totalPages && currentPage < 50)
            val customers = adminCustomers.map { it.asCustomer() }
            val pendingIds = customerDao.getPendingCustomers().map { it.id }.toSet()
            val unsyncedIds = customerDao.getUnsyncedCustomerIds().toSet()
            val conflictIds = customers.map { it.id }.filter { it in pendingIds }
            for (conflictId in conflictIds) {
                val localCustomer = customerDao.getCustomerById(conflictId)
                val localUpdatedAt = localCustomer?.updatedAt?.let { parseTimestamp(it) } ?: 0L
                val remoteUpdatedAt = customers.find { it.id == conflictId }?.updatedAt?.let { parseTimestamp(it) } ?: 0L
                if (localUpdatedAt > 0L && remoteUpdatedAt > 0L && remoteUpdatedAt > localUpdatedAt) {
                    customerDao.updateSyncState(
                        conflictId,
                        com.dismal.app.data.db.SyncStatus.CONFLICT.value,
                        "Conflicto con la nube",
                    )
                }
            }
            val filtered = customers.filterNot { it.id in unsyncedIds }
            if (filtered.isNotEmpty()) {
                customerDao.insertCustomers(filtered.map { customer -> customer.asDatabaseModel(isSynced = true) })
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error("Error al obtener clientes${e.message?.let { " ($it)" } ?: ""}", e)
        }
    }

    suspend fun addCustomer(
        name: String,
        email: String,
        phone: String?,
    ) {
        val customerEntity =
            CustomerEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                email = email,
                phone = phone,
                updatedAt = System.currentTimeMillis().toString(),
                isSynced = false,
                syncStatus = com.dismal.app.data.db.SyncStatus.PENDING.value,
            )
        customerDao.insertCustomers(listOf(customerEntity))
        syncOutboxRepository.enqueueCustomerCreate(customerEntity.id)
    }

    suspend fun createCustomerRemote(
        name: String,
        email: String,
        phone: String?,
        password: String?,
    ): AppResult<Customer> {
        val cleanName = name.trim()
        val parts = cleanName.split(" ").filter { it.isNotBlank() }
        val firstname = parts.firstOrNull() ?: cleanName
        val lastname = parts.drop(1).joinToString(" ")
        return try {
            val response =
                retryWithBackoff {
                    val attempt =
                        customerApi.createAdminClient(
                            AdminUserCreateRequestDto(
                                firstname = firstname,
                                lastname = lastname,
                                email = email.trim(),
                                phone = phone?.trim()?.takeIf { it.isNotBlank() },
                                password = password?.trim()?.takeIf { it.isNotBlank() },
                            ),
                        )
                    if (!attempt.isSuccessful) {
                        throw IllegalStateException("HTTP ${attempt.code()}")
                    }
                    attempt
                }
            val body = response.body() ?: return AppResult.Error("Respuesta vacia al crear cliente")
            val customer = body.asCustomer()
            customerDao.insertCustomers(listOf(customer.asDatabaseModel(isSynced = true)))
            AppResult.Success(customer)
        } catch (e: Exception) {
            AppResult.Error("No se pudo crear el cliente${e.message?.let { " ($it)" } ?: ""}", e)
        }
    }

    suspend fun syncCustomers(): AppResult<Unit> {
        return try {
            val pendingCustomers = customerDao.getPendingCustomers()
            if (pendingCustomers.isEmpty()) return AppResult.Success(Unit)
            var hasErrors = false
            for (customer in pendingCustomers) {
                val result = syncCustomerById(customer.id)
                if (result is AppResult.Error) {
                    hasErrors = true
                }
            }
            if (hasErrors) {
                AppResult.Error("Algunos clientes no pudieron sincronizarse.")
            } else {
                AppResult.Success(Unit)
            }
        } catch (e: Exception) {
            AppResult.Error("Error al sincronizar clientes${e.message?.let { " ($it)" } ?: ""}", e)
        }
    }

    suspend fun syncCustomerById(customerId: String): AppResult<Unit> {
        val customer =
            customerDao.getCustomerById(customerId)
                ?: return AppResult.Error("Cliente no encontrado para sincronizar.")
        return try {
            val response =
                retryWithBackoff {
                    val attempt = customerApi.syncCustomers(listOf(customer.asDomainModel()))
                    if (!attempt.isSuccessful && attempt.code() != 409) {
                        throw IllegalStateException("HTTP ${attempt.code()}")
                    }
                    attempt
                }
            if (response.code() == 409) {
                customerDao.updateSyncState(
                    customerId,
                    com.dismal.app.data.db.SyncStatus.CONFLICT.value,
                    "Conflicto con la nube",
                )
                return AppResult.Error("Conflicto con la nube")
            }
            val syncedCustomers = response.body() ?: emptyList()
            if (syncedCustomers.isEmpty()) {
                customerDao.updateSyncState(
                    customerId,
                    com.dismal.app.data.db.SyncStatus.ERROR.value,
                    "Respuesta vacia del servidor",
                )
                return AppResult.Error("Respuesta vacia del servidor")
            }
            customerDao.insertCustomers(syncedCustomers.map { it.asDatabaseModel(isSynced = true) })
            val syncedIds = syncedCustomers.map { it.id }
            if (syncedIds.isNotEmpty()) {
                customerDao.markCustomersSynced(syncedIds)
            }
            customerDao.updateSyncState(
                customerId,
                com.dismal.app.data.db.SyncStatus.SYNCED.value,
                null,
            )
            syncOutboxRepository.deleteByEntity(com.dismal.app.data.db.SyncOutboxType.CUSTOMER, customerId)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            customerDao.updateSyncState(
                customerId,
                com.dismal.app.data.db.SyncStatus.ERROR.value,
                e.message?.take(200),
            )
            AppResult.Error("Error al sincronizar cliente${e.message?.let { " ($it)" } ?: ""}", e)
        }
    }

    suspend fun cancelCustomer(customerId: String) {
        customerDao.deleteCustomerById(customerId)
        syncOutboxRepository.deleteByEntity(com.dismal.app.data.db.SyncOutboxType.CUSTOMER, customerId)
    }

    suspend fun resolveConflictUseServer(customerId: String) {
        customerDao.deleteCustomerById(customerId)
        syncOutboxRepository.deleteByEntity(com.dismal.app.data.db.SyncOutboxType.CUSTOMER, customerId)
    }

    suspend fun registerPaymentLocal(customerId: String, amount: Double) {
        val customer = customerDao.getCustomerById(customerId) ?: return
        val currentBalance = customer.saldoActual ?: 0.0
        val newBalance = (currentBalance - amount).coerceAtLeast(0.0)
        customerDao.updateSaldoAndSyncState(
            customerId,
            newBalance,
            com.dismal.app.data.db.SyncStatus.PENDING.value,
            System.currentTimeMillis().toString(),
        )
    }

    suspend fun hasSyncErrors(): Boolean {
        return customerDao.countSyncErrors() > 0
    }

    suspend fun countSyncErrors(): Int {
        return customerDao.countSyncErrors()
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

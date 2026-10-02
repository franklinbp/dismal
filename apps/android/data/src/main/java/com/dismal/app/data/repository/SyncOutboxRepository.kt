package com.dismal.app.data.repository

import com.dismal.app.data.db.SyncOutboxAction
import com.dismal.app.data.db.SyncOutboxDao
import com.dismal.app.data.db.SyncOutboxEntity
import com.dismal.app.data.db.SyncOutboxStatus
import com.dismal.app.data.db.SyncOutboxType
import java.util.UUID

class SyncOutboxRepository(
    private val syncOutboxDao: SyncOutboxDao,
) {
    suspend fun enqueueSaleCreate(saleId: String) {
        enqueue(SyncOutboxType.SALE, saleId, SyncOutboxAction.CREATE)
    }

    suspend fun enqueueCustomerCreate(customerId: String) {
        enqueue(SyncOutboxType.CUSTOMER, customerId, SyncOutboxAction.CREATE)
    }

    suspend fun getPending(): List<SyncOutboxEntity> = syncOutboxDao.getPending()

    suspend fun markSent(id: String) {
        syncOutboxDao.updateStatus(id, SyncOutboxStatus.SENT, null, System.currentTimeMillis())
    }

    suspend fun markError(
        id: String,
        message: String?,
    ) {
        syncOutboxDao.updateStatus(id, SyncOutboxStatus.ERROR, message, System.currentTimeMillis())
    }

    suspend fun deleteByEntity(
        entityType: String,
        entityId: String,
    ) {
        syncOutboxDao.deleteByEntity(entityType, entityId)
    }

    suspend fun cleanupSent() {
        syncOutboxDao.deleteSent()
    }

    suspend fun countPending(): Int = syncOutboxDao.countPending()

    suspend fun countErrors(): Int = syncOutboxDao.countErrors()

    private suspend fun enqueue(
        entityType: String,
        entityId: String,
        action: String,
    ) {
        val now = System.currentTimeMillis()
        syncOutboxDao.insert(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                entityType = entityType,
                entityId = entityId,
                action = action,
                status = SyncOutboxStatus.PENDING,
                errorMessage = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }
}

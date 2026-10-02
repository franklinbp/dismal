package com.dismal.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncOutboxDao {
    @Query("SELECT * FROM sync_outbox WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPending(): List<SyncOutboxEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SyncOutboxEntity)

    @Query("UPDATE sync_outbox SET status = :status, errorMessage = :errorMessage, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(
        id: String,
        status: String,
        errorMessage: String?,
        updatedAt: Long,
    )

    @Query("DELETE FROM sync_outbox WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM sync_outbox WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun deleteByEntity(
        entityType: String,
        entityId: String,
    )

    @Query("DELETE FROM sync_outbox WHERE status = 'SENT'")
    suspend fun deleteSent()

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE status = 'PENDING'")
    suspend fun countPending(): Int

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE status = 'ERROR'")
    suspend fun countErrors(): Int
}

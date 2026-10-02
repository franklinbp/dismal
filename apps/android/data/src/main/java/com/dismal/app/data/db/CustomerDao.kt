package com.dismal.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun countCustomers(): Int

    @Query("SELECT * FROM customers WHERE isSynced = 0")
    suspend fun getUnsyncedCustomers(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE syncStatus = 'PENDING'")
    suspend fun getPendingCustomers(): List<CustomerEntity>

    @Query("SELECT id FROM customers WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedCustomerIds(): List<String>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Query("UPDATE customers SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markCustomersSynced(ids: List<String>)

    @Query("UPDATE customers SET syncStatus = :status, syncErrorMessage = :errorMessage WHERE id = :id")
    suspend fun updateSyncState(
        id: String,
        status: String,
        errorMessage: String? = null,
    )

    @Query(
        """
        UPDATE customers
        SET saldoActual = :saldoActual,
            syncStatus = :status,
            syncErrorMessage = NULL,
            updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateSaldoAndSyncState(id: String, saldoActual: Double, status: String, updatedAt: String)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: String)

    @Query("SELECT COUNT(*) FROM customers WHERE syncStatus IN ('ERROR', 'CONFLICT')")
    suspend fun countSyncErrors(): Int
}

package com.dismal.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales")
    fun getAllSales(): Flow<List<SaleEntity>>

    @Query("SELECT COUNT(*) FROM sales")
    suspend fun countSales(): Int

    @Query("SELECT * FROM sales WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSales(): List<SaleEntity>

    @Query("SELECT id FROM sales WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedSaleIds(): List<String>

    @Query("SELECT * FROM sales WHERE id = :id LIMIT 1")
    suspend fun getSaleById(id: String): SaleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSales(sales: List<SaleEntity>)

    @Query("UPDATE sales SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markSalesSynced(ids: List<String>)

    @Query("DELETE FROM sales WHERE id IN (:ids)")
    suspend fun deleteSales(ids: List<String>)

    @Query("UPDATE sales SET syncStatus = :status, syncErrorMessage = :errorMessage WHERE id = :id")
    suspend fun updateSyncState(
        id: String,
        status: String,
        errorMessage: String? = null,
    )

    @Query("SELECT COUNT(*) FROM sales WHERE syncStatus IN ('ERROR', 'CONFLICT')")
    suspend fun countSyncErrors(): Int
}

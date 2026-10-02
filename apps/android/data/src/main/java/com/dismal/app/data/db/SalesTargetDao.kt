package com.dismal.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SalesTargetDao {
    @Query("SELECT * FROM sales_targets")
    fun getAllTargets(): Flow<List<SalesTargetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTargets(targets: List<SalesTargetEntity>)

    @Query("DELETE FROM sales_targets")
    suspend fun clearTargets()
}

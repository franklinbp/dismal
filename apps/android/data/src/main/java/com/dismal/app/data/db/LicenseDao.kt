package com.dismal.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LicenseDao {
    @Query("SELECT * FROM licenses")
    fun getAllLicenses(): Flow<List<LicenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLicenses(licenses: List<LicenseEntity>)

    @Query("DELETE FROM licenses")
    suspend fun clearLicenses()
}

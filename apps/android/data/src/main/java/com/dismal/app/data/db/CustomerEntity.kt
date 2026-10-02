package com.dismal.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dismal.app.domain.models.Customer

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val phone: String?,
    @androidx.room.ColumnInfo(defaultValue = "''")
    val updatedAt: String = "",
    @androidx.room.ColumnInfo(defaultValue = "0")
    val isSynced: Boolean = false,
    val saldoActual: Double? = null,
    val diasAtraso: Long = 0L,
    val estado: String? = null,
    val syncStatus: String = com.dismal.app.data.db.SyncStatus.SYNCED.value,
    val syncErrorMessage: String? = null,
)

fun CustomerEntity.asDomainModel(): Customer {
    return Customer(
        id = id,
        name = name,
        email = email,
        phone = phone,
        updatedAt = updatedAt,
        isSynced = isSynced,
        saldoActual = saldoActual,
        diasAtraso = diasAtraso,
        estado = estado,
        syncStatus = syncStatus,
        syncErrorMessage = syncErrorMessage,
    )
}

fun Customer.asDatabaseModel(
    isSynced: Boolean = false,
    syncStatus: String = if (isSynced) com.dismal.app.data.db.SyncStatus.SYNCED.value else com.dismal.app.data.db.SyncStatus.PENDING.value,
    syncErrorMessage: String? = null,
): CustomerEntity {
    return CustomerEntity(
        id = id,
        name = name,
        email = email,
        phone = phone,
        updatedAt = updatedAt ?: "",
        isSynced = isSynced,
        saldoActual = saldoActual,
        diasAtraso = diasAtraso,
        estado = estado,
        syncStatus = syncStatus,
        syncErrorMessage = syncErrorMessage,
    )
}

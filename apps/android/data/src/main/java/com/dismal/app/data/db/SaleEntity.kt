package com.dismal.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dismal.app.domain.models.Sale

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey val id: String,
    val saleNumber: String? = null,
    @ColumnInfo(name = "customerId")
    val clientId: String,
    @ColumnInfo(defaultValue = "'CASH'")
    val saleType: String,
    @ColumnInfo(defaultValue = "'DRAFT'")
    val status: String,
    @ColumnInfo(defaultValue = "'EC'")
    val country: String = "EC",
    @ColumnInfo(defaultValue = "'USD'")
    val currency: String = "USD",
    @ColumnInfo(defaultValue = "0")
    val total: Double,
    @ColumnInfo(defaultValue = "0")
    val paid: Double,
    @ColumnInfo(defaultValue = "0")
    val balance: Double,
    @ColumnInfo(defaultValue = "''")
    val createdAt: String,
    @ColumnInfo(defaultValue = "''")
    val updatedAt: String,
    @ColumnInfo(defaultValue = "'[]'")
    val itemsJson: String,
    @ColumnInfo(defaultValue = "0")
    val isSynced: Boolean = false,
    @ColumnInfo(defaultValue = "'SYNCED'")
    val syncStatus: String = SyncStatus.SYNCED.value,
    val syncErrorMessage: String? = null,
)

fun SaleEntity.asDomainModel(): Sale {
    val items = Converters.toSaleItemList(itemsJson) ?: emptyList()
    return Sale(
        id = id,
        saleNumber = saleNumber,
        clientId = clientId,
        saleType = saleType,
        country = country,
        currency = currency,
        status = status,
        total = total,
        paid = paid,
        balance = balance,
        createdAt = createdAt,
        updatedAt = updatedAt,
        items = items,
        syncStatus = syncStatus,
        syncErrorMessage = syncErrorMessage,
    )
}

fun Sale.asDatabaseModel(
    isSynced: Boolean = false,
    syncStatus: String = if (isSynced) SyncStatus.SYNCED.value else SyncStatus.PENDING.value,
    syncErrorMessage: String? = null,
): SaleEntity {
    val itemsJson = Converters.fromSaleItemList(items) ?: "[]"
    return SaleEntity(
        id = id,
        saleNumber = saleNumber,
        clientId = clientId,
        saleType = saleType,
        status = status,
        country = country,
        currency = currency,
        total = total,
        paid = paid,
        balance = balance,
        createdAt = createdAt,
        updatedAt = updatedAt,
        itemsJson = itemsJson,
        isSynced = isSynced,
        syncStatus = syncStatus,
        syncErrorMessage = syncErrorMessage,
    )
}

enum class SyncStatus(val value: String) {
    PENDING("PENDING"),
    SYNCED("SYNCED"),
    ERROR("ERROR"),
    CONFLICT("CONFLICT"),
}

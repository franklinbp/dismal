package com.dismal.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_outbox")
data class SyncOutboxEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val action: String,
    val status: String,
    val errorMessage: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

object SyncOutboxType {
    const val SALE = "SALE"
    const val CUSTOMER = "CUSTOMER"
}

object SyncOutboxAction {
    const val CREATE = "CREATE"
}

object SyncOutboxStatus {
    const val PENDING = "PENDING"
    const val SENT = "SENT"
    const val ERROR = "ERROR"
}

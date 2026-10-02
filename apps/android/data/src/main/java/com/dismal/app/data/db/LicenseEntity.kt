package com.dismal.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dismal.app.domain.models.License

@Entity(tableName = "licenses")
data class LicenseEntity(
    @PrimaryKey val id: String,
    val licenseKey: String,
    val status: String,
    val available: Boolean,
    val usedActivations: Int,
    val maxActivations: Int,
    val purchasePrice: Double,
    val softwareId: String?,
    val softwareName: String?,
    val ownerId: String?,
    val ownerEmail: String?,
)

fun LicenseEntity.asDomainModel(): License {
    return License(
        id = id,
        licenseKey = licenseKey,
        softwareId = softwareId,
        softwareName = softwareName,
        purchasePrice = purchasePrice,
        maxActivations = maxActivations,
        usedActivations = usedActivations,
        status = status,
        ownerId = ownerId,
        ownerEmail = ownerEmail,
        available = available,
    )
}

fun License.asDatabaseModel(): LicenseEntity {
    return LicenseEntity(
        id = id,
        licenseKey = licenseKey,
        status = status,
        available = available,
        usedActivations = usedActivations,
        maxActivations = maxActivations,
        purchasePrice = purchasePrice,
        softwareId = softwareId,
        softwareName = softwareName,
        ownerId = ownerId,
        ownerEmail = ownerEmail,
    )
}

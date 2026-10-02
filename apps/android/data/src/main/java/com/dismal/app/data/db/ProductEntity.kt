package com.dismal.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dismal.app.domain.models.Product

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val platform: String,
    val imageUrl: String?,
    // Changed from List<License> to String
    val licensesJson: String,
    @androidx.room.ColumnInfo(defaultValue = "0")
    val isSynced: Boolean = false,
)

fun ProductEntity.asDomainModel(): Product {
    val licenses = Converters.toLicenseList(licensesJson) ?: emptyList()
    return Product(
        id = id,
        name = name,
        description = description,
        price = price,
        platform = platform,
        imageUrl = imageUrl,
        licenses = licenses,
    )
}

fun Product.asDatabaseModel(isSynced: Boolean = false): ProductEntity {
    val licensesJson = Converters.fromLicenseList(licenses) ?: "[]"
    return ProductEntity(
        id = id,
        name = name,
        description = description,
        price = price,
        platform = platform,
        imageUrl = imageUrl,
        // Changed
        licensesJson = licensesJson,
        isSynced = isSynced,
    )
}

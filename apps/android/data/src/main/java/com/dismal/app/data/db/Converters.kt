package com.dismal.app.data.db

import androidx.room.TypeConverter
import com.dismal.app.domain.models.License
import com.dismal.app.domain.models.SaleItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object Converters { // Changed from class to object
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listLicenseType = Types.newParameterizedType(List::class.java, License::class.java)
    private val listSaleItemType = Types.newParameterizedType(List::class.java, SaleItem::class.java)
    private val jsonAdapter = moshi.adapter<List<License>>(listLicenseType)
    private val saleItemAdapter = moshi.adapter<List<SaleItem>>(listSaleItemType)

    @TypeConverter
    fun fromLicenseList(licenses: List<License>?): String? {
        return licenses?.let { jsonAdapter.toJson(it) }
    }

    @TypeConverter
    fun toLicenseList(json: String?): List<License>? {
        return json?.let { jsonAdapter.fromJson(it) }
    }

    @TypeConverter
    fun fromSaleItemList(items: List<SaleItem>?): String? {
        return items?.let { saleItemAdapter.toJson(it) }
    }

    @TypeConverter
    fun toSaleItemList(json: String?): List<SaleItem>? {
        return json?.let { saleItemAdapter.fromJson(it) }
    }
}

package com.dismal.app.data.network.dto

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ProductDtoParsingTest {
    @Test
    fun adminProduct_allowsNullDescription() {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val type = Types.newParameterizedType(List::class.java, AdminProductDto::class.java)
        val adapter = moshi.adapter<List<AdminProductDto>>(type)

        val products =
            adapter.fromJson(
                """
                [
                  {
                    "id": "product-1",
                    "name": "Microsoft 365",
                    "description": null,
                    "price": 8.0,
                    "platform": "Windows"
                  }
                ]
                """.trimIndent(),
            )

        assertNotNull(products)
        assertEquals("", products!!.first().asDomainModel().description)
    }
}

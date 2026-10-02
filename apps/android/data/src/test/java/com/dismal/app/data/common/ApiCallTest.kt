package com.dismal.app.data.common

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import retrofit2.Response

class ApiCallTest {
    @Test
    fun serverErrorsDoNotExposeTheRawResponseBody() {
        val response =
            Response.error<String>(
                500,
                "{\"message\":\"internal database detail\"}"
                    .toResponseBody("application/json".toMediaType()),
            )

        val message = formatHttpError("No se pudo cargar el resumen", response)

        assertEquals(
            "No se pudo cargar el resumen. El servidor no pudo procesar la solicitud. Intenta nuevamente.",
            message,
        )
        assertFalse(message.contains("database detail"))
    }
}

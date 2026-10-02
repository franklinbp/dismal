package com.dismal.app.data.repository

import com.dismal.app.data.common.AppResult
import com.dismal.app.data.common.safeApiCall
import com.dismal.app.data.network.InvoicesApi
import com.dismal.app.domain.models.CreateInvoiceRequest
import com.dismal.app.domain.models.Invoice

class InvoiceRepository(
    private val invoicesApi: InvoicesApi,
) {
    suspend fun listInvoices(
        query: String? = null,
        from: String? = null,
        to: String? = null,
        page: Int = 0,
        size: Int = 25,
    ): AppResult<List<Invoice>> {
        return safeApiCall(
            errorPrefix = "Error al obtener facturas",
            call = { invoicesApi.getInvoices(query, from, to, page, size) },
        ) { body ->
            body?.content.orEmpty()
        }
    }

    suspend fun createInvoice(
        saleId: String,
        dueDate: String?,
    ): AppResult<Invoice> {
        val request =
            CreateInvoiceRequest(
                saleId = saleId,
                dueDate = dueDate?.takeIf { it.isNotBlank() },
            )
        return safeApiCall(
            errorPrefix = "Error al crear factura",
            emptyBodyMessage = "Respuesta vacia al crear factura",
            call = { invoicesApi.createInvoice(request) },
        ) { body ->
            body!!
        }
    }
}

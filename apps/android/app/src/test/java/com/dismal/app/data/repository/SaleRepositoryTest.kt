package com.dismal.app.data.repository

import com.dismal.app.data.common.AppResult
import com.dismal.app.data.db.SaleDao
import com.dismal.app.data.network.PaymentsApi
import com.dismal.app.data.network.SalesApi
import com.dismal.app.data.network.dto.PageResponseDto
import com.dismal.app.domain.models.Sale
import com.dismal.app.domain.models.SaleItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class SaleRepositoryTest {
    private val salesApi = mockk<SalesApi>()
    private val paymentsApi = mockk<PaymentsApi>()
    private val saleDao = mockk<SaleDao>(relaxed = true)
    private val syncOutboxRepository = mockk<SyncOutboxRepository>(relaxed = true)

    @Test
    fun createAndConfirmSale_sendsCountryAndRegistersFullPayment() =
        runTest {
            val draft = sale(id = "s1", status = "DRAFT", paid = 0.0, balance = 8.0)
            val confirmed = draft.copy(status = "CONFIRMED", saleNumber = "MS-EC-10000")
            val paid = confirmed.copy(status = "PAID", paid = 8.0, balance = 0.0)
            coEvery { salesApi.createSale(any()) } returns Response.success(draft)
            coEvery { salesApi.confirmSale("s1", any()) } returns Response.success(confirmed)
            coEvery { paymentsApi.addPayment(any()) } returns Response.success(paid)

            val repo = SaleRepository(saleDao, salesApi, paymentsApi, syncOutboxRepository)
            val result =
                repo.createAndConfirmSaleRemote(
                    clientId = "c1",
                    softwareId = "p1",
                    quantity = 1,
                    unitPrice = 8.0,
                    saleType = "CASH",
                    country = "EC",
                    deliveryChannels = listOf("EMAIL"),
                    registerFullPayment = true,
                    paymentMethod = "TRANSFER",
                    paymentReference = "DEP-100",
                )

            assertTrue(result is AppResult.Success)
            assertEquals("MS-EC-10000", (result as AppResult.Success).data.saleNumber)
            coVerify { salesApi.createSale(match { it.country == "EC" }) }
            coVerify { salesApi.confirmSale("s1", match { it.deliveryChannels == listOf("EMAIL") }) }
            coVerify { paymentsApi.addPayment(match { it.saleId == "s1" && it.method == "TRANSFER" }) }
        }

    @Test
    fun createAndConfirmSale_reportsPersistedSaleWhenConfirmationFails() =
        runTest {
            coEvery { salesApi.createSale(any()) } returns
                Response.success(sale(id = "s1", status = "DRAFT", paid = 0.0, balance = 8.0))
            coEvery { salesApi.confirmSale("s1", any()) } returns
                Response.error(
                    409,
                    "Sin inventario disponible".toResponseBody("text/plain".toMediaTypeOrNull()),
                )

            val repo = SaleRepository(saleDao, salesApi, paymentsApi, syncOutboxRepository)
            val result =
                repo.createAndConfirmSaleRemote(
                    clientId = "c1",
                    softwareId = "p1",
                    quantity = 1,
                    unitPrice = 8.0,
                    saleType = "CASH",
                    country = "EC",
                )

            assertTrue(result is AppResult.Error)
            assertTrue((result as AppResult.Error).message.startsWith("La venta ya fue creada"))
            coVerify(exactly = 1) { salesApi.createSale(any()) }
            coVerify(exactly = 1) { salesApi.confirmSale("s1", any()) }
            coVerify(exactly = 0) { paymentsApi.addPayment(any()) }
        }

    @Test
    fun refreshSales_insertsItems() =
        runTest {
            val sales =
                listOf(
                    Sale(
                        id = "s1",
                        clientId = "c1",
                        saleType = "CASH",
                        status = "CONFIRMED",
                        total = 10.0,
                        paid = 10.0,
                        balance = 0.0,
                        createdAt = "2026-01-13T21:46:44.323",
                        updatedAt = "2026-01-13T21:46:44.323",
                        items = emptyList(),
                    ),
                )
            coEvery { salesApi.getSales() } returns
                Response.success(
                    PageResponseDto(
                        content = sales,
                        totalElements = 1,
                        totalPages = 1,
                        size = 1,
                        number = 0,
                    ),
                )

            val repo = SaleRepository(saleDao, salesApi, paymentsApi, syncOutboxRepository)
            val result = repo.refreshSales()

            coVerify { saleDao.insertSales(match { it.size == 1 }) }
            assertTrue(result is com.dismal.app.data.common.AppResult.Success)
        }

    @Test
    fun refreshSales_throwsOnError() =
        runTest {
            coEvery { salesApi.getSales() } returns
                Response.error(
                    500,
                    "".toResponseBody("text/plain".toMediaTypeOrNull()),
                )

            val repo = SaleRepository(saleDao, salesApi, paymentsApi, syncOutboxRepository)
            val result = repo.refreshSales()

            assertTrue(result is com.dismal.app.data.common.AppResult.Error)
        }

    @Test
    fun syncSales_marksSynced() =
        runTest {
            val sales =
                listOf(
                    Sale(
                        id = "s1",
                        clientId = "c1",
                        saleType = "CASH",
                        status = "CONFIRMED",
                        total = 10.0,
                        paid = 10.0,
                        balance = 0.0,
                        createdAt = "2026-01-13T21:46:44.323",
                        updatedAt = "2026-01-13T21:46:44.323",
                        items =
                            listOf(
                                SaleItem(
                                    id = "i1",
                                    softwareId = "p1",
                                    softwareName = null,
                                    quantity = 1,
                                    unitPrice = 10.0,
                                    subtotal = 10.0,
                                ),
                            ),
                    ),
                )
            coEvery { saleDao.getPendingSales() } returns
                sales.map {
                    com.dismal.app.data.db.SaleEntity(
                        id = it.id,
                        clientId = it.clientId,
                        saleType = it.saleType,
                        status = it.status,
                        total = it.total,
                        paid = it.paid,
                        balance = it.balance,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt,
                        itemsJson = com.dismal.app.data.db.Converters.fromSaleItemList(it.items) ?: "[]",
                        isSynced = false,
                        syncStatus = com.dismal.app.data.db.SyncStatus.PENDING.value,
                        syncErrorMessage = null,
                    )
                }
            coEvery { saleDao.getSaleById("s1") } returns
                com.dismal.app.data.db.SaleEntity(
                    id = "s1",
                    clientId = "c1",
                    saleType = "CASH",
                    status = "CONFIRMED",
                    total = 10.0,
                    paid = 10.0,
                    balance = 0.0,
                    createdAt = "2026-01-13T21:46:44.323",
                    updatedAt = "2026-01-13T21:46:44.323",
                    itemsJson = com.dismal.app.data.db.Converters.fromSaleItemList(sales.first().items) ?: "[]",
                    isSynced = false,
                    syncStatus = com.dismal.app.data.db.SyncStatus.PENDING.value,
                    syncErrorMessage = null,
                )
            coEvery { salesApi.createSale(any()) } returns Response.success(sales.first())

            val repo = SaleRepository(saleDao, salesApi, paymentsApi, syncOutboxRepository)
            val result = repo.syncSales()

            coVerify { saleDao.markSalesSynced(listOf("s1")) }
            assertTrue(result is com.dismal.app.data.common.AppResult.Success)
        }

    private fun sale(
        id: String,
        status: String,
        paid: Double,
        balance: Double,
    ): Sale =
        Sale(
            id = id,
            saleNumber = null,
            clientId = "c1",
            saleType = "CASH",
            country = "EC",
            currency = "USD",
            status = status,
            total = 8.0,
            paid = paid,
            balance = balance,
            createdAt = "2026-08-11T12:00:00",
            updatedAt = "2026-08-11T12:00:00",
            items =
                listOf(
                    SaleItem(
                        id = "i1",
                        softwareId = "p1",
                        softwareName = "Microsoft 365",
                        quantity = 1,
                        unitPrice = 8.0,
                        subtotal = 8.0,
                    ),
                ),
        )
}

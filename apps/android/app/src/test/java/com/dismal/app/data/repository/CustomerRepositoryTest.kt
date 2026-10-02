package com.dismal.app.data.repository

import com.dismal.app.data.db.CustomerDao
import com.dismal.app.data.network.CustomerApi
import com.dismal.app.data.network.dto.AdminUserResponseDto
import com.dismal.app.data.network.dto.ArSummaryDto
import com.dismal.app.data.network.dto.PageResponseDto
import com.dismal.app.domain.models.Customer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class CustomerRepositoryTest {
    private val customerApi = mockk<CustomerApi>()
    private val customerDao = mockk<CustomerDao>(relaxed = true)
    private val syncOutboxRepository = mockk<SyncOutboxRepository>(relaxed = true)

    @Test
    fun refreshCustomers_insertsItems() =
        runTest {
            val users =
                listOf(
                    AdminUserResponseDto(
                        id = "c1",
                        firstname = "Customer",
                        lastname = null,
                        email = "test@example.com",
                        phone = null,
                        arSummary = ArSummaryDto(0.0, 0L, "AL_DIA"),
                    ),
                )
            coEvery { customerApi.getAdminClients(any(), any(), any()) } returns
                Response.success(
                    PageResponseDto(users, 1, 1, 10, 0),
                )

            val repo = CustomerRepository(customerDao, customerApi, syncOutboxRepository)
            val result = repo.refreshCustomers()

            coVerify { customerDao.insertCustomers(match { it.size == 1 }) }
            assertTrue(result is com.dismal.app.data.common.AppResult.Success)
        }

    @Test
    fun refreshCustomers_throwsOnError() =
        runTest {
            coEvery { customerApi.getAdminClients(any(), any(), any()) } returns
                Response.error(
                    500,
                    "".toResponseBody("text/plain".toMediaTypeOrNull()),
                )

            val repo = CustomerRepository(customerDao, customerApi, syncOutboxRepository)
            val result = repo.refreshCustomers()

            assertTrue(result is com.dismal.app.data.common.AppResult.Error)
        }

    @Test
    fun syncCustomers_marksSynced() =
        runTest {
            val customers =
                listOf(
                    Customer(
                        id = "c1",
                        name = "Customer",
                        email = "test@example.com",
                        phone = null,
                    ),
                )
            coEvery { customerDao.getPendingCustomers() } returns
                customers.map {
                    com.dismal.app.data.db.CustomerEntity(
                        id = it.id,
                        name = it.name,
                        email = it.email,
                        phone = it.phone,
                        updatedAt = "",
                        isSynced = false,
                        syncStatus = com.dismal.app.data.db.SyncStatus.PENDING.value,
                        syncErrorMessage = null,
                    )
                }
            coEvery { customerDao.getCustomerById("c1") } returns
                com.dismal.app.data.db.CustomerEntity(
                    id = "c1",
                    name = "Customer",
                    email = "test@example.com",
                    phone = null,
                    updatedAt = "",
                    isSynced = false,
                    syncStatus = com.dismal.app.data.db.SyncStatus.PENDING.value,
                    syncErrorMessage = null,
                )
            coEvery { customerApi.syncCustomers(any()) } returns Response.success(customers)

            val repo = CustomerRepository(customerDao, customerApi, syncOutboxRepository)
            val result = repo.syncCustomers()

            coVerify { customerDao.markCustomersSynced(listOf("c1")) }
            assertTrue(result is com.dismal.app.data.common.AppResult.Success)
        }
}

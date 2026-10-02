package com.dismal.app.data.repository

import com.dismal.app.data.common.AppResult
import com.dismal.app.data.common.safeApiCall
import com.dismal.app.data.network.AccountsReceivableApi
import com.dismal.app.domain.models.AccountsReceivable

class AccountsReceivableRepository(
    private val accountsReceivableApi: AccountsReceivableApi,
) {
    suspend fun getOpenByClient(clientId: String): AppResult<List<AccountsReceivable>> {
        return safeApiCall(
            errorPrefix = "Error al obtener cuentas por cobrar",
            call = { accountsReceivableApi.getByClient(clientId) },
        ) { body ->
            body
                .orEmpty()
                .filter { it.balance > 0.0 && it.status != "PAID" }
                .sortedWith(compareBy<AccountsReceivable> { it.dueDate ?: "" }.thenBy { it.id })
        }
    }
}

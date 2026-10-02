package com.dismal.app.data.common

import retrofit2.Response

suspend inline fun <T, R> safeApiCall(
    errorPrefix: String,
    emptyBodyMessage: String? = null,
    crossinline call: suspend () -> Response<T>,
    crossinline mapper: (T?) -> R,
): AppResult<R> {
    return try {
        val response = retryWithBackoff { call() }
        if (!response.isSuccessful) {
            AppResult.Error(formatHttpError(errorPrefix, response))
        } else {
            val body = response.body()
            if (body == null && emptyBodyMessage != null) {
                AppResult.Error(emptyBodyMessage)
            } else {
                AppResult.Success(mapper(body))
            }
        }
    } catch (e: Exception) {
        AppResult.Error(buildExceptionMessage(errorPrefix, e), e)
    }
}

fun buildExceptionMessage(
    prefix: String,
    throwable: Throwable,
): String {
    return prefix + (throwable.message?.let { " ($it)" } ?: "")
}

fun <T> formatHttpError(
    prefix: String,
    response: Response<T>,
): String {
    if (response.code() >= 500) {
        return "$prefix. El servidor no pudo procesar la solicitud. Intenta nuevamente."
    }

    val detail = response.errorBody()?.string()?.trim().orEmpty().take(200)
    return buildString {
        append(prefix)
        append(" (HTTP ")
        append(response.code())
        append(")")
        if (detail.isNotBlank()) {
            append(": ")
            append(detail)
        }
    }
}

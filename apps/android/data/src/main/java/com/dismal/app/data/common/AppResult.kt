package com.dismal.app.data.common

sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()

    data class Error(val message: String, val cause: Throwable? = null) : AppResult<Nothing>()
}

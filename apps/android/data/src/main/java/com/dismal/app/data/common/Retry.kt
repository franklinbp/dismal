package com.dismal.app.data.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

suspend fun <T> retryWithBackoff(
    times: Int = 3,
    initialDelayMs: Long = 200,
    maxDelayMs: Long = 2000,
    factor: Double = 2.0,
    block: suspend () -> T,
): T {
    var currentDelay = initialDelayMs
    repeat(times - 1) {
        try {
            return block()
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
        }
    }
    return block()
}

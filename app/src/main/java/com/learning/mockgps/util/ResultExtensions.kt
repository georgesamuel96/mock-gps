package com.learning.mockgps.util

import com.learning.mockgps.MockLocationException

/**
 * Extracts user-friendly error message from a Result failure.
 */
fun <T> Result<T>.getErrorMessage(defaultMessage: String): String {
    val exception = exceptionOrNull()
    return when (exception) {
        is MockLocationException -> exception.message ?: defaultMessage
        else -> exception?.message ?: defaultMessage
    }
}

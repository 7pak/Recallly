package com.at.recallly.core.util

sealed class RecalllyException(message: String, cause: Throwable? = null) : Exception(message, cause)

package com.musicstream.data.api

/**
 * Sealed class wrapping API responses — forces callers to handle all states.
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int = -1) : ApiResult<Nothing>()
    data object Loading : ApiResult<Nothing>()

    val isSuccess get() = this is Success
    val isError get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data

    fun <R> map(transform: (T) -> R): ApiResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> this
    }

    suspend fun onSuccess(action: suspend (T) -> Unit): ApiResult<T> {
        if (this is Success) action(data)
        return this
    }

    suspend fun onError(action: suspend (String, Int) -> Unit): ApiResult<T> {
        if (this is Error) action(message, code)
        return this
    }
}

suspend fun <T> safeApiCall(block: suspend () -> T): ApiResult<T> = try {
    ApiResult.Success(block())
} catch (e: retrofit2.HttpException) {
    ApiResult.Error(e.message ?: "HTTP error", e.code())
} catch (e: java.io.IOException) {
    ApiResult.Error("Network error: ${e.message}")
} catch (e: Exception) {
    ApiResult.Error(e.message ?: "Unknown error")
}

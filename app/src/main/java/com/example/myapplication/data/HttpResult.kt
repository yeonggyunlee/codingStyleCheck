package com.example.myapplication

sealed class HttpResult<out R> {
    data class Success<out T>(val data: T?) : HttpResult<T>()
    data class Failure(val throwable: Throwable, val httpError: Boolean = false) :
        HttpResult<Nothing>() {
        fun getCode() = (throwable as HttpException).code()
        var error: Error? = null
    }

    object Loading : HttpResult<Nothing>()
    object None : HttpResult<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Failure -> "Error[exception=$throwable]"
            is None -> "None"
            Loading -> "Loading"
        }

    }

}

data class Error(val code: Int, val msg: String, val data: Map<String, Any>?)

fun Throwable.onError(): HttpResult.Failure {
    return when (this) {
        is HttpException -> {
            val body = response()?.errorBody()
            HttpResult.Failure(this, true).apply {
                body?.let {
                    val type = object : TypeToken<Error>() {}.type
                    try {
                        error = Gson().fromJson(it.charStream(), type)
                    } catch (e: Exception) {
                    }
                }
            }
        }
        else -> {
            HttpResult.Failure(this)
        }
    }
}

inline fun <T> runBlocking(block: () -> T): HttpResult<T> {
    return try {
        HttpResult.Success(block())
    } catch (t: Throwable) {
        HttpResult.Failure(t)
    }
}

fun <T> HttpResult<T>.asFlow() = flowOf(this)

fun <T> HttpResult<T>.isSuccess(): Boolean {
    return this is HttpResult.Success
}

fun <T> HttpResult<T>.toFailure(): HttpResult.Failure? {
    return this as? HttpResult.Failure
}

fun <T> HttpResult<T>.onError(): Error? {
    return this.toFailure()?.error
}

fun <T> HttpResult<T>.successOr(fallback: T): T {
    return (this as? HttpResult.Success<T>)?.data ?: fallback
}

fun <T> HttpResult<T>.success(callback: (T?) -> Unit) {
    if (isSuccess()) {
        callback(this.data)
    }
}

fun <T> HttpResult<T>.successOrFailure(
    success: (T?) -> Unit,
    failure: (HttpResult<T>) -> Unit = {}
) {
    if (isSuccess() && this !is HttpResult.None) {
        success(data)
    } else {
        failure(this)
    }
}

val <T> HttpResult<T>.data: T?
    get() = (this as? HttpResult.Success)?.data

inline fun <reified T> HttpResult<T>.updateOnSuccess(liveData: MutableLiveData<T>) {
    if (this is HttpResult.Success) {
        liveData.postValue(data)
    }
}

fun <T> HttpResult<T>.onSuccess(callback: (T?) -> Unit): HttpResult<T> {
    if (isSuccess()) {
        callback(this.data)
    }
    return this
}

fun <T> HttpResult<T>.onFailure(callback: (HttpResult.Failure?) -> Unit): HttpResult<T> {
    if (this is HttpResult.Failure) {
        callback(this)
    }
    return this
}

/**
 * [Response] 를 [HttpResult] 변환해서 반환한다.
 *
 * - responseCode > 200 && responseCode <= 300 : [HttpResult.Success]
 * - responseCode > 200 && responseCode <= 300 && responseBody == null : [HttpResult.None]
 * - responseCode < 200 && responseCode > 300 : [HttpResult.Failure]
 */
fun <T> Response<T>.toHttpResult(): HttpResult<T> {
    return try {
        val response = this

        if (response.isSuccessful) {
            response.body()?.let { HttpResult.Success(it) } ?: HttpResult.None
        } else {
            // handle error code (errorCode < 200 && errorCode >= 300)
            throw HttpException(response)
        }
    } catch (e: Exception) {
        e.onError()
    }
}
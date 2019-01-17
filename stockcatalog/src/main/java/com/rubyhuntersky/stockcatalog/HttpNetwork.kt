package com.rubyhuntersky.stockcatalog

import io.reactivex.Single

interface HttpNetwork {
    fun request(request: HttpNetworkRequest): HttpNetworkResponse
}

data class HttpNetworkRequest(val url: String)

sealed class HttpNetworkResponse {
    data class ConnectionError(val url: String, val error: Throwable) : HttpNetworkResponse()
    data class Text(val url: String, val text: String, val httpCode: Int) : HttpNetworkResponse()
}

internal fun HttpNetwork.getRequestSingle(request: HttpNetworkRequest): Single<HttpNetworkResponse> = Single.create {
    try {
        val response = request(request)
        if (!it.isDisposed) {
            it.onSuccess(response)
        }
    } catch (t: Throwable) {
        if (!it.isDisposed) {
            it.onError(t)
        }
    }
}

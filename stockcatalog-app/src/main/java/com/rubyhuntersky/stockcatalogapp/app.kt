package com.rubyhuntersky.stockcatalogapp

import com.rubyhuntersky.stockcatalog.HttpNetwork
import com.rubyhuntersky.stockcatalog.HttpNetworkRequest
import com.rubyhuntersky.stockcatalog.HttpNetworkResponse
import okhttp3.OkHttpClient
import okhttp3.Request

object OkNetwork : HttpNetwork {

    override fun request(request: HttpNetworkRequest): HttpNetworkResponse {
        val okRequest = Request.Builder().url(request.url).get().build()
        val okResponse = client.newCall(okRequest).execute()
        return if (okResponse.isSuccessful) {
            val text = okResponse.body()?.string() ?: ""
            HttpNetworkResponse.Text(request.url, text, okResponse.code())
        } else {
            HttpNetworkResponse.ConnectionError(request.url, Exception("No body"))
        }
    }

    private val client = OkHttpClient()
}
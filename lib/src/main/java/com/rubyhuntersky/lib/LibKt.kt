package com.rubyhuntersky.lib

import io.reactivex.Single

interface HttpNetwork {
    fun request(request: Request): Response
    data class Request(val url: String)
    sealed class Response {
        data class ConnectionError(val error: Throwable) : Response()
        data class Text(val text: String, val httpCode: Int) : Response()
    }
}

interface StockCatalogClient {
    var stockCatalog: StockCatalog
    fun sendStockCatalogQuery(query: StockCatalog.Query) = stockCatalog.sendQuery(query)
    fun onStockCatalogResult(result: StockCatalog.Result)
}

fun StockCatalog.Query.sendToClient(client: StockCatalogClient) = client.sendStockCatalogQuery(this)


package com.rubyhuntersky.lib

import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy

interface StockCatalogClient {
    var stockCatalog: StockCatalog
    fun onStockCatalogResult(result: StockCatalog.Result)
    fun StockCatalog.Query.sendToCatalog() = stockCatalog.sendQuery(this)
}

class StockCatalog {

    sealed class Query {
        object Clear : Query()
        data class FindStock(val symbol: String) : Query()
    }

    sealed class Result {
        data class StockData(val symbol: String) : Result()
        object NetworkError : Result()
        data class InvalidSymbol(val symbol: String) : Result()
    }

    fun connectClient(client: StockCatalogClient) {
        client.stockCatalog = this
        this.client = client
    }

    private lateinit var client: StockCatalogClient

    fun connectNetwork(network: HttpNetwork) {
        this.network = network
    }

    private lateinit var network: HttpNetwork

    fun sendQuery(query: Query) {
        when (query) {
            is Query.Clear -> requestDisposable?.dispose()
            is Query.FindStock -> {
                requestDisposable?.dispose()
                if (query.symbol.isBlank()) {
                    Result.InvalidSymbol(query.symbol).sendToClient()
                } else {
                    requestDisposable = query.asRequest().toSingle()
                        .subscribeBy(
                            onError = {
                                Result.NetworkError.sendToClient()
                            },
                            onSuccess = { response ->
                                val result = when (response) {
                                    is HttpNetwork.Response.ConnectionError -> Result.NetworkError
                                    is HttpNetwork.Response.Text -> if (response.httpCode == 200) {
                                        Result.StockData(symbol = query.symbol)
                                    } else {
                                        Result.NetworkError
                                    }
                                }
                                result.sendToClient()
                            }
                        )
                }

            }
        }
    }

    private var requestDisposable: Disposable? = null
    private fun HttpNetwork.Request.toSingle() = Single.fromCallable { network.request(this) }
    private fun Result.sendToClient() = client.onStockCatalogResult(this)
    private fun Query.FindStock.asRequest(): HttpNetwork.Request {
        val url = "?$symbol"
        // TODO Make a real request
        return HttpNetwork.Request(url)
    }
}

interface HttpNetwork {
    fun request(request: Request): Response
    data class Request(val url: String)
    sealed class Response {
        data class ConnectionError(val error: Throwable) : Response()
        data class Text(val text: String, val httpCode: Int) : Response()
    }
}

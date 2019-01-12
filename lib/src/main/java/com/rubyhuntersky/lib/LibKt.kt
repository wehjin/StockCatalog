package com.rubyhuntersky.lib

import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers

interface StockCatalogClient {
    var stockCatalog: StockCatalog
    fun sendStockCatalogQuery(query: StockCatalog.Query) = stockCatalog.sendQuery(query)
    fun onStockCatalogResult(result: StockCatalog.Result)
}

fun StockCatalog.Query.send(client: StockCatalogClient) = client.sendStockCatalogQuery(this)

class StockCatalog(private val network: HttpNetwork) {

    sealed class Query {
        object Clear : Query()
        data class FindStock(val symbol: String) : Query()
    }

    sealed class Result {
        data class StockData(val symbol: String) : Result()
        object NetworkError : Result()
        data class InvalidSymbol(val symbol: String) : Result()
    }

    fun connect(client: StockCatalogClient) {
        client.stockCatalog = this
        this.client = client
    }

    private lateinit var client: StockCatalogClient

    fun sendQuery(query: Query) {
        when (query) {
            is Query.Clear -> requestDisposable?.dispose()
            is Query.FindStock -> {
                requestDisposable?.dispose()
                if (query.symbol.isBlank()) {
                    Result.InvalidSymbol(query.symbol).sendToClient()
                } else {
                    requestDisposable = query.asRequest().toSingle()
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.single())
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

    private fun Result.sendToClient() = client.onStockCatalogResult(this)

    private fun Query.FindStock.asRequest(): HttpNetwork.Request {
        val fields = listOf(
            "symbol",
            "longName",
            "shortName",
            "regularMarketPrice",
            "marketCap",
            "sharesOutstanding"
        ).joinToString("%2C")
        val params =
            "lang=en-US&region=US&corsDomain=finance.yahoo.com&fields=$fields&symbols=${symbol.trim()}&formatted=false"
        val url = "https://query1.finance.yahoo.com/v7/finance/quote?$params"
        return HttpNetwork.Request(url)
    }

    private fun HttpNetwork.Request.toSingle(): Single<HttpNetwork.Response> = Single.create {
        try {
            val response = network.request(this)
            if (!it.isDisposed) {
                it.onSuccess(response)
            }
        } catch (t: Throwable) {
            if (!it.isDisposed) {
                it.onError(t)
            }
        }
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

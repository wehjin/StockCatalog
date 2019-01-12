package com.rubyhuntersky.lib

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers


class StockCatalogClient(private val listener: Listener) {
    interface Listener {
        fun onStockCatalogResult(result: StockCatalog.Result)
    }

    fun sendQuery(query: StockCatalog.Query) = catalog.sendQuery(query)
    internal lateinit var catalog: StockCatalog
    internal fun sendResult(result: StockCatalog.Result) = listener.onStockCatalogResult(result)
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

    fun connect(catalogClient: StockCatalogClient, network: HttpNetwork) {
        catalogClient.catalog = this
        this.catalogClient = catalogClient
        this.network = network
    }

    private lateinit var catalogClient: StockCatalogClient
    private lateinit var network: HttpNetwork

    internal fun sendQuery(query: Query) {
        when (query) {
            is Query.Clear -> requestDisposable?.dispose()
            is Query.FindStock -> {
                requestDisposable?.dispose()
                if (query.symbol.isBlank()) {
                    Result.InvalidSymbol(query.symbol).sendDeferred()
                } else {
                    requestDisposable = query.asRequest().toSingle()
                        .subscribeBy(
                            onError = {
                                Result.NetworkError.sendDeferred()
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
                                result.sendDeferred()
                            }
                        )
                }

            }
        }
    }

    private var requestDisposable: Disposable? = null
    private fun HttpNetwork.Request.toSingle() = Single.fromCallable { network.request(this) }
    private var resultDisposable: Disposable? = null
    private fun Result.sendDeferred() {
        resultDisposable?.dispose()
        resultDisposable = Completable.complete()
            .subscribeOn(Schedulers.trampoline())
            .subscribeBy {
                catalogClient.sendResult(this)
            }
    }

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

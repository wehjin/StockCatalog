package com.rubyhuntersky.lib

import com.beust.klaxon.Klaxon
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal

class StockCatalog(private val network: HttpNetwork) {

    sealed class Query {
        object Clear : Query()
        data class FindStock(val symbol: String) : Query()
    }

    sealed class Result {

        data class InvalidSymbol(val symbol: String) : Result()
        data class NetworkError(val url: String?) : Result()
        data class ParseError(val text: String, val reason: String? = null) : Result()
        data class Samples(
            val search: String,
            val samples: List<StockSample>
        ) : Result()
    }

    fun connect(client: StockCatalogClient) {
        client.stockCatalog = this
        this.client = client
    }

    private lateinit var client: StockCatalogClient

    fun sendQuery(query: Query) {
        queryDisposable?.dispose()
        when (query) {
            is Query.Clear -> Unit
            is Query.FindStock -> {
                val result = if (query.symbol.isBlank()) {
                    Single.just(Result.InvalidSymbol(query.symbol))
                } else {
                    query.asRequest().toSingle()
                        .map { response ->
                            when (response) {
                                is HttpNetwork.Response.ConnectionError -> Result.NetworkError(response.url)
                                is HttpNetwork.Response.Text -> if (response.httpCode != 200) {
                                    Result.NetworkError(response.url)
                                } else {
                                    response.text.parseResult(query.symbol)
                                }
                            }
                        }
                        .onErrorReturn { Result.NetworkError(it.localizedMessage) }
                }
                queryDisposable = result.subscribeOn(Schedulers.io()).observeOn(Schedulers.single())
                    .subscribeBy(onSuccess = this::sendToClient)
            }
        }
    }


    private var queryDisposable: Disposable? = null

    private fun sendToClient(result: Result) = client.onStockCatalogResult(result)

    private fun Query.FindStock.asRequest(): HttpNetwork.Request {
        val fields = listOf(
            "quoteType",
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

    private fun String.parseResult(search: String): Result {
        try {
            val requestResponse = Klaxon().parse<FinanceRequestResponse>(this)
            return requestResponse?.let { it ->
                Result.Samples(
                    search = search,
                    samples = it.quoteResponse.result
                        .map { financeQuote ->
                            StockSample(
                                symbol = financeQuote.symbol.toUpperCase().trim(),
                                sharePrice = BigDecimal.valueOf(financeQuote.regularMarketPrice),
                                marketCapitalization = BigDecimal.valueOf(financeQuote.marketCap)
                            )
                        })
            } ?: Result.ParseError(text = this)
        } catch (t: Throwable) {
            return Result.ParseError(text = this, reason = t.localizedMessage)
        }
    }
}
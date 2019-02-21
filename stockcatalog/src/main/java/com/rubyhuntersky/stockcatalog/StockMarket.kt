package com.rubyhuntersky.stockcatalog

import io.reactivex.Single

object StockMarket {
    lateinit var network: HttpNetwork

    sealed class Result {
        data class NetworkError(val reason: String) : Result()
        data class ParseError(val text: String, val reason: String? = null) : Result()
        data class Samples(val samples: List<StockSample>) : Result()
    }

    fun fetchSamples(symbols: List<String>): Single<Result> {
        return network.getRequestSingle(FinanceApi.getQuotesHttpNetworkRequest(symbols))
            .map { response ->
                when (response) {
                    is HttpNetworkResponse.ConnectionError ->
                        Result.NetworkError(response.url)
                    is HttpNetworkResponse.Text ->
                        if (response.httpCode == 200) {
                            val responseText = response.text
                            try {
                                FinanceApi.parseResponseText(responseText)
                                    ?.let {
                                        Result.Samples(it.toStockSampleList())
                                    } ?: Result.ParseError(responseText)
                            } catch (t: Throwable) {
                                Result.ParseError(responseText, t.localizedMessage)
                            }
                        } else {
                            Result.NetworkError(response.url)
                        }
                }
            }
            .onErrorReturn { Result.NetworkError(it.localizedMessage) }
    }
}
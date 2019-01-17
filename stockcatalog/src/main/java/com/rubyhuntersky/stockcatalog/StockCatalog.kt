package com.rubyhuntersky.stockcatalog

import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal

class StockCatalog(private val network: HttpNetwork) {

    fun connect(client: StockCatalogClient) {
        client.stockCatalog = this
        this.client = client
    }

    private lateinit var client: StockCatalogClient

    fun sendQuery(query: StockCatalogQuery) {
        queries.clear()
        when (query) {
            is StockCatalogQuery.Clear -> Unit
            is StockCatalogQuery.FindStock -> {
                val catalogResultSingle = if (query.symbol.isBlank()) {
                    Single.just(StockCatalogResult.InvalidSymbol(query.symbol) as StockCatalogResult)
                } else {
                    network.getRequestSingle(FinanceApi.getFindStockHttpNetworkRequest(query.symbol))
                        .map { response ->
                            when (response) {
                                is HttpNetworkResponse.ConnectionError -> StockCatalogResult.NetworkError(response.url)
                                is HttpNetworkResponse.Text -> if (response.httpCode != 200) {
                                    StockCatalogResult.NetworkError(response.url)
                                } else {
                                    getCatalogResultFromResponseText(response.text, query.symbol)
                                }
                            }
                        }
                        .onErrorReturn { StockCatalogResult.NetworkError(it.localizedMessage) }
                }
                catalogResultSingle
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.single())
                    .subscribeBy(onSuccess = this::sendCatalogResultToClient)
                    .addTo(queries)
            }
        }
    }

    private var queries = CompositeDisposable()

    private fun getCatalogResultFromResponseText(responseText: String, searchText: String): StockCatalogResult {
        try {
            val financeRequestResponse = FinanceApi.parseResponseText(responseText)
            return if (financeRequestResponse == null) {
                StockCatalogResult.ParseError(text = responseText)
            } else {
                StockCatalogResult.Samples(
                    search = searchText,
                    samples = financeRequestResponse.quoteResponse.result.map { financeQuote ->
                        StockSample(
                            symbol = financeQuote.symbol.toUpperCase().trim(),
                            sharePrice = BigDecimal.valueOf(financeQuote.regularMarketPrice),
                            marketCapitalization = BigDecimal.valueOf(financeQuote.marketCap),
                            issuer = financeQuote.longName ?: financeQuote.shortName
                        )
                    })
            }
        } catch (t: Throwable) {
            return StockCatalogResult.ParseError(text = responseText, reason = t.localizedMessage)
        }
    }

    private fun sendCatalogResultToClient(catalogResult: StockCatalogResult) =
        client.onStockCatalogResult(catalogResult)
}
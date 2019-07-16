package com.rubyhuntersky.stockcatalog

import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers

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
                val catalogResultSingle =
                    if (query.symbol.isBlank()) {
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
        return try {
            FinanceApi.parseResponseText(responseText)
                ?.let {
                    StockCatalogResult.Samples(searchText, it.toStockSampleList())
                }
                ?: StockCatalogResult.ParseError(responseText)
        } catch (t: Throwable) {
            StockCatalogResult.ParseError(responseText, t.localizedMessage)
        }
    }

    private fun sendCatalogResultToClient(catalogResult: StockCatalogResult) =
        client.onStockCatalogResult(catalogResult)
}
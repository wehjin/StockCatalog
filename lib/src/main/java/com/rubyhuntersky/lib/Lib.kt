package com.rubyhuntersky.lib

import java.math.BigDecimal

sealed class StockCatalogQuery {
    object Clear : StockCatalogQuery()
    data class FindStock(val symbol: String) : StockCatalogQuery()
}

sealed class StockCatalogResult {

    data class InvalidSymbol(val symbol: String) : StockCatalogResult()
    data class NetworkError(val url: String?) : StockCatalogResult()
    data class ParseError(val text: String, val reason: String? = null) : StockCatalogResult()
    data class Samples(val search: String, val samples: List<StockSample>) : StockCatalogResult()
}

data class StockSample(
    val symbol: String,
    val sharePrice: BigDecimal,
    val marketCapitalization: BigDecimal
)

interface HttpNetwork {
    fun request(request: Request): Response
    data class Request(val url: String)
    sealed class Response {
        data class ConnectionError(val url: String, val error: Throwable) : Response()
        data class Text(val url: String, val text: String, val httpCode: Int) : Response()
    }
}

interface StockCatalogClient {
    var stockCatalog: StockCatalog
    fun sendStockCatalogQuery(query: StockCatalogQuery) = stockCatalog.sendQuery(query)
    fun onStockCatalogResult(result: StockCatalogResult)
}

fun StockCatalogQuery.sendToClient(client: StockCatalogClient) = client.sendStockCatalogQuery(this)

internal data class FinanceQuote(
    val quoteType: String,
    val symbol: String,
    val longName: String,
    val shortName: String,
    val regularMarketPrice: Double,
    val marketCap: Long,
    val sharesOutstanding: Long
)

internal data class FinanceQuoteResponse(val result: List<FinanceQuote>)
internal data class FinanceRequestResponse(val quoteResponse: FinanceQuoteResponse)

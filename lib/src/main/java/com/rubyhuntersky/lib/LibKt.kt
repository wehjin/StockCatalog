package com.rubyhuntersky.lib

import java.math.BigDecimal

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
    fun sendStockCatalogQuery(query: StockCatalog.Query) = stockCatalog.sendQuery(query)
    fun onStockCatalogResult(result: StockCatalog.Result)
}

data class StockSample(
    val symbol: String,
    val sharePrice: BigDecimal,
    val marketCapitalization: BigDecimal
)

fun StockCatalog.Query.sendToClient(client: StockCatalogClient) = client.sendStockCatalogQuery(this)

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

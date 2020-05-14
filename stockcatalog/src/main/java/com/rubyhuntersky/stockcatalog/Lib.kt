package com.rubyhuntersky.stockcatalog

import java.math.BigDecimal
import java.util.*


interface StockCatalogClient {
    var stockCatalog: StockCatalog
    fun sendStockCatalogQuery(query: StockCatalogQuery) = stockCatalog.sendQuery(query)
    fun onStockCatalogResult(result: StockCatalogResult)
}

sealed class StockCatalogQuery {
    object Clear : StockCatalogQuery()
    data class FindStock(val symbol: String) : StockCatalogQuery()
}

fun StockCatalogQuery.sendToClient(client: StockCatalogClient) = client.sendStockCatalogQuery(this)

sealed class StockCatalogResult {
    data class InvalidSymbol(val symbol: String) : StockCatalogResult()
    data class NetworkError(val reason: String) : StockCatalogResult()
    data class ParseError(val text: String, val reason: String? = null) : StockCatalogResult()
    data class Samples(val search: String, val samples: List<StockSample>) : StockCatalogResult()
}

data class StockSample(
    val symbol: String,
    val sharePrice: BigDecimal,
    val marketWeight: MarketWeight,
    val issuer: String
)

sealed class MarketWeight {
    data class Capitalization(val marketCapitalization: BigDecimal) : MarketWeight()
    object None : MarketWeight()
}

internal fun FinanceRequestResponse.toStockSampleList(): List<StockSample> =
    quoteResponse.result
        .filter { it.regularMarketPrice != null }
        .map { financeQuote ->
            StockSample(
                symbol = financeQuote.symbol.toUpperCase(Locale.ROOT).trim(),
                sharePrice = financeQuote.regularMarketPrice!!.toBigDecimal(),
                marketWeight = financeQuote.marketWeight,
                issuer = financeQuote.longName ?: financeQuote.shortName
            )
        }

private val FinanceQuote.marketWeight
    get() = when (quoteType) {
        "ETF" -> MarketWeight.None
        "MUTUALFUND" -> MarketWeight.None
        "CURRENCY" -> MarketWeight.None
        else -> marketCap
            ?.let { MarketWeight.Capitalization(it.toBigDecimal()) }
            ?: MarketWeight.None
    }

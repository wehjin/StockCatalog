package com.rubyhuntersky.lib

import java.math.BigDecimal


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
    val marketCapitalization: BigDecimal,
    val issuer: String
)

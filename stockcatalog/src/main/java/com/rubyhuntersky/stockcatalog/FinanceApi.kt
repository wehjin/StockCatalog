package com.rubyhuntersky.stockcatalog

import com.beust.klaxon.Klaxon

internal object FinanceApi {

    fun getFindStockHttpNetworkRequest(symbol: String): HttpNetworkRequest =
        getQuotesHttpNetworkRequest(listOf(symbol))

    fun getQuotesHttpNetworkRequest(symbolList: List<String>): HttpNetworkRequest {
        val symbols = symbolList.joinToString("%2C", transform = String::trim)
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
            "lang=en-US&region=US&corsDomain=finance.yahoo.com&fields=$fields&symbols=$symbols&formatted=false"
        val url = "https://query1.finance.yahoo.com/v7/finance/quote?$params"
        return HttpNetworkRequest(url)
    }

    fun parseResponseText(responseText: String): FinanceRequestResponse? =
        Klaxon().parse<FinanceRequestResponse>(responseText)
}

internal data class FinanceQuote(
    val quoteType: String,
    val symbol: String,
    val longName: String?,
    val shortName: String,
    val regularMarketPrice: Double,
    val marketCap: Long,
    val sharesOutstanding: Long
)

internal data class FinanceQuoteResponse(val result: List<FinanceQuote>)
internal data class FinanceRequestResponse(val quoteResponse: FinanceQuoteResponse)

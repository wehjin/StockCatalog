package com.rubyhuntersky.stockcatalog

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.Test
import java.math.BigDecimal

class StockMarketTest {

    private val url =
        "https://query1.finance.yahoo.com/v7/finance/quote?lang=en-US&region=US&corsDomain=finance.yahoo.com&fields=quoteType%2Csymbol%2ClongName%2CshortName%2CregularMarketPrice%2CmarketCap%2CsharesOutstanding&symbols=tsla%2Cnvda&formatted=false"
    private val responseText =
        "{\"quoteResponse\":{\"result\":[{\"language\":\"en-US\",\"region\":\"US\",\"quoteType\":\"EQUITY\",\"currency\":\"USD\",\"sharesOutstanding\":172720992,\"marketCap\":52258463744,\"gmtOffSetMilliseconds\":-18000000,\"sourceInterval\":15,\"exchangeTimezoneName\":\"America/New_York\",\"exchangeTimezoneShortName\":\"EST\",\"shortName\":\"Tesla, Inc.\",\"regularMarketPrice\":302.56,\"regularMarketTime\":1550696401,\"market\":\"us_market\",\"exchange\":\"NMS\",\"fullExchangeName\":\"NasdaqGS\",\"longName\":\"Tesla, Inc.\",\"marketState\":\"POST\",\"esgPopulated\":false,\"tradeable\":true,\"exchangeDataDelayedBy\":0,\"symbol\":\"TSLA\"},{\"language\":\"en-US\",\"region\":\"US\",\"quoteType\":\"EQUITY\",\"currency\":\"USD\",\"sharesOutstanding\":610000000,\"marketCap\":96715505664,\"gmtOffSetMilliseconds\":-18000000,\"sourceInterval\":15,\"exchangeTimezoneName\":\"America/New_York\",\"exchangeTimezoneShortName\":\"EST\",\"shortName\":\"NVIDIA Corporation\",\"regularMarketPrice\":158.55,\"regularMarketTime\":1550696401,\"market\":\"us_market\",\"exchange\":\"NMS\",\"fullExchangeName\":\"NasdaqGS\",\"longName\":\"NVIDIA Corporation\",\"marketState\":\"POST\",\"esgPopulated\":false,\"tradeable\":true,\"exchangeDataDelayedBy\":0,\"symbol\":\"NVDA\"}],\"error\":null}}"

    @Test
    fun fetchSamples() {
        StockMarket.network = mock {
            on { request(HttpNetworkRequest(url)) } doReturn HttpNetworkResponse.Text(url, responseText, 200)
        }

        val test = StockMarket.fetchSamples(listOf("tsla", "nvda")).test()
        verify(StockMarket.network).request(HttpNetworkRequest(url))
        test.assertValue(
            StockMarket.Result.Samples(
                listOf(
                    StockSample(
                        symbol = "TSLA",
                        sharePrice = BigDecimal("302.56"),
                        marketCapitalization = BigDecimal("52258463744"),
                        issuer = "Tesla, Inc."
                    ),
                    StockSample(
                        symbol = "NVDA",
                        sharePrice = BigDecimal("158.55"),
                        marketCapitalization = BigDecimal("96715505664"),
                        issuer = "NVIDIA Corporation"
                    )
                )
            )
        )
    }
}
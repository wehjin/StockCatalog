package com.rubyhuntersky.lib

import com.beust.klaxon.Klaxon
import com.beust.klaxon.json
import com.nhaarman.mockitokotlin2.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class StockCatalogTest {

    private val client = object : StockCatalogClient {
        val resultLatch = CountDownLatch(1)
        val results: MutableList<StockCatalogResult> = mutableListOf()

        override lateinit var stockCatalog: StockCatalog
        override fun onStockCatalogResult(result: StockCatalogResult) {
            results += result
            resultLatch.countDown()
        }
    }

    private val financeRequestResponseJson = json {
        obj(
            "quoteResponse" to obj(
                "result" to array(
                    listOf(
                        obj(
                            "quoteType" to "EQUITY",
                            "sharesOutstanding" to 171732992,
                            "regularMarketPrice" to 347.26,
                            "shortName" to "Tesla, Inc",
                            "longName" to "Tesla, Inc.",
                            "marketCap" to 59635998720,
                            "symbol" to "TSLA"
                        )
                    )
                )
            )
        )
    }
    private val financeRequestResponse =
        Klaxon().parse<FinanceRequestResponse>(financeRequestResponseJson.toJsonString())!!

    @Test
    fun findStockSendsRequestToNetwork() {
        val mockNetwork = mock<HttpNetwork> {}
        StockCatalog(mockNetwork).connect(client)

        StockCatalogQuery.FindStock("TSLA").sendToClient(client)
        verify(mockNetwork).request(check {
            assertEquals(
                "https://query1.finance.yahoo.com/v7/finance/quote?lang=en-US&region=US&corsDomain=finance.yahoo.com&fields=quoteType%2Csymbol%2ClongName%2CshortName%2CregularMarketPrice%2CmarketCap%2CsharesOutstanding&symbols=TSLA&formatted=false",
                it.url
            )
        })
    }

    @Test
    fun findStockSendsResultToClient() {
        val requestLatch = CountDownLatch(1)
        val mockNetwork = mock<HttpNetwork> {
            on { request(argThat { url.contains("ERROR") }) }.doAnswer {
                requestLatch.await()
                HttpNetwork.Response.ConnectionError("url", Exception("No network"))
            }
        }

        StockCatalog(mockNetwork).connect(client)
        StockCatalogQuery.FindStock("ERROR").sendToClient(client)
        requestLatch.countDown()
        if (client.resultLatch.await(1, TimeUnit.SECONDS)) {
            assertTrue(client.results.size == 1 && client.results[0] is StockCatalogResult.NetworkError)
        } else {
            fail("No result")
        }
    }

    @Test
    fun clearCancelsPreviousFind() {
        val requestStartedLatch = CountDownLatch(1)
        val requestEndLatch = CountDownLatch(1)
        val mockNetwork = mock<HttpNetwork> {
            on { request(argThat { url.contains("TSLA") }) }.doAnswer {
                requestStartedLatch.countDown()
                requestEndLatch.await()
                HttpNetwork.Response.Text("url", financeRequestResponseJson.toJsonString(), 200)
            }
        }

        StockCatalog(mockNetwork).connect(client)
        StockCatalogQuery.FindStock("TSLA").sendToClient(client)
        requestStartedLatch.await()
        StockCatalogQuery.Clear.sendToClient(client)
        requestEndLatch.countDown()
        Thread.sleep(10)
        assertEquals(emptyList<StockCatalogResult>(), client.results)
    }

    @Test
    fun secondFindStockCancelsPreviousFind() {
        val request1StartedLatch = CountDownLatch(1)
        val request2StartedLatch = CountDownLatch(1)
        val requestsEndLatch = CountDownLatch(1)
        val mockNetwork = mock<HttpNetwork> {
            on { request(argThat { url.contains("ERROR") }) }.doAnswer {
                request1StartedLatch.countDown()
                requestsEndLatch.await()
                HttpNetwork.Response.ConnectionError("url", Exception("No network"))
            }
            on { request(argThat { url.contains("TSLA") }) }.doAnswer {
                request2StartedLatch.countDown()
                requestsEndLatch.await()
                HttpNetwork.Response.Text("url", financeRequestResponseJson.toJsonString(), 200)
            }
        }

        StockCatalog(mockNetwork).connect(client)
        StockCatalogQuery.FindStock("ERROR").sendToClient(client)
        request1StartedLatch.await()
        StockCatalogQuery.FindStock("TSLA").sendToClient(client)
        request2StartedLatch.await()
        requestsEndLatch.countDown()
        if (client.resultLatch.await(1, TimeUnit.MINUTES)) {
            assertEquals(1, client.results.size)
            assertEquals(
                StockCatalogResult.Samples(
                    search = "TSLA",
                    samples = listOf(
                        StockSample(
                            symbol = "TSLA",
                            sharePrice = BigDecimal.valueOf(financeRequestResponse.quoteResponse.result[0].regularMarketPrice),
                            marketCapitalization = BigDecimal.valueOf(financeRequestResponse.quoteResponse.result[0].marketCap)
                        )
                    )
                ),
                client.results[0]
            )
        } else {
            fail("No result")
        }
    }
}
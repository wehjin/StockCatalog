package com.rubyhuntersky.lib

import com.nhaarman.mockitokotlin2.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class StockCatalogTest {

    private val client = object : StockCatalogClient {
        val resultLatch = CountDownLatch(1)
        val results: MutableList<StockCatalog.Result> = mutableListOf()

        override lateinit var stockCatalog: StockCatalog
        override fun onStockCatalogResult(result: StockCatalog.Result) {
            results += result
            resultLatch.countDown()
        }
    }

    @Test
    fun findStockSendsRequestToNetwork() {
        val mockNetwork = mock<HttpNetwork> {}
        StockCatalog(mockNetwork).connect(client)

        StockCatalog.Query.FindStock("TSLA").send(client)
        verify(mockNetwork).request(argWhere {
            it.url == "https://query1.finance.yahoo.com/v7/finance/quote?lang=en-US&region=US&corsDomain=finance.yahoo.com&fields=symbol%2ClongName%2CshortName%2CregularMarketPrice%2CmarketCap%2CsharesOutstanding&symbols=TSLA&formatted=false"
        })
    }

    @Test
    fun findStockSendsResultToClient() {
        val requestLatch = CountDownLatch(1)
        val mockNetwork = mock<HttpNetwork> {
            on { request(argThat { url.contains("ERROR") }) }.doAnswer {
                requestLatch.await()
                HttpNetwork.Response.ConnectionError(Exception("No network"))
            }
        }

        StockCatalog(mockNetwork).connect(client)
        StockCatalog.Query.FindStock("ERROR").send(client)
        requestLatch.countDown()
        if (client.resultLatch.await(1, TimeUnit.SECONDS)) {
            assertTrue(client.results.size == 1 && client.results[0] is StockCatalog.Result.NetworkError)
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
                HttpNetwork.Response.Text("TSLA", 200)
            }
        }

        StockCatalog(mockNetwork).connect(client)
        StockCatalog.Query.FindStock("TSLA").send(client)
        StockCatalog.Query.Clear.send(client)
        requestStartedLatch.await()
        requestEndLatch.countDown()
        Thread.sleep(10)
        assertEquals(0, client.results.size)
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
                HttpNetwork.Response.ConnectionError(Exception("No network"))
            }
            on { request(argThat { url.contains("TSLA") }) }.doAnswer {
                request2StartedLatch.countDown()
                requestsEndLatch.await()
                HttpNetwork.Response.Text("TSLA", 200)
            }
        }

        StockCatalog(mockNetwork).connect(client)
        StockCatalog.Query.FindStock("ERROR").send(client)
        request1StartedLatch.await()
        StockCatalog.Query.FindStock("TSLA").send(client)
        request2StartedLatch.await()
        requestsEndLatch.countDown()
        if (client.resultLatch.await(1, TimeUnit.SECONDS)) {
            assertTrue(client.results.size == 1 && client.results[0] is StockCatalog.Result.StockData)
        } else {
            fail("No result")
        }
    }
}
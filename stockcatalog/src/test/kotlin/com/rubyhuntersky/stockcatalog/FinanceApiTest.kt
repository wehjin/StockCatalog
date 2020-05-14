package com.rubyhuntersky.stockcatalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FinanceApiTest {

    @Test
    fun etfResponse() {
        val responseText = this::class.java.getResource("etf_response.json").readText()
        val response = FinanceApi.parseResponseText(responseText)
        assertNotNull(response)
    }

    @Test
    fun mfResponse() {
        val responseText = this::class.java.getResource("mf_response.json").readText()
        val response = FinanceApi.parseResponseText(responseText)
        assertNotNull(response)
    }

    @Test
    fun spacResponse() {
        val responseText = this::class.java.getResource("spac_response.json").readText()
        val response = FinanceApi.parseResponseText(responseText)
        assertNotNull(response)
        val list = response!!.toStockSampleList()
        assertEquals(1, list.size)
    }
}
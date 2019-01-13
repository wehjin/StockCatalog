package com.rubyhuntersky.stockcatalog

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SearchView
import android.util.Log
import com.rubyhuntersky.lib.StockCatalog
import com.rubyhuntersky.lib.StockCatalogClient
import com.rubyhuntersky.lib.StockCatalogQuery
import com.rubyhuntersky.lib.StockCatalogResult
import kotlinx.android.synthetic.main.activity_stock_search.*

class StockSearchActivity : AppCompatActivity(), StockCatalogClient {

    override lateinit var stockCatalog: StockCatalog

    override fun onStockCatalogResult(result: StockCatalogResult) {
        Log.d("onStockCatalogResult", result.toString())
        runOnUiThread {
            textView.text = result.toString()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(toolbar)
        setContentView(R.layout.activity_stock_search)
        StockCatalog(OkNetwork).connect(this)
    }

    override fun onStart() {
        super.onStart()
        with(searchView) {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextChange(text: String): Boolean {
                    Log.d("OnQueryTextListener", "New text: $text")
                    textView.text = ""
                    val search = text.trim().toUpperCase()
                    val query = if (search.isBlank()) {
                        StockCatalogQuery.Clear
                    } else {
                        StockCatalogQuery.FindStock(search)
                    }
                    sendStockCatalogQuery(query)
                    return true
                }

                override fun onQueryTextSubmit(text: String): Boolean = false
            })
        }
    }
}

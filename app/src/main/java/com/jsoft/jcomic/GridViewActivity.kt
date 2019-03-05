package com.jsoft.jcomic

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.net.http.HttpResponseCache
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebViewClient
import android.webkit.WebView
import android.widget.GridView
import android.widget.LinearLayout

import com.jsoft.jcomic.adapter.GridViewImageAdapter
import com.jsoft.jcomic.helper.AppConstant
import com.jsoft.jcomic.helper.BookDTO
import com.jsoft.jcomic.helper.BookmarkDb
import com.jsoft.jcomic.helper.Utils

import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayList

class GridViewActivity : AppCompatActivity() {

    private var utils: Utils? = null
    private var gridView: GridView? = null
    private var webView: WebView? = null
    private var books: List<BookDTO>? = null
    private var bookmarkDb: BookmarkDb? = null
    private var gridViewActivity: GridViewActivity? = null

    //permission is automatically granted on sdk<23 upon installation
    val isReadStoragePermissionGranted: Boolean
        get() {
            if (Build.VERSION.SDK_INT >= 23) {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    return true
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 3)
                    return false
                }
            } else {
                return true
            }
        }

    //permission is automatically granted on sdk<23 upon installation
    val isWriteStoragePermissionGranted: Boolean
        get() {
            if (Build.VERSION.SDK_INT >= 23) {
                if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    return true
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 2)
                    return false
                }
            } else {
                return true
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bookmarkDb = BookmarkDb(this)

        enableHttpCaching()
        gridViewActivity = this
        setContentView(R.layout.activity_grid_view)
        utils = Utils(this)
        initWebView()

        gridView = initGridLayout()
        //bookmarkDb.clearDb();
        isReadStoragePermissionGranted
        isWriteStoragePermissionGranted
    }

    public override fun onResume() {
        super.onResume()
        books = bookmarkDb!!.bookmarkedList
        val view = findViewById<LinearLayout>(R.id.button_list_link)
        if (!Utils.isInternetAvailable) {
            view.visibility = View.GONE
        } else {
            view.visibility = View.VISIBLE
        }
        val adapter = GridViewImageAdapter(this@GridViewActivity, books!!)
        gridView!!.adapter = adapter
    }

    override fun onBackPressed() {
        if (webView!!.visibility == View.VISIBLE && webView!!.canGoBack()) {
            webView!!.goBack()
            return
        } else if (webView!!.visibility == View.VISIBLE) {
            webView!!.visibility = View.GONE
            gridView!!.visibility = View.VISIBLE
            return
        }
        // Otherwise defer to system default behavior.
        super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.book_list_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_download -> {
                val intent = Intent(this, DownloadListActivity::class.java)
                this.startActivityForResult(intent, 0)
                invalidateOptionsMenu()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun initGridLayout(): GridView {
        val r = resources
        gridView = findViewById(R.id.grid_view)
        val padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                AppConstant.GRID_PADDING.toFloat(), r.displayMetrics)

        val numColumns = 3
        val columnWidth = ((utils!!.screenWidth - (numColumns + 1) * padding) / numColumns).toInt()

        gridView!!.numColumns = numColumns
        gridView!!.columnWidth = columnWidth
        gridView!!.stretchMode = GridView.NO_STRETCH
        gridView!!.setPadding(padding.toInt(), padding.toInt(), padding.toInt(),
                padding.toInt())
        gridView!!.horizontalSpacing = padding.toInt()
        gridView!!.verticalSpacing = padding.toInt()

        return gridView!!
    }

    private fun initWebView() {
        webView = findViewById(R.id.web_view)
        webView!!.visibility = View.INVISIBLE
        val webSettings = webView!!.settings
        webSettings.javaScriptEnabled = true
        webView!!.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                try {
                    val uri = request.url
                    if (uri.host!!.contains("cartoonmad.com") && uri.path!!.startsWith("/m/comic/") || uri.host!!.contains("comicbus.com") && uri.path!!.startsWith("/comic/")) {
                        val i = Intent(gridViewActivity, EpisodeListActivity::class.java)
                        i.putExtra("bookUrl", uri.toString())
                        startActivity(i)
                        return true
                    } else if (uri.host!!.contains("dm5.com")) {
                        InterceptDM5Task("UTF-8", view).execute(URL(uri.toString()))
                        return true
                    }
                } catch (e: Exception) {
                    Log.e("jComics", "Caught by shouldOverrideUrlLoading", e)
                }

                return false
            }
        }
    }

    inner class InterceptDM5Task(private val encoding: String, private val wv: WebView) : AsyncTask<URL, Int, List<String>>() {
        private var url: String? = null

        override fun doInBackground(vararg urls: URL): List<String> {
            val result = ArrayList<String>()
            val urlConn = urls[0]
            this.url = urls[0].toString()
            try {
                val conn = urlConn.openConnection() as HttpURLConnection
                conn.readTimeout = 5000
                conn.useCaches = true
                conn.setRequestProperty("Referer", "http://m.dm5.com/manhua-list/")
                val `is` = BufferedInputStream(conn.inputStream)
                val `in` = BufferedReader(InputStreamReader(`is`, encoding))
                var readLine: String
                do {
                    readLine = `in`.readLine()
                    result.add(readLine)
                } while (readLine != null)
                `in`.close()
                `is`.close()
            } catch (e: Exception) {
                Log.e("jComics", "Caught by InterceptDM5Task", e)
            }

            return result
        }

        override fun onPostExecute(result: List<String>) {
            var data = ""
            if (result.size > 0) {
                for (i in result.indices) {
                    data = data + "\n" + result[i].replace("\\s{4,}".toRegex(), "\n")
                }
            }
            if (data.contains("chapteritem")) {
                val i = Intent(gridViewActivity, EpisodeListActivity::class.java)
                i.putExtra("bookUrl", url)
                gridViewActivity!!.startActivity(i)
            } else {
                data = data.replace("var tagid = \"(.*)\";".toRegex(), "var tagid = \"$1\"; var categoryid = \"0\"")
                wv.loadDataWithBaseURL(url, data, "text/html; charset=utf-8", "utf-8", null)
            }
        }
    }

    fun getEpisodeList(position: Int) {
        val i = Intent(this, EpisodeListActivity::class.java)
        i.putExtra("bookUrl", books!![position].bookUrl)
        startActivityForResult(i, position)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

    }

    fun goToCartoonMad(view: View) {
        openWebView("https://www.cartoonmad.com/m/?act=2")
    }

    fun goTo8Comic(view: View) {
        openWebView("http://m.comicbus.com/")
    }

    fun goToDM5(view: View) {
        webView!!.visibility = View.VISIBLE
        gridView!!.visibility = View.GONE
        try {
            InterceptDM5Task("UTF-8", webView!!).execute(URL("http://m.dm5.com/manhua-list/"))
        } catch (e: Exception) {
            Log.e("jComics", "Caught by goToDM5", e)
        }

    }

    fun goToHome(view: View) {
        webView!!.visibility = View.GONE
        gridView!!.visibility = View.VISIBLE
    }

    private fun openWebView(url: String) {
        webView!!.visibility = View.VISIBLE
        gridView!!.visibility = View.GONE
        webView!!.loadUrl(url)
    }

    private fun enableHttpCaching() {
        try {
            val httpCacheDir = File(applicationContext.cacheDir, "http")
            val httpCacheSize = (100 * 1024 * 1024).toLong() // 10 MiB
            HttpResponseCache.install(httpCacheDir, httpCacheSize)
        } catch (e: IOException) {
            Log.e("jComics", "Exception caught in enableHttpCaching", e)
        }

    }
}

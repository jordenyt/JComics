package com.jsoft.jcomic.praser

import android.net.Uri
import android.os.AsyncTask
import android.util.Log

import com.jsoft.jcomic.helper.BookDTO

import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.ArrayList

abstract class BookParser(protected var book: BookDTO, protected var listener: BookParserListener, encoding: String) {

    init {
        try {
            DownloadFilesTask(encoding).execute(URL(book.bookUrl))
        } catch (e: MalformedURLException) {
            Log.e("jComics", "MalformedURLException: " + book.bookUrl!!)
        }
    }

    inner class DownloadFilesTask(private val encoding: String) : AsyncTask<URL, Int, ArrayList<String>>() {

        override fun doInBackground(vararg urls: URL): ArrayList<String> {

            var result: ArrayList<String> = ArrayList()
            for (url in urls) {
                var readLine: String?
                val uri = Uri.parse(url.toString())
                try {
                    val conn = url.openConnection() as HttpURLConnection
                    conn.readTimeout = 5000
                    conn.useCaches = true
                    if (uri.host!!.contains("dm5.com") && uri.getQueryParameter("from") != null) {
                        conn.setRequestProperty("Referer", "http://m.dm5.com" + uri.getQueryParameter("from")!!)
                    }
                    val bis = BufferedInputStream(conn.inputStream)
                    val br = BufferedReader(InputStreamReader(bis, encoding))
                    readLine = br.readLine()
                    while (readLine != null) {
                        result.add(readLine)
                        readLine = br.readLine()
                    }
                    br.close()
                    bis.close()
                } catch (e: Exception) {
                    result = ArrayList()
                    Log.e("jComics", "Exception when getting file: $url", e)
                }

            }
            return result
        }

        override fun onProgressUpdate(vararg progress: Int?) {
            //setProgressPercent(progress[0]);
        }

        override fun onPostExecute(result: ArrayList<String>) {
            if (result.size > 0) {
                getBookFromUrlResult(result)
                listener.onBookFetched(book)
            } else if (book.bookSynopsis == null) {
                book.bookSynopsis = "Cannot load page from Internet"
                listener.onBookFetched(book)
            }
        }
    }

    protected open fun getBookFromUrlResult(html: ArrayList<String>) {}

    companion object {

        fun parseBook(book: BookDTO, listener: BookParserListener) {
            if (book.bookUrl!!.contains("comicbus")) {
                ComicVIPBookParser(book, listener)
            } else if (book.bookUrl!!.contains("cartoonmad")) {
                CartoonMadBookParser(book, listener)
            } else if (book.bookUrl!!.contains("dm5.com")) {
                DM5BookParser(book, listener)
            }
        }
    }
}

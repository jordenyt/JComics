package com.jsoft.jcomic.praser

import android.net.Uri
import android.os.AsyncTask
import android.util.Log
import com.jsoft.jcomic.helper.BookDTO
import com.jsoft.jcomic.helper.Utils
import java.net.MalformedURLException
import java.net.URL
import java.util.*

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
            val uri = Uri.parse(urls[0].toString())
            var referer : String? = null
            if (uri.host!!.contains("dm5.com") && uri.getQueryParameter("from") != null) {
                referer = "http://m.dm5.com" + uri.getQueryParameter("from")
            }
            return Utils.getURLResponse(urls[0], referer, encoding)
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
            when {
                book.bookUrl!!.contains("comicbus") -> ComicVIPBookParser(book, listener)
                book.bookUrl!!.contains("cartoonmad") -> CartoonMadBookParser(book, listener)
                book.bookUrl!!.contains("dm5.com") -> DM5BookParser(book, listener)
            }
        }
    }
}
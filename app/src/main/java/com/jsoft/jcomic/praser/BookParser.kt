package com.jsoft.jcomic.praser

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
            return getURLResponse(urls[0], encoding)
        }

        override fun onPostExecute(result: ArrayList<String>) {
            if (result.size > 0) {
                val finished = getBookFromUrlResult(result)
                if (finished) {
                    loadedAllEpisodes();
                }
            } else if (book.bookSynopsis == null) {
                book.bookSynopsis = "Cannot load page from Internet"
                listener.onBookFetched(book)
            }
        }
    }

    protected open fun loadedAllEpisodes() {
        val myBookFile = Utils.getBookFile(book)
        if (myBookFile.exists()) Utils.saveBook(book)
        listener.onBookFetched(book)
    }

    protected open fun getURLResponse(url: URL, encoding: String): ArrayList<String> {
        return Utils.getURLResponse(url, null, encoding)
    }

    protected open fun getBookFromUrlResult(html: ArrayList<String>): Boolean {
        return true
    }

    companion object {

        fun parseBook(book: BookDTO, listener: BookParserListener) {
            when {
                book.bookUrl!!.contains("comicbus") -> ComicVIPBookParser(book, listener)
                book.bookUrl!!.contains("cartoonmad") -> CartoonMadBookParser(book, listener)
                book.bookUrl!!.contains("dm5.com") -> DM5BookParser(book, listener)
                book.bookUrl!!.contains("qimiaomh.com") -> QiMiaoBookParser(book, listener)
                book.bookUrl!!.contains("kuman5.com") -> KuMan5BookParser(book, listener)
            }
        }
    }
}

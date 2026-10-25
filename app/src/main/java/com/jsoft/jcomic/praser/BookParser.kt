package com.jsoft.jcomic.praser

import android.content.Context
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
            val target = normalizeBookUrl(book.bookUrl ?: "")
            book.bookUrl = target
            DownloadFilesTask(encoding).execute(URL(target))
        } catch (e: MalformedURLException) {
            Log.e("jComics", "MalformedURLException: " + book.bookUrl!!)
        }
    }

    inner class DownloadFilesTask(private val encoding: String) : AsyncTask<URL, Int, ArrayList<String>>() {

        override fun doInBackground(vararg urls: URL): ArrayList<String> {
            return getURLResponse(urls[0], encoding)
        }

        override fun onPostExecute(result: ArrayList<String>) {
            try {
                if (result.size > 0) {
                    val finished = getBookFromUrlResult(result)
                    if (finished) {
                        loadedAllEpisodes()
                    }
                } else if (book.bookSynopsis == null) {
                    book.bookSynopsis = "Cannot load page from Internet"
                    listener.onBookFetched(book)
                }
            } catch (e: Exception) {
                Log.e("jComics", "Exception caught in BookParser", e)
                if (book.bookSynopsis == null) {
                    book.bookSynopsis = "Cannot load page from Internet"
                }
                listener.onBookFetched(book)
            }
        }
    }

    protected open fun loadedAllEpisodes() {
        val myBookFile = Utils.getBookFile(book)
        if (myBookFile.exists()) Utils.saveBook(book)
        listener.onBookFetched(book)
    }

    /**
     * Sites that are served by more than one host can rewrite the url of the book here, the parser
     * fetches the returned url and the book keeps it.
     */
    protected open fun normalizeBookUrl(url: String): String {
        return url
    }

    protected open fun getURLResponse(url: URL, encoding: String): ArrayList<String> {
        return Utils.getURLResponse(url, null, encoding)
    }

    protected open fun getBookFromUrlResult(html: ArrayList<String>): Boolean {
        return true
    }

    companion object {

        fun parseBook(book: BookDTO, listener: BookParserListener, context: Context) {
            when {
                book.bookUrl!!.contains("8comic") -> Comic8BookParser(book, listener)
                book.bookUrl!!.contains("manga.bilibili.com") -> BilibiliMangaBookParser(book, listener, context)
                book.bookUrl!!.contains("kalamanhua.com") -> KalamBookParser(book, listener)
                book.bookUrl!!.contains("dm5.com") -> DM5BookParser(book, listener)
                book.bookUrl!!.contains("qimiaomh.com") -> QiMiaoBookParser(book, listener)
                book.bookUrl!!.contains("kuman5.com") -> KuMan5BookParser(book, listener)
            }
        }
    }
}

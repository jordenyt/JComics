package com.jsoft.jcomic.praser

import android.util.Log
import com.jsoft.jcomic.helper.BookDTO
import com.jsoft.jcomic.helper.EpisodeDTO
import com.jsoft.jcomic.helper.KalamSite
import java.net.URL

/**
 * Parser for the comics of kalamanhua.com (卡拉漫画).
 *
 * The page of the comic has the title, the cover, the description and the list of the chapters.
 * The site puts an access check in front of its pages, the check is solved by KalamSite.
 */
class KalamBookParser(
    book: BookDTO,
    listener: BookParserListener
) : BookParser(book, listener, "UTF-8") {

    override fun getURLResponse(url: URL, encoding: String): ArrayList<String> {
        val comicId = KalamSite.comicId(book.bookUrl)
        if (comicId == null) {
            Log.e("jComics", "kalamanhua: cannot read the comic id from " + book.bookUrl)
            return arrayListOf()
        }
        val page = KalamSite.readPage(KalamSite.bookUrl(comicId))
        if (page.isEmpty()) {
            return arrayListOf()
        }
        return arrayListOf(page)
    }

    override fun getBookFromUrlResult(html: ArrayList<String>): Boolean {
        val page = html.firstOrNull() ?: return false
        if (KalamSite.isCheckPage(page)) {
            Log.e("jComics", "kalamanhua: the access check of the site was not passed")
            return false
        }
        val comicId = KalamSite.comicId(book.bookUrl)
        if (comicId == null) {
            return false
        }
        try {
            val title = TITLE.find(page)?.groupValues?.get(1)?.trim()
            if (!title.isNullOrEmpty()) {
                book.bookTitle = title
            }
            val intro = INTRO.find(page)?.groupValues?.get(1)?.trim()
            if (!intro.isNullOrEmpty()) {
                book.bookSynopsis = intro
            }
            val cover = COVER.find(page)?.groupValues?.get(1)
            if (!cover.isNullOrEmpty()) {
                book.bookImgUrl = cover
            }

            val list = ArrayList<EpisodeDTO>()
            val numbers = HashSet<String>()
            for (match in CHAPTER.findAll(page)) {
                if (match.groupValues[1] != comicId) {
                    continue
                }
                val number = match.groupValues[2]
                val name = match.groupValues[3].trim()
                // The page of the comic has a button that starts the reader of the first chapter,
                // it is not a chapter of the list.
                if (name.isEmpty() || name.contains("播放")) {
                    continue
                }
                if (!numbers.add(number)) {
                    continue
                }
                list.add(EpisodeDTO(name, KalamSite.chapterUrl(comicId, number)))
            }
            // The site lists the chapters from the oldest to the newest one, the app expects the
            // newest chapter first - the next chapter of the reader is the one before it.
            list.reverse()
            book.episodes = list
            Log.d("jComics", "kalamanhua: " + book.bookTitle + " has " + list.size + " chapters")
        } catch (e: Exception) {
            Log.e("jComics", "kalamanhua: cannot read the page of the comic", e)
            return false
        }
        return true
    }

    companion object {
        private val TITLE = Regex("""<h1 class="title">([^<]*)</h1>""")
        private val INTRO = Regex("""<span class="detail-content"[^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
        private val COVER = Regex("data-original=\"(https://image\\.kalaimg\\.top/image/cover/[^\"]+)\"")

        // The chapters of the first line of the comic, they are the episodes of the comic.
        private val CHAPTER = Regex("""<a href="/chapter/(\d+)-1-(\d+)\.html"[^>]*>([^<]{0,60})</a>""")
    }
}

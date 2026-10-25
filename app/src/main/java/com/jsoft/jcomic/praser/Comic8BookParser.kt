package com.jsoft.jcomic.praser

import com.jsoft.jcomic.helper.BookDTO
import com.jsoft.jcomic.helper.Comic8Url
import com.jsoft.jcomic.helper.EpisodeDTO
import com.jsoft.jcomic.helper.Utils
import java.net.URL
import java.util.*
import java.util.regex.Pattern

class Comic8BookParser(book: BookDTO, listener: BookParserListener) : BookParser(book, listener, "UTF-8") {

    override fun normalizeBookUrl(url: String): String {
        return Comic8Url.normalize(url) ?: url
    }

    /**
     * The site is served by several hosts, only some of them can be reached from every network.
     * Try the canonical host first and the others afterwards.
     */
    override fun getURLResponse(url: URL, encoding: String): ArrayList<String> {
        var fallback = ArrayList<String>()
        for (candidate in candidateUrls(url.toString())) {
            val lines = Utils.getURLResponse(URL(candidate), null, encoding)
            if (lines.isEmpty()) continue
            if (fallback.size < lines.size) fallback = lines
            if (lines.any { CHAPTER_MARKER.containsMatchIn(it) }) return lines
        }
        return fallback
    }

    private fun candidateUrls(url: String): List<String> {
        val candidates = ArrayList<String>()
        val bookId = Comic8Url.bookId(url)
        if (bookId != null) {
            candidates.add("https://v.8comic.com/html/$bookId.html")
            candidates.add("http://v.8comic.com/html/$bookId.html")
            candidates.add("https://www.8comic.com/html/$bookId.html")
        }
        candidates.add(url)
        return candidates.distinct()
    }

    //Call when URL is fetched
    override fun getBookFromUrlResult(html: ArrayList<String>): Boolean {
        val episodes = ArrayList<EpisodeDTO>()
        for (i in 3 until html.size) {
            var s = html[i - 3].trim { it <= ' '} + html[i - 2].trim { it <= ' '} + html[i - 1].trim { it <= ' '} + html[i].trim { it <= ' ' }
            s = s.replace("\n", "")

            //<a href='#' onclick="cview('13313-1.html',6,0);return false;" id="c1" class="Ch eps_a d-block" >
            //1話
            //</a>
            var p = Pattern.compile(".*<a href='#' onclick=\"cview\\('(\\d+)-(.+)\\.html',(\\d+),(\\d+)\\);return false;\" id=\"[^\"]*\" class=\"(Vol|Ch) eps_a d-block\" >\\s*(.+)</a>.*")
            var m = p.matcher(s)
            if (m.matches()) {
                // https://v.8comic.com/online/new-13313.html?ch=1
                val chapter = Comic8Chapter.param(m.group(2) ?: "")
                if (chapter.isEmpty()) {
                    continue
                }
                val episodeUrl = "$EPISODE_HOST/online/new-" + m.group(1) + ".html?ch=" + chapter
                val episodeTitle = (m.group(6) ?: "").replace("<script>.*?</script>".toRegex(), "").replace("<.*?>".toRegex(), "")
                var found = false
                for (e in episodes) {
                    if (e.episodeUrl.trim() == episodeUrl) {
                        found = true
                        break
                    }
                }
                if (!found) {
                    episodes.add(0, EpisodeDTO(episodeTitle, episodeUrl))
                }
            }

            //<meta name="name" content="關于我轉生後成為史萊姆的那件事" />
            p = Pattern.compile(".*<meta name=\"name\" content=\"(.+?)\" />.*")
            m = p.matcher(s)
            if (m.matches()) {
                val bookTitle = (m.group(1) ?: "").replace("&nbsp;", " ").replace("<.*?>".toRegex(), "")
                book.bookTitle = bookTitle
            }

            //<li class="h2 mb-1">鬼滅之刃</li>
            p = Pattern.compile(".*<li class=\"h2 mb-1\">(.+?)</li>.*")
            m = p.matcher(s)
            if (m.matches()) {
                val bookTitle = (m.group(1) ?: "").replace("&nbsp;", " ").replace("<.*?>".toRegex(), "")
                if (book.bookTitle.isNullOrEmpty() || book.bookTitle == "Loading...") {
                    book.bookTitle = bookTitle
                }
            }

            //<li class="item_info_detail">
            //　　普通上班族三上悟(37,童貞)遇刺身亡.迎接他的,是轉生後的異世界史萊姆生活..
            //<span class="gradient"></span>
            p = Pattern.compile(".*<li class=\"item_info_detail\">(.*?)<span class=\"gradient\"></span>")
            m = p.matcher(s)
            if (m.matches()) {
                val bookSynopsis = (m.group(1) ?: "").replace("&nbsp;", " ").replace("<.*?>".toRegex(), "")
                book.bookSynopsis = bookSynopsis
            }

            //<div class="d-none d-md-block col-md-2 p-0 item-cover">
            //<img src="/pics/0/13313.jpg">
            //</div>
            p = Pattern.compile(".*<div class=\"d-none d-md-block col-md-2 p-0 item-cover\"><img src=\"(.+)\"></div>.*")
            m = p.matcher(s)
            if (m.matches()) {
                book.bookImgUrl = "$COVER_HOST" + m.group(1)
            }
        }
        book.episodes = episodes
        return true
    }

    companion object {
        // The episode pages are served by this host without needing any referer.
        private const val EPISODE_HOST = "https://v.8comic.com"
        private const val COVER_HOST = "https://v.8comic.com"

        private val CHAPTER_MARKER = Regex("""eps_a|cview\(""")
    }
}

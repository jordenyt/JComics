package com.jsoft.jcomic.praser

import com.jsoft.jcomic.helper.BookDTO
import com.jsoft.jcomic.helper.EpisodeDTO
import java.util.*
import java.util.regex.Pattern

class ComicVIPBookParser(book: BookDTO, listener: BookParserListener) : BookParser(book, listener, "BIG5") {

    //Call when URL is fetched
    override fun getBookFromUrlResult(html: ArrayList<String>) {
        val episodes = ArrayList<EpisodeDTO>()
        //Log.e("jComics", "${book.bookUrl}")
        for (i in 3 until html.size) {
            var s = html[i - 3].trim { it <= ' '} + html[i - 2].trim { it <= ' '} + html[i - 1].trim { it <= ' ' } + html[i].trim { it <= ' ' }
            s = s.replace("\n", "")

            var p = Pattern.compile(".*<a href='#' onclick=\"cview\\('(.+)-(.+)\\.html',(\\d+),(\\d+)\\);return false;\" id=\".+\" class=\"(Vol|Ch)\" >\\s*(.+)</a>.*")
            var m = p.matcher(s)
            if (m.matches()) {
                var baseurl = "http://m.comicgood.com"
                baseurl += "/comic/finance_"
                val episodeUrl = baseurl + m.group(1) + ".html?ch=" + m.group(2)
                val episodeTitle = m.group(6).replace("<script>.*?</script>".toRegex(), "").replace("<.*?>".toRegex(), "")
                var found = false
                for (e in episodes) {
                    if (e.episodeUrl.trim() == episodeUrl) {
                        found = true
                        break
                    }
                }
                if (!found) {
                    episodes.add(0, EpisodeDTO(episodeTitle, episodeUrl))
                    //Log.e("jComics", "$episodeTitle - $episodeUrl")
                }

            }

            p = Pattern.compile(".*<p class=\"title\">(.+?)</p>.*")
            m = p.matcher(s)
            if (m.matches()) {
                book.bookTitle = m.group(1).replace("&nbsp;", " ").replace("<.*?>".toRegex(), "")
            }

            p = Pattern.compile(".*<div class=\"item_show_detail\"><ul><li>(.+?)</li></ul>.*")
            m = p.matcher(s)
            if (m.matches()) {
                book.bookSynopsis = m.group(1).replace("&nbsp;", " ").replace("<.*?>".toRegex(), "")
            }

            p = Pattern.compile(".*<div class=\"cover\">.*<img src='(.+)'>.*")
            m = p.matcher(s)
            if (m.matches()) {
                book.bookImgUrl = "https://m.comicgood.com" + m.group(1)
            }

        }
        book.episodes = episodes
    }
}

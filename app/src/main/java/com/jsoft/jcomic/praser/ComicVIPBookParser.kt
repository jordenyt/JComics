package com.jsoft.jcomic.praser

//import android.util.Log
import com.jsoft.jcomic.helper.BookDTO
import com.jsoft.jcomic.helper.EpisodeDTO
import java.util.*
import java.util.regex.Pattern

class ComicVIPBookParser(book: BookDTO, listener: BookParserListener) : BookParser(book, listener, "UTF-8") {

    //Call when URL is fetched
    override fun getBookFromUrlResult(html: ArrayList<String>):Boolean {
        val episodes = ArrayList<EpisodeDTO>()
        //Log.e("jComics", "${book.bookUrl}")
        for (i in 3 until html.size) {
            var s = html[i - 3].trim { it <= ' '} + html[i - 2].trim { it <= ' '} + html[i - 1].trim { it <= ' ' } + html[i].trim { it <= ' ' }
            s = s.replace("\n", "")

            var p = Pattern.compile(".*<a href='#' onclick=\"cview\\('(.+)-(.+)\\.html',(\\d+),(\\d+)\\);return false;\" id=\".+\" class=\"(Vol|Ch)\" >\\s*(.+)</a>.*")
            var m = p.matcher(s)
            if (m.matches()) {
                var baseurl = "https://8.twobili.com"
                baseurl += "/comic/insurance_"
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

            p = Pattern.compile(".*<h6 class=\"title\">(.+?)</h6>.*")
            m = p.matcher(s)
            if (m.matches()) {
                val bookTitle = m.group(1).replace("&nbsp;", " ").replace("<.*?>".toRegex(), "")
                //Log.e("jComics", "bookTitle = $bookTitle")
                book.bookTitle = bookTitle
            }

            p = Pattern.compile(".*<div class=\"full_text\" style=\".+?\">(.+?)</div>.*")
            m = p.matcher(s)
            if (m.matches()) {
                val bookSynopsis = m.group(1).replace("&nbsp;", " ").replace("<.*?>".toRegex(), "")
                //Log.e("jComics", "bookSynopsis = $bookSynopsis")
                book.bookSynopsis =  bookSynopsis
            }

            p = Pattern.compile(".*<li class=\"cover\">.*<img src='(.+)'>.*")
            m = p.matcher(s)
            if (m.matches()) {
                book.bookImgUrl = "https://m.comicbus.com" + m.group(1)
            }

        }
        book.episodes = episodes
        return true
    }
}

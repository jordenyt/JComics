package com.jsoft.jcomic.praser

import android.text.Html
import com.jsoft.jcomic.helper.BookDTO
import com.jsoft.jcomic.helper.EpisodeDTO
import java.util.*
import java.util.regex.Pattern

class ComicVIPBookParser(book: BookDTO, listener: BookParserListener) : BookParser(book, listener, "BIG5") {

    //Call when URL is fetched
    override fun getBookFromUrlResult(html: ArrayList<String>) {
        val episodes = ArrayList<EpisodeDTO>()
        for (i in 1 until html.size) {
            var s = html[i - 1].trim { it <= ' ' } + html[i].trim { it <= ' ' }
            s = s.replace("\n", "")

            //Pattern p = Pattern.compile(".*<a href='#' onclick=\"cview\\('(.+)-(.+)\\.html',(.+)\\);return false;\" id=\".+\" class=\".+\">\\s*(.+)</a>.*");
            val baseurl = "https://m.comicgood.com"

            var p = Pattern.compile(".*<td style=\".*\">.*<a href='(.+)' class=\"(Vol|Ch)\"\\s+id=\"(.+)\"\\s+>\\s*(.+)</a>.*")
            var m = p.matcher(s)
            if (m.matches()) {
                val episodeUrl = baseurl + m.group(1)
                val episodeTitle = m.group(4).replace("<script>.*?</script>".toRegex(), "").replace("<.*?>".toRegex(), "")
                episodes.add(0, EpisodeDTO(episodeTitle, episodeUrl))
            }

            // var p = Pattern.compile(".*<a href='#' onclick=\"cview\\('(.+)-(.+)\\.html',(.+)\\);return false;\" id=\".+\" class=\".+\">\\s*(.+)</a>.*")
            p = Pattern.compile(".*<a href='#' onclick=\"cview\\('(.+)-(.+)\\.html',(\\d+),(\\d+)\\);return false;\" id=\".+\" class=\"(Vol|Ch)\" >\\s*(.+)</a>.*")
            m = p.matcher(s)
            if (m.matches()) {
                var baseurl = "http://m.comicgood.com"
                baseurl += "/comic/finance_"
                val episodeUrl = baseurl + m.group(1) + ".html?ch=" + m.group(2)
                val episodeTitle = m.group(6).replace("<script>.*?</script>".toRegex(), "").replace("<.*?>".toRegex(), "")
                episodes.add(0, EpisodeDTO(episodeTitle, episodeUrl))
            }

            p = Pattern.compile(".*<font color=\"#FF3300\" style=\"font:14pt;font-weight:bold;\">(.+)</font>.*")
            m = p.matcher(s)
            if (m.matches()) {
                book.bookTitle = m.group(1).replace("&nbsp;", " ").replace("<.*?>".toRegex(), "")
            }

            p = Pattern.compile(".*<td colspan=\"3\" valign=\"top\" bgcolor=\"f8f8f8\" style=\"padding:10px;line-height:25px\">(.+)</td>.*")
            m = p.matcher(s)
            if (m.matches()) {
                book.bookSynopsis = Html.fromHtml(m.group(1)).toString()
            }

            p = Pattern.compile(".*<td align=\"center\" bgcolor=\"fafafa\"><img src='(.+)' hspace=\"10\" vspace=\"10\" border=\"0\" />.*")
            //<td align="center" bgcolor="fafafa"><img src='/pics/0/3654s.jpg' hspace="10" vspace="10" border="0" />
            //<td align="center" bgcolor="fafafa"><img src='/pics/0/103s.jpg' hspace="10" vspace="10" border="0" />
            m = p.matcher(s)
            if (m.matches()) {
                book.bookImgUrl = "https://m.comicgood.com" + m.group(1).replace("(\\d+)s\\.".toRegex(), "$1.")
            }

        }
        book.episodes = episodes
    }
}

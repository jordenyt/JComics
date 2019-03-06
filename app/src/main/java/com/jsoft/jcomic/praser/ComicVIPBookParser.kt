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
        for (i in 2 until html.size) {
            var s = html[i - 2].trim { it <= ' ' } + html[i - 1].trim { it <= ' ' } + html[i].trim { it <= ' ' }
            s = s.replace("\n", "")

            if (book.bookUrl.contains("www.comicbus.com")) {
                var p = Pattern.compile(".*<a href='#' onclick=\"cview\\('(.+)-(.+)\\.html',(.+)\\);return false;\" id=\".+\" class=\".+\">\\s*(.+)</a>.*")
                var m = p.matcher(s)
                if (m.matches()) {
                    val catid = Integer.parseInt(m.group(3))
                    var baseurl = "https://www.comicbus.com"
                    if (catid == 4 || catid == 6 || catid == 12 || catid == 22)
                        baseurl += "/show/cool-"
                    else if (catid == 1 || catid == 17 || catid == 19 || catid == 21)
                        baseurl += "/show/cool-"
                    else if (catid == 2 || catid == 5 || catid == 7 || catid == 9)
                        baseurl += "/show/cool-"
                    else if (catid == 10 || catid == 11 || catid == 13 || catid == 14)
                        baseurl += "/show/best-manga-"
                    else if (catid == 3 || catid == 8 || catid == 15 || catid == 16 || catid == 18 || catid == 20)
                        baseurl += "/show/best-manga-"
                    val episodeUrl = baseurl + m.group(1) + ".html?ch=" + m.group(2)
                    val episodeTitle = m.group(4).replace("<script>.*?</script>".toRegex(), "").replace("<.*?>".toRegex(), "").replace("(.*?)\\s.*".toRegex(), "$1")
                    episodes.add(0, EpisodeDTO(episodeTitle, episodeUrl))
                }

                p = Pattern.compile(".*<font color=\"#006666\">(.+)<b>.*")
                m = p.matcher(s)
                if (m.matches()) {
                    book.bookTitle = m.group(1).replace("&nbsp;", " ")
                }

                p = Pattern.compile(".*<td colspan=\"3\" valign=\"top\" bgcolor=\"f0f8ff\" style=\"padding:10px;line-height:25px\">(.+)</td>.*")
                m = p.matcher(s)
                if (m.matches()) {
                    book.bookSynopsis = Html.fromHtml(m.group(1)).toString()
                }

                p = Pattern.compile(".*<img src='(.+)' hspace=\"10\" vspace=\"10\" border=\"0\" style=\"border:#CCCCCC solid 1px\" />.*")
                m = p.matcher(s)
                if (m.matches()) {
                    book.bookImgUrl = "https://www.comicbus.com" + m.group(1)
                }
            } else if (book.bookUrl.contains("m.comicbus.com")) {
                //Pattern p = Pattern.compile(".*<a href='#' onclick=\"cview\\('(.+)-(.+)\\.html',(.+)\\);return false;\" id=\".+\" class=\".+\">\\s*(.+)</a>.*");
                val baseurl = "https://m.comicbus.com"

                var p = Pattern.compile(".*<td style=\".*\">.*<a href='(.+)' class=\"(Vol|Ch)\"  id=\"(.+)\"  >\\s*(.+)</a>.*")
                var m = p.matcher(s)
                if (m.matches()) {
                    val episodeUrl = baseurl + m.group(1)
                    val episodeTitle = m.group(4).replace("<script>.*?</script>".toRegex(), "").replace("<.*?>".toRegex(), "")
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
                    book.bookImgUrl = "https://m.comicbus.com" + m.group(1).replace("(\\d+)s\\.".toRegex(), "$1.")
                }
            }
        }
        for (episode in episodes) {
            episode.bookTitle = book.bookTitle
        }
        book.episodes = episodes
    }
}

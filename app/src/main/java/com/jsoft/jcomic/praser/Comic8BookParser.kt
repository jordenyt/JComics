package com.jsoft.jcomic.praser

//import android.util.Log
import com.jsoft.jcomic.helper.BookDTO
import com.jsoft.jcomic.helper.EpisodeDTO
import java.util.*
import java.util.regex.Pattern

class Comic8BookParser(book: BookDTO, listener: BookParserListener) : BookParser(book, listener, "UTF-8") {

    //Call when URL is fetched
    override fun getBookFromUrlResult(html: ArrayList<String>):Boolean {
        val episodes = ArrayList<EpisodeDTO>()
        //Log.e("jComics", "${book.bookUrl}")
        for (i in 3 until html.size) {
            var s = html[i - 3].trim { it <= ' '} + html[i - 2].trim { it <= ' '} + html[i - 1].trim { it <= ' ' } + html[i].trim { it <= ' ' }
            s = s.replace("\n", "")

            var p = Pattern.compile(".*<a href='#' onclick=\"cview\\('(.+)-(.+)\\.html',(\\d+),(\\d+)\\);return false;\" id=\".+\" class=\"(Vol|Ch) eps_a d-block\" >\\s*(.+)</a>.*")
            var m = p.matcher(s)
            if (m.matches()) {
                // https://articles.onemoreplace.tw/online/new-13313.html?ch=1
                var baseurl = "https://articles.onemoreplace.tw"
                baseurl += "/online/new-"
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

            //<meta name="name" content="關于我轉生後成為史萊姆的那件事" />
            //p = Pattern.compile(".*<h6 class=\"title\">(.+?)</h6>.*")
            p = Pattern.compile(".*<meta name=\"name\" content=\"(.+?)\" />.*")
            m = p.matcher(s)
            if (m.matches()) {
                val bookTitle = m.group(1).replace("&nbsp;", " ").replace("<.*?>".toRegex(), "")
                //Log.e("jComics", "bookTitle = $bookTitle")
                book.bookTitle = bookTitle
            }

            //<li class="item_info_detail">
            //　　普通上班族三上悟(37,童貞)遇刺身亡.迎接他的,是轉生後的異世界史萊姆生活..
            //<span class="gradient"></span>
            //p = Pattern.compile(".*<div class=\"full_text\" style=\".+?\">(.+?)</div>.*")
            p = Pattern.compile(".*<li class=\"item_info_detail\">(.*?)<span class=\"gradient\"></span>")
            m = p.matcher(s)
            if (m.matches()) {
                val bookSynopsis = m.group(1).replace("&nbsp;", " ").replace("<.*?>".toRegex(), "")
                //Log.e("jComics", "bookSynopsis = $bookSynopsis")
                book.bookSynopsis =  bookSynopsis
            }

            //<div class="d-none d-md-block col-md-2 p-0 item-cover">
            //<img src="/pics/0/13313.jpg">
            //</div>
            //p = Pattern.compile(".*<li class=\"cover\">.*<img src='(.+)'>.*")
            p = Pattern.compile(".*<div class=\"d-none d-md-block col-md-2 p-0 item-cover\"><img src=\"(.+)\"></div>.*")
            m = p.matcher(s)
            if (m.matches()) {
                book.bookImgUrl = "https://www.8comic.com" + m.group(1)
            }

        }
        book.episodes = episodes
        return true
    }
}

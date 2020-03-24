package com.jsoft.jcomic.praser

import android.util.Log
import com.jsoft.jcomic.helper.BookDTO
import com.jsoft.jcomic.helper.EpisodeDTO
import taobe.tec.jcc.JChineseConvertor
import java.util.ArrayList
import java.util.regex.Pattern

class QiMiaoBookParser(book: BookDTO, listener: BookParserListener) : BookParser(book, listener, "UTF-8") {
    private fun chineseS2T(simplifiedChineseString: String): String {
        var result = simplifiedChineseString
        try {
            val jChineseConvertor = JChineseConvertor.getInstance()
            result = jChineseConvertor.s2t(simplifiedChineseString)
        } catch (e: Exception) {
            Log.e("jComics", "Error caught in DM5BookParser", e)
        }

        return result
    }

    override fun getBookFromUrlResult(html: ArrayList<String>):Boolean {
        val episodes = ArrayList<EpisodeDTO>()
        //Log.e("jComics", "${book.bookUrl}")
        for (i in 3 until html.size) {
            var s = html[i - 3].trim { it <= ' '} + html[i - 2].trim { it <= ' '} + html[i - 1].trim { it <= ' ' } + html[i].trim { it <= ' ' }
            s = s.replace("\n", "")

            var p = Pattern.compile(".*<a href=\\\"\\/manhua\\/(\\d+)\\/(\\d+)\\.html\\\">\\s*<img class=\\\"lazyload\\\".*<div class=\\\"ncp3li_div ncp3li_title\\\">(.+)<\\/div>.*")
            var m = p.matcher(s)
            if (m.matches()) {
                val episodeUrl = "https://m.qimiaomh.com/Action/Play/AjaxLoadImgUrl?did=" + m.group(1) + "&sid=" +  m.group(2) + "&tmp="
                val episodeTitle = chineseS2T(m.group(3))
                //Log.d("jComics", episodeTitle + " " + episodeUrl)
                var found = false
                for (e in episodes) {
                    if (e.episodeUrl.trim() == episodeUrl) {
                        found = true
                        break
                    }
                }
                if (!found) {
                    episodes.add(EpisodeDTO(episodeTitle, episodeUrl))
                    //Log.e("jComics", "$episodeTitle - $episodeUrl")
                }

            }

            p = Pattern.compile(".*<div class=\\\"ncp1_bac\\\">\\s*<div class=\\\"ncp1b_div ncp1b_tit\\\"><h1>(.*)<\\/h1>.*")
            m = p.matcher(s)
            if (m.matches()) {
                book.bookTitle = chineseS2T(m.group(1).replace("&nbsp;", " ").replace("<.*?>".toRegex(), ""))
            }

            p = Pattern.compile(".*<div class=\\\"nmain_com_p nmain_com_p2\\\">\\s*<div class=\\\"title\\\"><h2>.*<\\/h2><\\/div>\\s*<p>(.*)<\\/p>.*")
            m = p.matcher(s)
            if (m.matches()) {
                book.bookSynopsis = chineseS2T(m.group(1).replace("&nbsp;", " ").replace("<.*?>".toRegex(), ""))
            }

            p = Pattern.compile(".*<div class=\\\"nmain_com_p1\\\">\\s*<img class=\\\"lazyload\\\" data-src=\\\"(.+)\\\" src=.*")
            m = p.matcher(s)
            if (m.matches()) {
                book.bookImgUrl = m.group(1)
            }

        }
        book.episodes = episodes
        return true
    }
}
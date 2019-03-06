package com.jsoft.jcomic.praser

import android.text.Html
import android.util.Log
import com.jsoft.jcomic.helper.BookDTO
import com.jsoft.jcomic.helper.EpisodeDTO
import taobe.tec.jcc.JChineseConvertor
import java.util.*
import java.util.regex.Pattern


class DM5BookParser(book: BookDTO, listener: BookParserListener) : BookParser(book, listener, "UTF-8") {

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

    //Call when URL is fetched
    override fun getBookFromUrlResult(html: ArrayList<String>) {
        var episodes: MutableList<EpisodeDTO> = ArrayList()
        var s = ""
        for (i in html.indices) {
            s += html[i]
        }

        var p = Pattern.compile("<a href=\"([A-Za-z0-9\\/]+?)\" title=\"(.*?)\" class=\"chapteritem\">(.+?)</a>")
        var m = p.matcher(s)
        while (m.find()) {
            val episodeUrl = "http://m.dm5.com" + m.group(1)
            //Log.d("jComics", episodeUrl);
            episodes.add(EpisodeDTO(chineseS2T(m.group(3).trim { it <= ' ' } + " " + m.group(2).trim { it <= ' ' }), episodeUrl))
        }

        p = Pattern.compile("<a href=\"([a-zA-Z0-9/]*?)\" class=\"chapteritem\">.*?<p class=\"detail-list-2-info-title\">(.*?)</p>.+?</a>")
        m = p.matcher(s)
        while (m.find()) {
            val episodeUrl = "http://m.dm5.com" + m.group(1)
            episodes.add(0, EpisodeDTO(chineseS2T(m.group(2)), episodeUrl))
        }

        p = Pattern.compile(".*<span class=\"normal-top-title\">(.+?)</span>.*")
        m = p.matcher(s)
        if (m.matches()) {
            book.bookTitle = chineseS2T(m.group(1).replace("&nbsp;", " ").replace("<.*?>".toRegex(), ""))
        }

        p = Pattern.compile(".*<p class=\"detail-desc\" id=\"detail-desc\".*?>(.*?)</p>.*")
        m = p.matcher(s)
        if (m.matches()) {
            book.bookSynopsis = chineseS2T(Html.fromHtml(m.group(1)).toString())
        }

        if (book.bookSynopsis == null) {
            p = Pattern.compile(".*<p class=\"detail-desc\".*?>(.*?)</p>.*")
            m = p.matcher(s)
            if (m.matches()) {
                val synopsis = Html.fromHtml(m.group(1)).toString()
                book.bookSynopsis = chineseS2T(synopsis)
            }
        }

        p = Pattern.compile(".*<div class=\"detail-main-cover\"><img src=\"(.+?)\"></div>.*")
        m = p.matcher(s)
        if (m.matches()) {
            book.bookImgUrl = m.group(1)
        }

        val cleanEpisodes = ArrayList<EpisodeDTO>()
        for (episode in episodes) {
            var found = false
            for (cleanEpisode in cleanEpisodes) {
                if (cleanEpisode.episodeUrl.trim { it <= ' ' } == episode.episodeUrl.trim { it <= ' ' }) {
                    found = true
                    break
                }
            }
            if (!found) {
                cleanEpisodes.add(episode)
            }
        }
        episodes = cleanEpisodes
        for (episode in episodes) {
            episode.bookTitle = book.bookTitle
        }
        book.episodes = episodes
    }
}

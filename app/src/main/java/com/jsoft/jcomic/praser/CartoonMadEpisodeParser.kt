package com.jsoft.jcomic.praser

import com.jsoft.jcomic.helper.EpisodeDTO
import com.jsoft.jcomic.helper.Utils
import java.net.URL
import java.util.*
import java.util.regex.Pattern

class CartoonMadEpisodeParser(episode: EpisodeDTO, listener: EpisodeParserListener) : EpisodeParser(episode, listener, "BIG5") {

    override fun getURLResponse(url: URL, encoding: String): ArrayList<String> {
        val path = if (url.path.startsWith("/m")) url.path.substring(2) else url.path
        return Utils.getURLResponse(URL("http://" + url.host + path), null, encoding)
    }

    override fun getEpisodeFromUrlResult(result: List<String>) {
        episode.imageUrl = ArrayList()
        var imageUrlHost: String? = ""
        var imageUrlPath = ""
        var imageUrlQuery: String? = ""
        var numPage = 0

        for (s in result) {
            var p = Pattern.compile(".*<img src=\"(https?:\\/\\/[a-zA-Z0-9:\\.]+)?(.+)(\\d\\d\\d)(&.+=.+)?\" border=\"\\d+\" oncontextmenu=.+?>.*")
            //<img src="comicpic.asp?file=/1152/934/001" border="0" oncontextmenu='return false' onload=


            var m = p.matcher(s)
            if (m.matches()) {
                imageUrlHost = m.group(1)
                imageUrlPath = m.group(2)
                imageUrlQuery = m.group(4)
            }

            p = Pattern.compile(".*...<a class=pages href=(.+)>(\\d+)</a>.*")
            m = p.matcher(s)
            if (m.matches()) {
                numPage = Integer.parseInt(m.group(2))
            }

        }
        episode.pageCount = numPage

        val imageUrlList = ArrayList<String>()
        if (imageUrlHost == null) {
            imageUrlHost = "http://www.cartoonmad.com/comic/"
        }
        if (imageUrlQuery == null) {
            imageUrlQuery = ""
        }
        for (k in 1..episode.pageCount) {
            val imageUrl = imageUrlHost + imageUrlPath + String.format("%03d", k) + imageUrlQuery
            imageUrlList.add(imageUrl)
        }
        episode.imageUrl = imageUrlList

    }
}

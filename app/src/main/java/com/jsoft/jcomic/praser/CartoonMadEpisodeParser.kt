package com.jsoft.jcomic.praser

import com.jsoft.jcomic.helper.EpisodeDTO
import java.util.*
import java.util.regex.Pattern

class CartoonMadEpisodeParser(episode: EpisodeDTO, listener: EpisodeParserListener) : EpisodeParser(episode, listener, "BIG5") {
    override fun getEpisodeFromUrlResult(result: List<String>) {
        episode.imageUrl = ArrayList()
        var imageUrlHost: String? = ""
        var imageUrlPath = ""
        var imageUrlQuery: String? = ""
        var numPage = 0
        for (s in result) {
            var p = Pattern.compile(".*<img src=\"(https?:\\/\\/[a-zA-Z0-9:\\.]+)?(.+)(\\d\\d\\d)(&.+=.+)?\" border=\"0\" oncontextmenu='return false' width=\"\\d+\">.*")
            //<img src="comicpic.asp?file=/1152/932/002" border="0" oncontextmenu='return false' width="700">
            //<img src="comicpic.asp?file=/1558/001/001&rimg=1" border="0" oncontextmenu='return false' width="700"></a>

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
            imageUrlHost = "https://www.cartoonmad.com/m/comic/"
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

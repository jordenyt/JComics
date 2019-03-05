package com.jsoft.jcomic.praser

import com.jsoft.jcomic.helper.EpisodeDTO

import java.util.ArrayList
import java.util.regex.Matcher
import java.util.regex.Pattern

class CartoonMadEpisodeParser(episode: EpisodeDTO, listener: EpisodeParserListener) : EpisodeParser(episode, listener, "BIG5") {
    override fun getEpisodeFromUrlResult(result: List<String>) {
        episode.imageUrl = ArrayList()
        var imageUrl_0: String? = ""
        var imageUrl_1 = ""
        var imageUrl_3: String? = ""
        var numPage = 0
        for (s in result) {
            var p = Pattern.compile(".*<img src=\"(https?:\\/\\/[a-zA-Z0-9:\\.]+)?(.+)(\\d\\d\\d)(&.+=.+)?\" border=\"0\" oncontextmenu='return false' width=\"\\d+\">.*")
            //<img src="comicpic.asp?file=/1152/932/002" border="0" oncontextmenu='return false' width="700">
            //<img src="comicpic.asp?file=/1558/001/001&rimg=1" border="0" oncontextmenu='return false' width="700"></a>

            var m = p.matcher(s)
            if (m.matches()) {
                imageUrl_0 = m.group(1)
                imageUrl_1 = m.group(2)
                imageUrl_3 = m.group(4)
            }

            p = Pattern.compile(".*...<a class=pages href=(.+)>(\\d+)</a>.*")
            m = p.matcher(s)
            if (m.matches()) {
                numPage = Integer.parseInt(m.group(2))
            }

        }
        episode.pageCount = numPage

        val imageUrlList = ArrayList<String>()
        if (imageUrl_0 == null) {
            imageUrl_0 = "https://www.cartoonmad.com/m/comic/"
        }
        if (imageUrl_3 == null) {
            imageUrl_3 = ""
        }
        for (k in 1..episode.pageCount) {
            val imageUrl = imageUrl_0 + imageUrl_1 + String.format("%03d", k) + imageUrl_3
            imageUrlList.add(imageUrl)
        }
        episode.imageUrl = imageUrlList
    }
}

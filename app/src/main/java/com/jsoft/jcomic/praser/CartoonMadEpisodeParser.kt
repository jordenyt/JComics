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

        //http://www.cartoonmad.com/m/comic/102900123095001.html
        var p = Pattern.compile(".*(\\d{4})\\d(\\d{3})\\d(\\d{3})(\\d{3}).*")
        var m = p.matcher(episode.episodeUrl)
        var bookId = ""
        var episodeId = ""
        if (m.matches()) {
            episode.pageCount = Integer.parseInt(m.group(3))
            bookId = m.group(1)
            episodeId = m.group(2)
        }
        //http://www.cartoonmad.com/m/comic/comicpic.asp?file=/1029/032/004&rimg=1
        val imageUrlList = ArrayList<String>()
        for (k in 1..episode.pageCount) {
            val imageUrl = "http://www.cartoonmad.com/comic/comicpic.asp?file=/" + bookId + "/" + episodeId + "/" + String.format("%03d", k) + "&rimg=1"
            //Log.d("jComics", imageUrl)
            imageUrlList.add(imageUrl)
        }
        episode.imageUrl = imageUrlList

    }
}

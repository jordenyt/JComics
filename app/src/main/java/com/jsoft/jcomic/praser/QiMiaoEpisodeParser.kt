package com.jsoft.jcomic.praser

import com.google.gson.Gson
import com.jsoft.jcomic.helper.EpisodeDTO
import com.jsoft.jcomic.helper.QiMiaoEpisodeDTO
import com.jsoft.jcomic.helper.Utils
import java.net.URL
import java.util.ArrayList
import kotlin.random.Random

class QiMiaoEpisodeParser(episode: EpisodeDTO, listener: EpisodeParserListener) : EpisodeParser(episode, listener, "BIG5") {

    override fun getURLResponse(url: URL, encoding: String): ArrayList<String> {
        val path = episode.episodeUrl + Random.nextFloat()
        //Log.d("jComics",path)
        return Utils.getURLResponse(URL(path), "https://m.qimiaomh.com/", encoding)
    }

    override fun getEpisodeFromUrlResult(result: List<String>) {

        episode.imageUrl = ArrayList()
        val s = result.joinToString()
        //Log.d("jComics",s)
        val gson = Gson()
        val obj = gson.fromJson(s,QiMiaoEpisodeDTO::class.java)
        episode.imageUrl = obj.listImg
        episode.pageCount = obj.listImg.size

    }
}
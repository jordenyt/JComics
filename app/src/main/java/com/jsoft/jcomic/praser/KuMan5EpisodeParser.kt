package com.jsoft.jcomic.praser

import android.util.Base64
import com.google.gson.Gson
import com.jsoft.jcomic.helper.EpisodeDTO
import java.io.UnsupportedEncodingException
import java.util.*
import java.util.regex.Pattern

class KuMan5EpisodeParser(episode: EpisodeDTO, listener: EpisodeParserListener) : EpisodeParser(episode, listener, "UTF-8") {
    override fun getEpisodeFromUrlResult(result: List<String>) {
        episode.pageCount = 0
        episode.imageUrl = ArrayList<String>()
        for (i in result.indices) {
            var s = result[i].trim { it <= ' ' }

            var p = Pattern.compile(".*var km5_img_url='(.*?)'.*")
            var m = p.matcher(s)
            if (m.matches()) {
                //Log.d("jComics", base64Decode(m.group(1)))
                val gson = Gson()
                val pageList: List<String> = gson.fromJson(base64Decode(m.group(1)), Array<String>::class.java).toList()
                for(page in pageList) {
                    episode.imageUrl.add(page.replace("\r","").replace("\\d+\\|".toRegex(), ""))
                    episode.pageCount ++
                }
            }
        }
    }

    private fun base64Decode(encoded: String): String? {
        val bytes: ByteArray = Base64.decode(encoded, Base64.DEFAULT)
        return try {
            String(bytes)
        } catch (e: UnsupportedEncodingException) {
            throw IllegalStateException(e)
        }
    }

}
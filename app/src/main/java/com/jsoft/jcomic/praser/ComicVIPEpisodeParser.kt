package com.jsoft.jcomic.praser

//import android.util.Log
import com.jsoft.jcomic.helper.EpisodeDTO
import java.util.*
import java.util.regex.Pattern

/**
 * Created by 01333855 on 05/10/2015.
 */
class ComicVIPEpisodeParser(episode: EpisodeDTO, listener: EpisodeParserListener) : EpisodeParser(episode, listener, "BIG5") {

    override fun getEpisodeFromUrlResult(result: List<String>) {
        episode.imageUrl = ArrayList()
        var episodeId = 0
        var bookId = 0
        var code = ""
        //Log.e("jComics", "${episode.episodeUrl}")
        if (episode.episodeUrl.contains("8.twobili.com")) {
            val p = Pattern.compile(".*8\\.twobili\\.com/comic/[a-z]+_(\\d+)\\.html\\?ch=(\\d+)")
            val m = p.matcher(episode.episodeUrl)
            if (m.matches()) {
                bookId = Integer.parseInt(m.group(1))
                episodeId = Integer.parseInt(m.group(2))
            }
        }
        //Log.e("jComics", "$bookId, $episodeId")
        for (s in result) {
            val p = Pattern.compile(".*var chs=(\\d+);var ti=(\\d+);var cs='([a-z0-9]+)';eval.*")
            val m = p.matcher(s)
            if (m.matches()) {
                code = m.group(3)
            }
        }
        //Log.e("jComics", code)
        var extractCode = ""
        if (code.isNotEmpty() && episodeId > 0 && bookId > 0) {
            for (i in 0 until code.length / 50) { // i = 0 to 399
                if (Integer.parseInt(ss(code, i * 50, 4, true)) == episodeId) {
                    extractCode = ss(code, i * 50, 50, false)
                    break
                }
            }
            val numPage = Integer.parseInt(ss(extractCode, 7, 3, true))
            episode.pageCount = numPage
            for (i in 1..numPage) {
                //'//img' + ss(c, 4, 2) + '.8comic.com/' + ss(c, 6, 1) + '/' + ti + '/' + ss(c, 0, 4) + '/' + nn(p) + '_' + ss(c, mm(p) + 10, 3, f) + '.jpg';
                val pageUrl = ("http://img" + ss(extractCode, 4, 2, true) + ".8comic.com/"
                        + ss(extractCode, 6, 1, true) + "/" + bookId + "/" + ss(extractCode, 0, 4, true) + "/"
                        + String.format("%03d", i) + '_'.toString() + ss(extractCode, mm(i) + 10, 3, false) + ".jpg")
                episode.imageUrl.add(pageUrl)
            }
        }
    }

    private fun ss(code: String, position: Int, size: Int, integerOnly: Boolean): String {
        val subCode = code.substring(position, position + size)
        return if (integerOnly) subCode.replace("[a-z]*".toRegex(), "") else subCode
    }

    private fun mm(p: Int): Int {
        return Math.floor(((p - 1) / 10).toDouble()).toInt() % 10 + (p - 1) % 10 * 3
    }
}

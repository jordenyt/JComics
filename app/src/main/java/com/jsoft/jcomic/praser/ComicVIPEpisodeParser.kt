package com.jsoft.jcomic.praser

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

        if (episode.episodeUrl.contains("www.comicbus.com")) {
            val p = Pattern.compile("https://www\\.comicbus\\.com/show/([a-z-]+)(\\d+)\\.html\\?ch=(\\d+)")
            val m = p.matcher(episode.episodeUrl)
            if (m.matches()) {
                bookId = Integer.parseInt(m.group(2))
                episodeId = Integer.parseInt(m.group(3))
            }
        } else if (episode.episodeUrl.contains("m.comicbus.com")) {
            val p = Pattern.compile("https://m\\.comicbus\\.com/comic/[a-z]+_(\\d+)\\.html\\?ch=(\\d+)")
            val m = p.matcher(episode.episodeUrl)
            if (m.matches()) {
                bookId = Integer.parseInt(m.group(1))
                episodeId = Integer.parseInt(m.group(2))
            }
        }

        for (s in result) {
            val p = Pattern.compile(".*<script>var chs=(\\d+);var ti=(\\d+);var cs='([a-z0-9]+)';eval.*")
            val m = p.matcher(s)
            if (m.matches()) {
                code = m.group(3)
            }
        }

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
                val pageUrl = ("https://img" + ss(extractCode, 4, 2, true) + ".8comic.com/"
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

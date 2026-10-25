package com.jsoft.jcomic.praser

import android.util.Log
import com.jsoft.jcomic.helper.EpisodeDTO
import com.jsoft.jcomic.helper.KalamSite
import java.net.URL

/**
 * Parser for a chapter of kalamanhua.com (卡拉漫画).
 *
 * The page of the chapter has the key of the reader, the reader is a page of the image host that
 * has the pictures of the chapter. The image host uses the same access check as the site, the
 * token of the check is needed for the pictures as well.
 */
class KalamEpisodeParser(
    episode: EpisodeDTO,
    listener: EpisodeParserListener
) : EpisodeParser(episode, listener, "UTF-8") {

    override fun getURLResponse(url: URL, encoding: String): ArrayList<String> {
        val ids = KalamSite.chapterIds(episode.episodeUrl)
        if (ids == null) {
            Log.e("jComics", "kalamanhua: cannot read the chapter id from " + episode.episodeUrl)
            return arrayListOf()
        }
        Log.d("jComics", "kalamanhua: reading chapter " + ids.second + " of comic " + ids.first)

        val page = KalamSite.readPage(episode.episodeUrl)
        if (KalamSite.isCheckPage(page)) {
            Log.e("jComics", "kalamanhua: the access check of the site was not passed")
            return arrayListOf()
        }
        val key = readerKey(page)
        if (key == null) {
            Log.e("jComics", "kalamanhua: the reader of " + episode.episodeUrl + " was not found")
            return arrayListOf()
        }

        val playUrl = KalamSite.IMAGE_HOST + "/play/min/" + key
        var reader = KalamSite.readImagePage(playUrl)
        if (!reader.contains("<img")) {
            // The image host wants to check the visitor first, the token of the check is written
            // into the page and is needed for the pictures as well.
            val token = KalamSite.tokenOf(reader)
            if (token == null) {
                Log.e("jComics", "kalamanhua: the reader of " + episode.episodeUrl + " was not read")
                return arrayListOf()
            }
            KalamSite.setImageToken(token)
            reader = KalamSite.readImagePage(playUrl + "?cf_verify=" + token, "omo_token=" + token)
        }

        val urls = ArrayList<String>()
        val pictures = if (IMAGE.containsMatchIn(reader)) IMAGE else IMAGE_FALLBACK
        for (match in pictures.findAll(reader)) {
            val src = match.groupValues[1]
            if (src.isEmpty()) {
                continue
            }
            urls.add(if (src.startsWith("http")) src else KalamSite.IMAGE_HOST + src)
        }
        if (urls.isEmpty()) {
            Log.e("jComics", "kalamanhua: no image found for " + episode.episodeUrl)
            return arrayListOf()
        }
        // The pictures are only served to a request that has the token of the reader.
        val token = KalamSite.tokenOfUrl(urls[0])
        if (token != null) {
            KalamSite.setImageToken(token)
        }
        return arrayListOf(urls.joinToString("\n"))
    }

    override fun getEpisodeFromUrlResult(result: List<String>) {
        val urls = ArrayList<String>()
        val text = result.firstOrNull() ?: ""
        for (line in text.split("\n")) {
            if (line.isNotEmpty()) {
                urls.add(line)
            }
        }
        if (urls.isEmpty()) {
            return
        }
        episode.imageUrl = urls
        episode.pageCount = urls.size
        Log.d("jComics", "kalamanhua: " + episode.episodeTitle + " has " + urls.size + " pages")
    }

    /**
     * The key of the reader is written into the player of the page of the chapter.
     */
    private fun readerKey(page: String): String? {
        val start = page.indexOf(PLAYER)
        if (start < 0) {
            return null
        }
        val end = page.indexOf("</script>", start)
        val block = if (end < 0) page.substring(start) else page.substring(start, end)
        val match = KEY.find(block) ?: return null
        return match.groupValues[1]
    }

    companion object {
        private const val PLAYER = "player_aaaa="
        private val KEY = Regex("\"url\":\"([^\"]+)\"")

        // The pictures of a chapter are relative to the image host, they are written into the
        // "data-src" attribute of the pictures, the "src" attribute holds a placeholder.
        private val IMAGE = Regex("data-src=\"(/image/[^\"]+)\"")
        private val IMAGE_FALLBACK = Regex("\\ssrc=\"(/image/[^\"]+)\"")
    }
}

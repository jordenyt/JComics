package com.jsoft.jcomic.helper

/**
 * The urls of manga.bilibili.com (哔哩哔哩漫画). The site uses "/detail/mc<comicId>" for the page of
 * a comic and "/mc<comicId>/<episodeId>" for the reader of a chapter.
 */
internal object BilibiliManga {

    const val DETAIL_URL = "https://manga.bilibili.com/detail/mc"
    const val READER_URL = "https://manga.bilibili.com/mc"

    private val COMIC_ID = Regex("""/mc(\d+)""")
    private val EPISODE_ID = Regex("""/mc(\d+)/(\d+)""")

    /**
     * The id of the comic inside the site.
     */
    fun comicId(url: String?): String? {
        val match = COMIC_ID.find(url ?: "") ?: return null
        return match.groupValues[1]
    }

    /**
     * The id of the comic and the id of the chapter, taken from the url of the reader.
     */
    fun episodeIds(url: String?): Pair<String, String>? {
        val match = EPISODE_ID.find(url ?: "") ?: return null
        return Pair(match.groupValues[1], match.groupValues[2])
    }

    /**
     * The page of the comic that the url belongs to, the reader of a chapter is turned into the
     * page of the comic.
     */
    fun comicUrl(url: String): String {
        val id = comicId(url) ?: return url
        return DETAIL_URL + id
    }
}

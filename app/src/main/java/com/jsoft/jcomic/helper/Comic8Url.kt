package com.jsoft.jcomic.helper

/**
 * The 8comic site (無限動漫) is served by several hosts. The site links to "www.8comic.com" but the
 * tls connection to that host fails on many networks - the handshake is answered with something that
 * is not a tls response - so the page cannot be read at all. "v.8comic.com" serves the same pages and
 * can be reached with http and with https, therefore every url of the site is rewritten to that host.
 */
internal object Comic8Url {

    private val SITE_URL = Regex("""^(?:https?)://(?:www\.|v\.)?8comic\.com(/.*)?$""", RegexOption.IGNORE_CASE)

    private val BOOK_ID = Regex("""8comic\.com/html/(\d+)\.html""")

    const val PRIMARY_HOST = "https://v.8comic.com"

    /**
     * Rewrites a url of the site to the host that can actually be reached, other urls are returned
     * unchanged.
     */
    fun normalize(url: String?): String? {
        if (url.isNullOrEmpty()) return url
        val match = SITE_URL.find(url) ?: return url
        val path = match.groupValues[1].ifEmpty { "/" }
        return PRIMARY_HOST + path
    }

    /**
     * The id of the comic inside the site, taken from the url of the book page.
     */
    fun bookId(url: String): String? {
        val match = BOOK_ID.find(url) ?: return null
        return match.groupValues[1]
    }
}

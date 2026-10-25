package com.jsoft.jcomic.helper

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Helper for the site kalamanhua.com (卡拉漫画).
 *
 * The site puts an access check in front of its pages: the check page contains a javascript that
 * writes a cookie ("omo_token") with a token, the pages themselves are only served to a request
 * that sends this cookie. The pictures live on a second host that uses the same check - it only
 * serves them to a request that has the cookie of that host and a referer of the site.
 *
 * The chapter list and the pictures of a chapter are not part of the page of the book: the book
 * page has the list of the chapters, the page of a chapter has the key of the reader, the reader
 * itself is a page of the image host that has the pictures of the chapter.
 */
internal object KalamSite {

    const val HOST = "https://www.kalamanhua.com"
    const val IMAGE_HOST = "https://image.kalaimg.top"
    const val REFERER = "$HOST/"

    // The token is written by the check page, this one is used when the check page cannot be read.
    private const val FALLBACK_TOKEN = "07cc8d8443a9bee3"

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Mobile Safari/537.36"

    private const val CONNECT_TIMEOUT = 15000
    private const val READ_TIMEOUT = 20000

    // The check page of the site writes the token into a variable named "k", the check page of the
    // image host writes it into a variable named "token".
    private val TOKEN_COOKIE = Regex("omo_token=([a-z0-9]+)")
    private val TOKEN_VARIABLE = Regex("var token\\s*=\\s*'([^']+)'")
    private val TOKEN_KEY = Regex("[\\s,;]k\\s*=\\s*\"([a-z0-9]{8,})\"")
    private val CF_VERIFY = Regex("cf_verify=([a-z0-9_]+)")

    private val COMIC_ID = Regex("""/comic/(\d+)\.html""")
    private val CHAPTER_ID = Regex("""/chapter/(\d+)-(\d+)-(\d+)\.html""")

    @Volatile
    private var siteToken: String? = null

    @Volatile
    private var imageToken: String? = null

    /**
     * The id of the comic inside the site, taken from the url of the book page.
     */
    fun comicId(url: String?): String? {
        val match = COMIC_ID.find(url ?: "") ?: return null
        return match.groupValues[1]
    }

    /**
     * The id of the comic and the number of the chapter, taken from the url of a chapter.
     */
    fun chapterIds(url: String?): Pair<String, String>? {
        val match = CHAPTER_ID.find(url ?: "") ?: return null
        return Pair(match.groupValues[1], match.groupValues[3])
    }

    fun bookUrl(comicId: String): String {
        return "$HOST/comic/$comicId.html"
    }

    /**
     * The chapters of the comic are served by three lines, the first line has one chapter per
     * episode, the other lines group them into volumes.
     */
    fun chapterUrl(comicId: String, number: String): String {
        return "$HOST/chapter/$comicId-1-$number.html"
    }

    fun isImageOfSite(url: String?): Boolean {
        return url != null && url.contains("kalaimg.top")
    }

    /**
     * The token of the check page of the site.
     */
    fun tokenOf(html: String): String? {
        val match = TOKEN_COOKIE.find(html)
                ?: TOKEN_VARIABLE.find(html)
                ?: TOKEN_KEY.find(html)
        return match?.groupValues?.get(1)
    }

    /**
     * The token that is written into the url of a picture.
     */
    fun tokenOfUrl(url: String): String? {
        val match = CF_VERIFY.find(url) ?: return null
        return match.groupValues[1]
    }

    /**
     * The cookie of the site, it is needed for every page of the site.
     */
    fun siteCookie(): String {
        return "omo_token=" + siteTokenValue()
    }

    /**
     * The cookie of the image host, it is needed for the pictures of a chapter. The token is only
     * known after the reader of a chapter was read, the covers of the books do not need it.
     */
    fun imageCookie(): String? {
        val token = imageToken ?: return null
        return "omo_token=" + token
    }

    fun setImageToken(token: String) {
        imageToken = token
    }

    /**
     * The cookie that is needed to read the url.
     */
    fun cookieFor(url: String?): String? {
        if (url == null) return null
        if (isImageOfSite(url)) return imageCookie()
        if (url.contains("kalamanhua.com")) return siteCookie()
        return null
    }

    /**
     * The referer that is needed to read the url, the image host only serves the pictures to a
     * request that comes from the site.
     */
    fun refererFor(url: String?): String? {
        if (isImageOfSite(url)) return REFERER
        return null
    }

    /**
     * Reads a page of the site, the access check is solved when it is needed.
     */
    fun readPage(url: String): String {
        var body = fetch(url, null, siteCookie()).second
        if (isCheckPage(body)) {
            // The token of the site is not valid any more, get a new one.
            siteToken = null
            body = fetch(url, null, siteCookie()).second
        }
        return body
    }

    /**
     * Reads a page of the image host, the referer of the site is needed to get the pictures.
     */
    fun readImagePage(url: String, cookie: String? = null): String {
        return fetch(url, REFERER, cookie).second
    }

    /**
     * The check page of the site, it is shown when the cookie of the site is not accepted.
     */
    fun isCheckPage(body: String): Boolean {
        return body.contains("访问验证") || body.contains("安全验证")
    }

    private fun siteTokenValue(): String {
        siteToken?.let { return it }
        synchronized(this) {
            siteToken?.let { return it }
            val body = fetch("$HOST/", null, null).second
            val token = tokenOf(body) ?: FALLBACK_TOKEN
            siteToken = token
            return token
        }
    }

    /**
     * Reads a page, the body of a check page is returned as well - it holds the token that is
     * needed to pass the check.
     */
    private fun fetch(url: String, referer: String?, cookie: String?): Pair<Int, String> {
        var status = 0
        var body = ""
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = CONNECT_TIMEOUT
            conn.readTimeout = READ_TIMEOUT
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            if (referer != null) {
                conn.setRequestProperty("Referer", referer)
            }
            if (cookie != null) {
                conn.setRequestProperty("Cookie", cookie)
            }
            conn.instanceFollowRedirects = true
            // The check of the site must not be answered from the cache.
            conn.useCaches = false

            status = conn.responseCode
            val stream = if (status >= 400) conn.errorStream else conn.inputStream
            if (stream != null) {
                val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
                val builder = StringBuilder()
                var line = reader.readLine()
                while (line != null) {
                    builder.append(line).append('\n')
                    line = reader.readLine()
                }
                reader.close()
                body = builder.toString()
            }
            conn.disconnect()
        } catch (e: Exception) {
            Log.e("jComics", "Exception caught in KalamSite.fetch", e)
        }
        return Pair(status, body)
    }
}

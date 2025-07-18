package com.jsoft.jcomic.praser

//import android.util.Log
import com.jsoft.jcomic.helper.EpisodeDTO
import java.net.URLDecoder
import java.util.*
import java.util.regex.Pattern

/**
 * Created by 01333855 on 05/10/2015.
 */
class Comic8EpisodeParser(episode: EpisodeDTO, listener: EpisodeParserListener) : EpisodeParser(episode, listener, "BIG5") {

    override fun getEpisodeFromUrlResult(result: List<String>) {
        episode.imageUrl = ArrayList()
        var episodeId = 0
        var bookId = 0
        var code = ""
        //Log.e("jComics", "${episode.episodeUrl}")
        //https://articles.onemoreplace.tw/online/new-13313.html?ch=130
        if (episode.episodeUrl.contains("articles.onemoreplace.tw")) {
            val p = Pattern.compile(".*online/new-(\\d+)\\.html\\?ch=(\\d+)")
            val m = p.matcher(episode.episodeUrl)
            if (m.matches()) {
                bookId = Integer.parseInt(m.group(1))
                episodeId = Integer.parseInt(m.group(2))
            }
        }
        //Log.e("jComics", "$bookId, $episodeId")
        for (s in result) {
            //val p = Pattern.compile(".*var chs=(\\d+);var ti=(\\d+);var cs='([a-z0-9]+)';eval.*")
            if (s.contains("jxsk2ys763")) {
                for (t in s.split(';')) {
                    if (t.contains("var jxsk2ys763='")) {
                        //Log.e("jComics", t)
                        val p = Pattern.compile(".*jxsk2ys763='(.*?)'.*")
                        val m = p.matcher(s)
                        if (m.matches()) {
                            //Log.e("jComics", t)
                            code = m.group(1)
                        }
                    }
                }
            }
        }
        //Log.e("jComics", code)

        val decoded = getChapterImageUrls(bookId, code, episodeId.toString())
        episode.pageCount = decoded.second
        episode.imageUrl = decoded.first
        //Log.e("jComics", episode.pageCount.toString())
    }

    fun getChapterImageUrls(comicId: Int, encodedData: String, chapterId: String): Pair<ArrayList<String>, Int> {
        fun decodeLc(input: String): String {
            if (input.length != 2) return input
            val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
            val firstChar = input[0]
            val secondChar = input[1]
            return if (firstChar == 'Z') {
                (8000 + alphabet.indexOf(secondChar)).toString()
            } else {
                (alphabet.indexOf(firstChar) * 52 + alphabet.indexOf(secondChar)).toString()
            }
        }

        fun extractString(index: Int): String {
            val start = encodedData.length - 47 - index * 6
            val substring = encodedData.substring(start, start + 6)
            var decoded = ""
            var i = 0
            while (i < substring.length) {
                val hex = substring.substring(i, i + 2)
                decoded += URLDecoder.decode("%$hex", "UTF-8")
                i += 2
            }
            return decoded
        }

        fun extractSubstring(input: String, startIndex: Int, length: Int = 40): String {
            return input.substring(startIndex, startIndex + length)
        }

        fun calculateCodeOffset(page: Int): Int {
            return ((page - 1) / 10 % 10) + ((page - 1) % 10 * 3)
        }

        val imgPrefix = extractString(4)
        val comicName = extractString(3)
        val domainSuffix = extractString(2)
        val fileExtension = extractString(1)

        val chapterNum = chapterId.replace(Regex("[a-z]$"), "")
        val subChapter = chapterId.takeLast(1).takeIf { it.matches(Regex("[a-z]")) } ?: ""

        for (i in 0 until 131) {
            val start = i * 47
            val currentChapterNum = decodeLc(encodedData.substring(start, start + 2))
            val currentSubChapter = encodedData.substring(start + 46, start + 47)

            if (currentChapterNum == chapterNum && (subChapter.isEmpty() || subChapter == currentSubChapter)) {
                val pageCount = decodeLc(extractSubstring(encodedData, start + 2, 2)).toInt()
                val domainPath = decodeLc(extractSubstring(encodedData, start + 4, 2))
                val subdomain = extractSubstring(domainPath, 0, 1)
                val path = extractSubstring(domainPath, 1, 1)
                val pageCodes = encodedData.substring(start + 6, start + 46)

                val urls = ArrayList<String>()
                for (j in 1..pageCount) {
                    val pageNum = j.toString().padStart(3, '0')
                    val codeStart = calculateCodeOffset(j)
                    val code = pageCodes.substring(codeStart, codeStart + 3)
                    val url = "https://${imgPrefix}${subdomain}.8${comicName}${domainSuffix}${comicName}/${path}/$comicId/$chapterId/${pageNum}_$code.$fileExtension"
                    //Log.e("jComics", url)
                    urls.add(url)
                }
                return Pair(urls, pageCount)
            }
        }
        return Pair(ArrayList<String>(), 0)
    }
}

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
        var extractParam = ArrayList<String>()
        for (s in result) {
            val loopPattern = Pattern.compile(
                "for\\s*\\(var\\s+i=0;i<\\d+;i\\+\\+\\)"
            )
            val matcher = loopPattern.matcher(s)
            if (matcher.find()) {
                //Log.e("jComics", s)
                extractParam = extractValues(s)
            }
        }
        val codeVar = extractParam.get(1)
        var code = ""
        for (s in result) {
            //val p = Pattern.compile(".*var chs=(\\d+);var ti=(\\d+);var cs='([a-z0-9]+)';eval.*")
            if (s.contains(codeVar)) {
                for (t in s.split(';')) {
                    if (t.contains("var $codeVar='")) {
                        //Log.e("jComics", t)
                        val p = Pattern.compile(".*$codeVar='(.*?)'.*")
                        val m = p.matcher(t)
                        if (m.matches()) {
                            //Log.e("jComics", t)
                            code = m.group(1)
                        }
                    }
                }
            }
        }
        //Log.e("jComics", code)

        val decoded = getChapterImageUrls(bookId, code, episodeId.toString(), extractParam)
        episode.pageCount = decoded.second
        episode.imageUrl = decoded.first
        //Log.e("jComics", episode.pageCount.toString())
    }

    fun extractValues(code: String): ArrayList<String> {
        // Extract the loop upper limit from for (var i=0;i<NUMBER;i++)
        val loopLimitPattern = Regex("""for \(var i=0;i<(\d+);i\+\+\)""")
        val loopLimit = loopLimitPattern.find(code)?.groups?.get(1)?.value
            ?: throw Exception("Loop limit not found")

        // Extract function name and variable name from lc(funcName(varName, ...)
        val lcPattern = Regex("""lc\((\w+)\((\w+),""")
        val lcMatch = lcPattern.find(code) ?: throw Exception("lc pattern not found")
        val funcName = lcMatch.groups[1]?.value ?: throw Exception("funcName not found")
        val varName = lcMatch.groups[2]?.value ?: throw Exception("varName not found")

        // Extract all variable assignments and their offsets
        val varPattern = Regex("""var (\w+)=lc\(\w+\(\w+,i\*\([^)]*\)\+(\d+)(?:,\d+)?\)\);""")
        val varMatches = varPattern.findAll(code)
        val varToOffset = mutableMapOf<String, Int>()
        for (match in varMatches) {
            val someVar = match.groups[1]?.value ?: continue
            val offset = match.groups[2]?.value?.toInt() ?: continue
            varToOffset[someVar] = offset
        }

        // Find the variable assigned to ps
        val psPattern = Regex("""ps=(\w+);""")
        val psVar = psPattern.find(code)?.groups?.get(1)?.value
            ?: throw Exception("psVar not found")

        // Extract condition variables from if(someVar==ch &&(part==''||part==anotherVar))
        val ifPattern = Regex("""if\((\w+)==ch &&\(part==''\|\|part==(\w+)\)\)""")
        val ifMatch = ifPattern.find(code) ?: throw Exception("if condition not found")
        val conditionVar1 = ifMatch.groups[1]?.value ?: throw Exception("conditionVar1 not found")
        val conditionVar2 = ifMatch.groups[2]?.value ?: throw Exception("conditionVar2 not found")

        // Extract HTML variables using the function name
        val htmlVar1Pattern = Regex("""${funcName}\((\w+), 0, 1\)""")
        val htmlVar1 = htmlVar1Pattern.find(code)?.groups?.get(1)?.value
            ?: throw Exception("htmlVar1 not found")
        val htmlVar2Pattern = Regex("""${funcName}\((\w+),mm\(j\),3\)""")
        val htmlVar2 = htmlVar2Pattern.find(code)?.groups?.get(1)?.value
            ?: throw Exception("htmlVar2 not found")

        // Retrieve offsets for all extracted variables
        val offsetPs = varToOffset[psVar] ?: throw Exception("Offset for psVar not found")
        val offsetConditionVar1 = varToOffset[conditionVar1]
            ?: throw Exception("Offset for conditionVar1 not found")
        val offsetConditionVar2 = varToOffset[conditionVar2]
            ?: throw Exception("Offset for conditionVar2 not found")
        val offsetHtmlVar1 = varToOffset[htmlVar1]
            ?: throw Exception("Offset for htmlVar1 not found")
        val offsetHtmlVar2 = varToOffset[htmlVar2]
            ?: throw Exception("Offset for htmlVar2 not found")

        // Return the list in the required order as strings
        return arrayListOf(
            loopLimit,
            varName,
            offsetPs.toString(),
            offsetConditionVar1.toString(),
            offsetConditionVar2.toString(),
            offsetHtmlVar1.toString(),
            offsetHtmlVar2.toString()
        )
    }

    fun getChapterImageUrls(comicId: Int, encodedData: String, chapterId: String, extractParam: ArrayList<String>): Pair<ArrayList<String>, Int> {
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

        for (i in 0 until extractParam.get(0).toInt()) {
            val start = i * 47
            val currentChapterNum = decodeLc(extractSubstring(encodedData, start + extractParam.get(3).toInt(), 2))
            val currentSubChapter = extractSubstring(encodedData, start + extractParam.get(4).toInt(), 1)

            if (currentChapterNum == chapterNum && (subChapter.isEmpty() || subChapter == currentSubChapter)) {
                val pageCount = decodeLc(extractSubstring(encodedData, start + extractParam.get(2).toInt(), 2)).toInt()
                val domainPath = decodeLc(extractSubstring(encodedData, start + extractParam.get(5).toInt(), 2))
                val subdomain = extractSubstring(domainPath, 0, 1)
                val path = extractSubstring(domainPath, 1, 1)
                val pageCodes = extractSubstring(encodedData, start + extractParam.get(6).toInt(), 40)

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

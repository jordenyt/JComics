package com.jsoft.jcomic.praser

import android.util.Log
import com.jsoft.jcomic.helper.EpisodeDTO
import com.jsoft.jcomic.helper.Utils
import java.net.URL
import java.util.*

/**
 * Parser for 8comic (無限動漫) episodes.
 *
 * The episode page (https://v.8comic.com/online/new-<bookId>.html?ch=<chapter>) does not contain the
 * image urls in the html. Instead it contains a small obfuscated javascript block which computes them.
 * The obfuscation is regenerated for every book, so every variable name and every field offset inside
 * the data blob is different from book to book. This parser therefore extracts the *structure* of that
 * script instead of relying on hard coded names/offsets:
 *
 *   the page contains:  for (var i=0;i<limit;i++) { var a=lc(fn(data, i*stride+off, len)); ... }
 *
 *   - limit        : number of chapters stored in the data blob
 *   - stride       : size of one chapter block inside the data blob
 *   - page count   : the field assigned to "ps"
 *   - chapter no.  : the field compared against "ch"
 *   - sub chapter  : the field compared against "part"
 *   - host digits  : the field used as fn(x, 0, 1) and fn(x, 1, 1)
 *   - page codes   : the field used as fn(x, mm(j), <codeLength>)
 */
class Comic8EpisodeParser(episode: EpisodeDTO, listener: EpisodeParserListener) :
    EpisodeParser(episode, listener, "UTF-8") {

    override fun getURLResponse(url: URL, encoding: String): ArrayList<String> {
        var fallback = ArrayList<String>()
        for (candidate in candidateUrls(url.toString())) {
            val lines = Utils.getURLResponse(URL(candidate), refererFor(candidate), encoding)
            if (lines.isEmpty()) continue
            if (fallback.size < lines.size) fallback = lines
            if (lines.any { LOOP_PATTERN.containsMatchIn(it) }) return lines
        }
        return fallback
    }

    override fun getEpisodeFromUrlResult(result: List<String>) {
        episode.imageUrl = ArrayList()
        val script = result.firstOrNull { LOOP_PATTERN.containsMatchIn(it) }
        if (script == null) {
            Log.e("jComics", "8comic: obfuscated script not found - " + episode.episodeUrl)
            return
        }

        val bookId = bookIdOf(episode.episodeUrl)
        val chapter = chapterOf(episode.episodeUrl)
        if (bookId == null || chapter == null) {
            Log.e("jComics", "8comic: cannot read book/chapter id from " + episode.episodeUrl)
            return
        }

        try {
            val info = Comic8Script.parse(script) ?: return
            val urls = info.buildImageUrls(bookId, chapter)
            if (urls.isEmpty()) {
                Log.e("jComics", "8comic: no image found for chapter " + chapter + " of book " + bookId)
                return
            }
            episode.imageUrl = urls
            episode.pageCount = urls.size
        } catch (e: Exception) {
            Log.e("jComics", "8comic: failed to parse " + episode.episodeUrl, e)
        }
    }

    /**
     * The canonical page on v.8comic.com works without any referer, the mirror
     * (articles.onemoreplace.tw) only serves the episode when it is called with a
     * matching referer. Try the canonical page first and the mirror afterwards.
     */
    private fun candidateUrls(url: String): List<String> {
        val candidates = ArrayList<String>()
        val bookId = bookIdOf(url)
        val chapter = chapterOf(url)
        if (bookId != null && chapter != null) {
            candidates.add("$PRIMARY_HOST/online/new-$bookId.html?ch=$chapter")
            candidates.add("$MIRROR_HOST/online/new-$bookId.html?ch=$chapter")
        }
        candidates.add(url)
        return candidates.distinct()
    }

    private fun refererFor(url: String): String {
        return if (url.contains(MIRROR_HOST)) url else "$PRIMARY_HOST/"
    }

    private fun bookIdOf(url: String): String? {
        val m = BOOK_ID_PATTERN.find(url) ?: return null
        return m.groupValues[1]
    }

    private fun chapterOf(url: String): String? {
        val m = CHAPTER_PATTERN.find(url) ?: return null
        val chapter = Comic8Chapter.param(m.groupValues[1])
        return if (chapter.isEmpty()) null else chapter
    }

    companion object {
        private val LOOP_PATTERN = Regex("""for\s*\(\s*var\s+\w+=0;""")
        private const val PRIMARY_HOST = "https://v.8comic.com"
        private const val MIRROR_HOST = "https://articles.onemoreplace.tw"
        private val BOOK_ID_PATTERN = Regex("""online/new-(\d+)\.html""")
        private val CHAPTER_PATTERN = Regex("""[?&]ch=([^&#]*)""")
    }
}

/**
 * 8comic addresses a chapter by its number plus an optional sub chapter, for example
 * "?ch=12" or "?ch=5e". The site itself only understands a single lower case letter as
 * the sub chapter (it is taken out with /[a-z]$/), every other suffix - the chinese
 * characters used for the extra chapters of some books - makes the site fall through to
 * the last chapter instead of the requested one. Those suffixes are therefore dropped and
 * the chapter is addressed by its number only.
 */
internal object Comic8Chapter {

    fun param(label: String): String {
        val value = decode(label.substringBefore('-'))
        val digits = value.takeWhile { it in '0'..'9' }
        if (digits.isEmpty()) return ""
        val suffix = value.substring(digits.length)
        return if (suffix.length == 1 && suffix[0] in 'a'..'z') digits + suffix else digits
    }

    private fun decode(value: String): String {
        return try {
            java.net.URLDecoder.decode(value, "UTF-8")
        } catch (e: Exception) {
            value
        }
    }
}

/**
 * The decoded obfuscation script of one episode page.
 */
internal class Comic8Script private constructor(
    private val data: String,
    private val limit: Int,
    private val stride: Int,
    private val pageCountOffset: Int,
    private val pageCountLength: Int,
    private val chapterOffset: Int,
    private val chapterLength: Int,
    private val subChapterOffset: Int,
    private val subChapterLength: Int,
    private val hostOffset: Int,
    private val hostLength: Int,
    private val codesOffset: Int,
    private val codesLength: Int,
    private val codeLength: Int,
    private val tailBase: Int,
    private val tailLength: Int
) {

    fun buildImageUrls(bookId: String, chapter: String): ArrayList<String> {
        val urls = ArrayList<String>()
        val subChapter = if (chapter.isNotEmpty() && chapter.last() in 'a'..'z') chapter.takeLast(1) else ""
        val chapterNumber = if (subChapter.isNotEmpty() && chapter.length > 1) chapter.dropLast(1) else chapter
        for (i in 0 until limit) {
            val start = i * stride
            val currentChapter = decodeLc(slice(start + chapterOffset, chapterLength)) ?: continue
            val currentSubChapter = slice(start + subChapterOffset, subChapterLength)
            if (currentChapter != chapterNumber) continue
            if (subChapter.isNotEmpty() && subChapter != currentSubChapter) continue

            val pageCount = decodeLc(slice(start + pageCountOffset, pageCountLength))?.toIntOrNull() ?: continue
            val host = decodeLc(slice(start + hostOffset, hostLength)) ?: continue
            val codes = slice(start + codesOffset, codesLength)
            val chapterSegment = encodePath(
                currentChapter + (if (currentSubChapter == "0") "" else currentSubChapter)
            )
            val hostDigit0 = if (host.isNotEmpty()) host[0] else ' '
            val path = if (host.length > 1) host[1] else ' '
            val prefix = tail(4) + hostDigit0 + "." + "8" + tail(3) + tail(2) + tail(3)

            for (j in 1..pageCount) {
                val pageNumber = pageNumberOf(j)
                val codeOffset = codeOffsetOf(j)
                if (codeOffset + codeLength > codes.length) continue
                val code = codes.substring(codeOffset, codeOffset + codeLength)
                urls.add(
                    "https://$prefix/$path/$bookId/$chapterSegment/${pageNumber}_$code." + tail(1)
                )
            }
            return urls
        }
        return urls
    }

    /**
     * The tail of the data blob holds the pieces used to build the image host and the file extension.
     */
    private fun tail(index: Int): String {
        val start = data.length - tailBase - index * tailLength
        if (start < 0 || start + tailLength > data.length) return ""
        return decodeHex(data.substring(start, start + tailLength))
    }

    private fun slice(offset: Int, length: Int): String {
        if (offset < 0 || offset + length > data.length) return ""
        return data.substring(offset, offset + length)
    }

    private fun decodeLc(value: String): String? {
        if (value.length != 2) return value
        val first = ALPHABET.indexOf(value[0])
        val second = ALPHABET.indexOf(value[1])
        if (first < 0 || second < 0) return null
        return if (value[0] == 'Z') (8000 + second).toString() else (first * 52 + second).toString()
    }

    private fun decodeHex(hex: String): String {
        val bytes = ByteArray(hex.length / 2)
        for (i in bytes.indices) {
            bytes[i] = hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return String(bytes, Charsets.UTF_8)
    }

    /**
     * Some books use chinese characters in the chapter name, they have to be percent encoded
     * to be a valid url.
     */
    private fun encodePath(value: String): String {
        val builder = StringBuilder()
        for (byte in value.toByteArray(Charsets.UTF_8)) {
            val c = byte.toInt().toChar()
            if (c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' || c == '-' || c == '_' || c == '.' || c == '~') {
                builder.append(c)
            } else {
                builder.append('%').append(String.format("%02X", byte.toInt() and 0xFF))
            }
        }
        return builder.toString()
    }

    private fun pageNumberOf(page: Int): String {
        return String.format("%03d", page)
    }

    private fun codeOffsetOf(page: Int): Int {
        return (page - 1) / 10 % 10 + (page - 1) % 10 * 3
    }

    companion object {
        private const val ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

        // The site reads a field without an explicit length with a default length of 40 characters.
        private const val DEFAULT_FIELD_LENGTH = 40
        private const val DEFAULT_CODE_LENGTH = 3

        private val LOOP_LIMIT = Regex("""for\s*\(\s*var\s+(\w+)=0;\s*\1<(\d+);\s*\1\+\+\s*\)""")
        private val NUMERIC_CONSTANT = Regex("""var\s+(\w+)\s*=\s*(\d+)\s*;""")
        private val PAGE_COUNT_FIELD = Regex("""(?<![\w$])ps\s*=\s*(\w+)\s*;""")
        private val CONDITION_FIELDS = Regex(
            """if\(\s*(\w+)\s*==\s*ch\s*&&\s*\(part\s*==\s*''\s*\|\|\s*part\s*==\s*(\w+)\s*\)"""
        )
        private val TAIL_SHAPE = Regex("""\.length-(\d+)-\w+\*(\d+)""")
        private val EXPRESSION_TERM = Regex("""([+-]?)(\w+)""")

        fun parse(script: String): Comic8Script? {
            val loop = LOOP_LIMIT.find(script) ?: return null
            val loopVariable = loop.groupValues[1]
            val limit = loop.groupValues[2].toIntOrNull() ?: return null

            val constants = HashMap<String, Int>()
            for (m in NUMERIC_CONSTANT.findAll(script)) {
                val value = m.groupValues[2].toIntOrNull() ?: continue
                constants[m.groupValues[1]] = value
            }

            // Every field of a chapter block is read with the same helper, the loop variable
            // and the stride are the same for all of them.
            val chapterField = Regex(
                """var\s+(\w+)\s*=\s*lc\(\s*(\w+)\(\s*(\w+)\s*,\s*$loopVariable\*\(([^)]*)\)\s*\+\s*(\d+)\s*(?:,\s*(\d+)\s*)?\)\s*\)\s*;"""
            )

            val offsets = HashMap<String, Int>()
            val lengths = HashMap<String, Int>()
            var dataVariable: String? = null
            var helperFunction: String? = null
            var stride = 0
            for (m in chapterField.findAll(script)) {
                val offset = m.groupValues[5].toIntOrNull() ?: continue
                val multiplier = resolveExpression(m.groupValues[4], constants) ?: continue
                offsets[m.groupValues[1]] = offset
                lengths[m.groupValues[1]] = m.groupValues[6].toIntOrNull() ?: DEFAULT_FIELD_LENGTH
                if (dataVariable == null) {
                    dataVariable = m.groupValues[3]
                    helperFunction = m.groupValues[2]
                }
                if (stride == 0) stride = multiplier
            }
            if (dataVariable == null || helperFunction == null || offsets.isEmpty()) return null
            if (stride <= 0) return null

            val data = Regex("""var\s+${Regex.escape(dataVariable)}\s*=\s*'([^']*)'""")
                .find(script)?.groupValues?.get(1) ?: return null
            if (data.isEmpty()) return null

            val pageCountField = PAGE_COUNT_FIELD.findAll(script)
                .map { it.groupValues[1] }
                .firstOrNull { offsets.containsKey(it) } ?: return null
            val pageCountOffset = offsets[pageCountField] ?: return null
            val pageCountLength = lengths[pageCountField] ?: DEFAULT_FIELD_LENGTH
            val condition = CONDITION_FIELDS.find(script) ?: return null
            val chapterOffset = offsets[condition.groupValues[1]] ?: return null
            val chapterLength = lengths[condition.groupValues[1]] ?: DEFAULT_FIELD_LENGTH
            val subChapterOffset = offsets[condition.groupValues[2]] ?: return null
            val subChapterLength = lengths[condition.groupValues[2]] ?: DEFAULT_FIELD_LENGTH
            val hostField = Regex("""${Regex.escape(helperFunction)}\(\s*(\w+)\s*,\s*0\s*,\s*1\s*\)""")
                .find(script)?.groupValues?.get(1) ?: return null
            val hostOffset = offsets[hostField] ?: return null
            val hostLength = lengths[hostField] ?: DEFAULT_FIELD_LENGTH
            val codes = Regex(
                """${Regex.escape(helperFunction)}\(\s*(\w+)\s*,\s*mm\(\s*\w+\s*\)\s*,\s*(\d+)\s*\)"""
            ).find(script) ?: return null
            val codesField = codes.groupValues[1]
            val codesOffset = offsets[codesField] ?: return null
            val codesLength = lengths[codesField] ?: DEFAULT_FIELD_LENGTH
            val codeLength = codes.groupValues[2].toIntOrNull() ?: DEFAULT_CODE_LENGTH

            val tailShape = TAIL_SHAPE.find(script)
            val tailBase = tailShape?.groupValues?.get(1)?.toIntOrNull() ?: 47
            val tailLength = tailShape?.groupValues?.get(2)?.toIntOrNull() ?: 6

            return Comic8Script(
                data, limit, stride, pageCountOffset, pageCountLength, chapterOffset, chapterLength,
                subChapterOffset, subChapterLength, hostOffset, hostLength, codesOffset, codesLength,
                codeLength, tailBase, tailLength
            )
        }

        private fun resolveExpression(expression: String, constants: Map<String, Int>): Int? {
            var total = 0
            var found = false
            for (m in EXPRESSION_TERM.findAll(expression)) {
                val name = m.groupValues[2]
                val value = if (name.all { it.isDigit() }) {
                    name.toIntOrNull() ?: return null
                } else {
                    constants[name] ?: return null
                }
                total += if (m.groupValues[1] == "-") -value else value
                found = true
            }
            return if (found) total else null
        }
    }
}

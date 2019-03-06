package com.jsoft.jcomic.praser

import com.jsoft.jcomic.helper.EpisodeDTO
import java.util.*
import java.util.regex.Pattern

class DM5EpisodeParser(episode: EpisodeDTO, listener: EpisodeParserListener) : EpisodeParser(episode, listener, "UTF-8") {

    private fun intCode(i: Int): String {
        var s = ""
        val characters = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        if (i >= characters.length) {
            s += characters[i / characters.length]
        }
        s += characters[i % characters.length]
        return s
    }

    override fun getEpisodeFromUrlResult(result: List<String>) {
        var s = ""
        for (i in result.indices) {
            s += result[i]
        }

        val imageUrlList = ArrayList<String>()

        val p = Pattern.compile("eval\\(function\\(p,a,c,k,e,d\\)\\{.+\\}\\((.+)\\)\\s*</script>")
        val m = p.matcher(s)
        while (m.find()) {

            val s1 = m.group(1)
            val p1 = Pattern.compile("\\d+,\\d+,\'(.*)\'\\.split")
            val m1 = p1.matcher(s1)
            var replaceStrArray = arrayOfNulls<String>(0)
            while (m1.find()) {
                val s3 = m1.group(1)
                replaceStrArray = s3.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            }

            val s2 = m.group(1)
            val p2 = Pattern.compile("=\\[(.+?)\\];")
            val m2 = p2.matcher(s2)
            while (m2.find()) {
                val s3 = m2.group(1)
                val urls = s3.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (url in urls) {
                    var imgUrl = url
                    imgUrl = imgUrl.replace("\\\\\\'".toRegex(), "")
                    for (j in replaceStrArray.indices) {
                        if (replaceStrArray[j]!!.isNotEmpty()) {
                            imgUrl = imgUrl.replace(("\\b" + intCode(j) + "\\b").toRegex(), replaceStrArray[j]!!)
                        }
                    }
                    imageUrlList.add(imgUrl)
                }
            }
        }
        episode.pageCount = imageUrlList.size
        episode.imageUrl = imageUrlList
    }
}

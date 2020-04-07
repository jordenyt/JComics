package com.jsoft.jcomic.praser

import android.os.AsyncTask
import com.google.gson.Gson
import com.jsoft.jcomic.helper.BookDTO
import com.jsoft.jcomic.helper.EpisodeDTO
import com.jsoft.jcomic.helper.KuMan5EpisodeDTO
import taobe.tec.jcc.JChineseConvertor
import java.net.URL
import java.util.ArrayList
import java.util.regex.Pattern

class KuMan5BookParser(book: BookDTO, listener: BookParserListener) : BookParser(book, listener, "UTF-8") {

    private var loadMore = HashMap<String, Boolean>()
    private var comicId = ""
    private var episodeList = HashMap<String, ArrayList<EpisodeDTO>>()

    private fun chineseS2T(simplifiedChineseString: String): String {
        var result = simplifiedChineseString
        try {
            val jChineseConvertor = JChineseConvertor.getInstance()
            result = jChineseConvertor.s2t(simplifiedChineseString)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }

    inner class GetMoreTask(private val section: String) : AsyncTask<URL, Int, ArrayList<String>>() {

        override fun doInBackground(vararg urls: URL): ArrayList<String> {
            return getURLResponse(urls[0], "UTF-8")
        }

        override fun onPostExecute(result: ArrayList<String>) {
            if (result.size > 0) {
                val s = result.joinToString()
                val gson = Gson()
                val km5EpisodeList: List<KuMan5EpisodeDTO> = gson.fromJson(s , Array<KuMan5EpisodeDTO>::class.java).toList()
                for (km5Episode in km5EpisodeList) {
                    episodeList[section]?.add(EpisodeDTO(km5Episode.name, "http://kuman5.com/" + comicId + "/" + km5Episode.id + ".html"))
                    loadMore[section] = true
                }
            }
            var finished = true
            for(p in loadMore.keys) {
                if (loadMore[p] == false) {
                    finished = false
                }
            }
            if (finished) {
                book.episodes = ArrayList<EpisodeDTO>()
                for(e in episodeList.keys) {
                    episodeList[e]?.let { book.episodes.addAll(it) }
                }
                loadedAllEpisodes()
            }
        }
    }

    override fun getBookFromUrlResult(html: ArrayList<String>):Boolean {
        var episodes = ArrayList<EpisodeDTO>()
        for (i in 2 until html.size) {
            var s = html[i - 2].trim { it <= ' '} + html[i - 1].trim { it <= ' ' } + html[i].trim { it <= ' ' }
            s = s.replace("\n", "")

            val pp = Regex("<li><a href=\"(.+?)\" class=\"d-nowrap\">(.+?)<\\/a><\\/li>")
            val mm : Sequence<MatchResult> = pp.findAll(s, 0)
            mm.forEach() {
                val episodeUrl = "http://kuman5.com/" +  it.groupValues[1]
                val episodeTitle = chineseS2T(it.groupValues[2])
                var found = false
                for (e in episodes) {
                    if (e.episodeUrl.trim() == episodeUrl) {
                        found = true
                        break
                    }
                }
                if (!found) {
                    episodes.add(EpisodeDTO(episodeTitle, episodeUrl))
                }
            }

            var p = Pattern.compile(".*<p class=\\\"d-nowrap-clamp d-nowrap-clamp-2\\\">(.*?)<\\/p>.*")
            var m = p.matcher(s)
            if (m.matches()) {
                book.bookSynopsis = chineseS2T(m.group(1).replace("&nbsp;", " ").replace("<.*?>".toRegex(), ""))
            }

            p = Pattern.compile(".*<div class=\\\"pic\\\" id=\\\"Cover\\\">\\s*<img src=\\\"(.*?)\\\" width=\\\"100%\\\" title=\\\"(.*?)\\\">\\s*<\\/div>.*")
            m = p.matcher(s)
            if (m.matches()) {
                book.bookImgUrl = m.group(1)
                book.bookTitle = chineseS2T(m.group(2).replace("&nbsp;", " ").replace("<.*?>".toRegex(), ""))
            }

            //p = Pattern.compile(".*javascript:jj_charpter\\(\\\"(\\d+)\\\",\\\"(\\d+)\\\",\\\"(\\d+)\\\"\\).*")
            p = Pattern.compile(".*<a class=\\\"mm\\\" id=\\\"all_mores\\d+\\\" data-id=\\\"(\\d+)\\\" data-vid=\\\"(\\d+)\\\" data-aid=\\\"(\\d+)\\\">.*")
            m = p.matcher(s)
            if (m.matches()) {
                if (!episodeList.keys.contains(m.group(2))) {
                    comicId = m.group(1)
                    episodeList[m.group(2)] = episodes
                    loadMore[m.group(2)] = false
                    episodes = ArrayList<EpisodeDTO>()
                }
            }
        }
        return if (loadMore.count() > 0) {
            for (p in loadMore.keys) {
                GetMoreTask(p).execute(URL("http://kuman5.com/bookchapter/?id=$comicId&id2=$p"))
            }
            false
        } else {
            true
        }
    }
}
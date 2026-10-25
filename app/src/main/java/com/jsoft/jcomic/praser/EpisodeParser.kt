package com.jsoft.jcomic.praser

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import com.jsoft.jcomic.helper.EpisodeDTO
import com.jsoft.jcomic.helper.Utils
import java.net.URL
import java.util.*

/**
 * Created by 01333855 on 02/10/2015.
 */
abstract class EpisodeParser(protected var episode: EpisodeDTO, protected var listener: EpisodeParserListener, encoding: String) {

    // Parsers that need more than one request to read a chapter set this, the listener is then
    // called as soon as the result is there.
    private var resultPending = false

    init {
        try {
            DownloadFilesTask(encoding).execute(URL(episode.episodeUrl))
        } catch (e: Exception) {
            Log.e("jComics", "Exception: " + episode.episodeUrl)
        }

    }

    inner class DownloadFilesTask(private val encoding: String) : AsyncTask<URL, Int, ArrayList<String>>() {

        override fun doInBackground(vararg urls: URL): ArrayList<String> {
            return getURLResponse(urls[0], encoding)
        }

        override fun onPostExecute(result: ArrayList<String>) {
            try {
                getEpisodeFromUrlResult(result)
                if (!resultPending) {
                    listener.onEpisodeFetched(episode)
                }
            } catch (e: Exception) {
                Log.e("jComics", "Exception caught in EpisodeParser", e)
                listener.onEpisodeFetched(episode)
            }
        }
    }

    protected open fun getEpisodeFromUrlResult(result: List<String>) {

    }

    protected open fun getURLResponse(url: URL, encoding: String): ArrayList<String> {
        return Utils.getURLResponse(url, episode.episodeUrl, encoding)
    }

    /**
     * The result is not ready when the request is done, the parser calls finishEpisodeResult()
     * as soon as it has the images of the chapter.
     */
    protected fun postponeEpisodeResult() {
        resultPending = true
    }

    protected fun finishEpisodeResult() {
        resultPending = false
        listener.onEpisodeFetched(episode)
    }

    companion object {

        fun parseEpisode(episode: EpisodeDTO, listener: EpisodeParserListener, context: Context) {
            when {
                episode.episodeUrl.contains("8comic.com")
                        || episode.episodeUrl.contains("onemoreplace.tw") -> Comic8EpisodeParser(episode, listener)
                episode.episodeUrl.contains("manga.bilibili.com") -> BilibiliMangaEpisodeParser(episode, listener, context)
                episode.episodeUrl.contains("kalamanhua.com") -> KalamEpisodeParser(episode, listener)
                episode.episodeUrl.contains("dm5.com") -> DM5EpisodeParser(episode, listener)
                episode.episodeUrl.contains("qimiaomh.com") -> QiMiaoEpisodeParser(episode, listener)
                episode.episodeUrl.contains("kuman5.com") -> KuMan5EpisodeParser(episode, listener)
            }
        }
    }
}

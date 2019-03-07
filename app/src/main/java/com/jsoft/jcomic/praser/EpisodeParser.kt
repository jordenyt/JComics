package com.jsoft.jcomic.praser

import android.os.AsyncTask
import android.util.Log
import com.jsoft.jcomic.helper.EpisodeDTO
import com.jsoft.jcomic.helper.Utils
import java.net.MalformedURLException
import java.net.URL
import java.util.*

/**
 * Created by 01333855 on 02/10/2015.
 */
abstract class EpisodeParser(protected var episode: EpisodeDTO, protected var listener: EpisodeParserListener, encoding: String) {

    init {
        try {
            DownloadFilesTask(encoding).execute(URL(episode.episodeUrl))
        } catch (e: MalformedURLException) {
            Log.e("jComics", "MalformedURLException: " + episode.episodeUrl!!)
        }

    }

    inner class DownloadFilesTask(private val encoding: String) : AsyncTask<URL, Int, ArrayList<String>>() {

        override fun doInBackground(vararg urls: URL): ArrayList<String> {
            return getURLResponse(urls[0], encoding)
        }

        override fun onPostExecute(result: ArrayList<String>) {
            getEpisodeFromUrlResult(result)
            listener.onEpisodeFetched(episode)
        }
    }

    protected open fun getEpisodeFromUrlResult(result: List<String>) {

    }

    protected open fun getURLResponse(url: URL, encoding: String): ArrayList<String> {
        return Utils.getURLResponse(url, episode.episodeUrl, encoding)
    }

    companion object {

        fun parseEpisode(episode: EpisodeDTO, listener: EpisodeParserListener) {
            if (episode.episodeUrl!!.contains("comicbus")) {
                ComicVIPEpisodeParser(episode, listener)
            } else if (episode.episodeUrl!!.contains("cartoonmad")) {
                CartoonMadEpisodeParser(episode, listener)
            } else if (episode.episodeUrl!!.contains("dm5.com")) {
                DM5EpisodeParser(episode, listener)
            }
        }
    }
}
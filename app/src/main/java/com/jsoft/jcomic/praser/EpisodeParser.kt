package com.jsoft.jcomic.praser

import android.os.AsyncTask
import android.util.Log

import com.jsoft.jcomic.helper.EpisodeDTO

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.MalformedURLException
import java.net.URL
import java.util.ArrayList

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
            val result = ArrayList<String>()
            for (url in urls) {
                var readLine: String?
                try {
                    val `is` = url.openStream()
                    val `in` = BufferedReader(InputStreamReader(`is`, encoding))
                    do {
                        readLine = `in`.readLine()
                        if (readLine != null) result.add(readLine)
                    } while (readLine != null)
                    `is`.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
            return result
        }

        override fun onProgressUpdate(vararg progress: Int?) {
            //setProgressPercent(progress[0]);
        }

        override fun onPostExecute(result: ArrayList<String>) {
            getEpisodeFromUrlResult(result)
            listener.onEpisodeFetched(episode)
        }
    }

    protected open fun getEpisodeFromUrlResult(result: List<String>) {

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

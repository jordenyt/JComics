package com.jsoft.jcomic.praser;

import android.os.AsyncTask;
import android.util.Log;

import com.jsoft.jcomic.FullscreenActivity;
import com.jsoft.jcomic.helper.EpisodeDTO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by 01333855 on 02/10/2015.
 */
public abstract class EpisodeParser {

    protected EpisodeDTO episode;
    protected EpisodeParserListener listener;

    public EpisodeParser(EpisodeDTO episode, EpisodeParserListener listener, String encoding) {
        this.episode = episode;
        this.listener = listener;
        try {
            new DownloadFilesTask(encoding).execute(new URL(episode.getEpisodeUrl()));
        } catch (MalformedURLException e) {
            Log.e("jComics", "MalformedURLException: " + episode.getEpisodeUrl());
        }
    }

    public class DownloadFilesTask extends AsyncTask<URL, Integer, List<String>> {

        private String encoding;

        public DownloadFilesTask(String encoding) {
            this.encoding = encoding;
        }

        protected List<String> doInBackground(URL... urls) {
            int count = urls.length;
            List<String> result = new ArrayList<String>();
            for (int i = 0; i < count; i++) {
                String readLine;
                try {
                    InputStream is = urls[i].openStream();
                    BufferedReader in = new BufferedReader(new InputStreamReader(is, encoding));
                    while ((readLine = in.readLine()) != null) {
                        result.add(readLine);
                    }
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return result;
        }

        protected void onProgressUpdate(Integer... progress) {
            //setProgressPercent(progress[0]);
        }

        protected void onPostExecute(List<String> result) {
            getEpisodeFromUrlResult(result);
            listener.onEpisodeFetched(episode);
        }
    }

    protected void getEpisodeFromUrlResult(List<String> result) {

    };

    public static void parseEpisode(EpisodeDTO episode, EpisodeParserListener listener) {
        if (episode.getEpisodeUrl().contains("comicbus")) {
            new ComicVIPEpisodeParser(episode, listener);
        } else if (episode.getEpisodeUrl().contains("cartoonmad")) {
            new CartoonMadEpisodeParser(episode, listener);
        } else if (episode.getEpisodeUrl().contains("dm5.com")) {
            new DM5EpisodeParser(episode, listener);
        }
    }
}

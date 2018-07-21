package com.jsoft.jcomic.praser;

import android.os.AsyncTask;
import android.util.Log;

import com.jsoft.jcomic.FullscreenActivity;
import com.jsoft.jcomic.helper.EpisodeDTO;

import java.io.BufferedReader;
import java.io.IOException;
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
    protected FullscreenActivity activity;

    public EpisodeParser(EpisodeDTO episode, FullscreenActivity activity, String encoding) {
        this.episode = episode;
        this.activity = activity;
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
                    BufferedReader in = new BufferedReader(new InputStreamReader(urls[i].openStream(), encoding));
                    while ((readLine = in.readLine()) != null) {
                        result.add(readLine);
                    }
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
        }
    }

    protected void getEpisodeFromUrlResult(List<String> result) {

    };
}

package com.jsoft.jcomic.praser;

import android.os.AsyncTask;
import android.util.Log;

import com.jsoft.jcomic.EpisodeListActivity;
import com.jsoft.jcomic.helper.BookDTO;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public abstract class BookParser {

    protected BookDTO book;
    protected EpisodeListActivity activity;

    public BookParser(BookDTO book, EpisodeListActivity activity, String encoding) {
        this.book = book;
        this.activity = activity;
        try {
            new DownloadFilesTask(encoding).execute(new URL(book.getBookUrl()));
        } catch (MalformedURLException e) {
            Log.e("jComics", "MalformedURLException: " + book.getBookUrl());
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
                } catch (MalformedURLException e) {
                    Log.e("jComics", "MalformedURLException: " + urls[i]);
                } catch (Exception e) {
                    Log.e("jComics", "Exception when getting file: " + urls[i]);
                }
            }
            return result;
        }

        protected void onProgressUpdate(Integer... progress) {
            //setProgressPercent(progress[0]);
        }

        protected void onPostExecute(List<String> result) {
            getBookFromUrlResult(result);
        }
    }

    protected void getBookFromUrlResult(List<String> html) {};
}

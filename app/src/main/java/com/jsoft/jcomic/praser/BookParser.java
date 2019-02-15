package com.jsoft.jcomic.praser;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.jsoft.jcomic.EpisodeListActivity;
import com.jsoft.jcomic.helper.BookDTO;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
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
                URL url=urls[i];
                Uri uri = Uri.parse(url.toString());
                try {
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setReadTimeout(5000);
                    conn.setUseCaches(true);
                    if (uri.getHost().contains("dm5.com") && uri.getQueryParameter("from") != null) {
                        conn.setRequestProperty("Referer", "http://m.dm5.com" + uri.getQueryParameter("from"));
                    }
                    InputStream is = new BufferedInputStream(conn.getInputStream());
                    BufferedReader in = new BufferedReader(new InputStreamReader(is, encoding));
                    while ((readLine = in.readLine()) != null) {
                        result.add(readLine);
                    }
                    in.close();
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

package com.jsoft.jcomic.praser;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.jsoft.jcomic.helper.BookDTO;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public abstract class BookParser {

    protected BookDTO book;
    protected BookParserListener listener;

    public BookParser(BookDTO book, BookParserListener listener, String encoding) {
        this.book = book;
        this.listener = listener;
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

            List<String> result = new ArrayList<>();
            for (URL url : urls) {
                String readLine;
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
                } catch (Exception e) {
                    result = new ArrayList<>();
                    Log.e("jComics", "Exception when getting file: " + url, e);
                }
            }
            return result;
        }

        protected void onProgressUpdate(Integer... progress) {
            //setProgressPercent(progress[0]);
        }

        protected void onPostExecute(List<String> result) {
            if (result.size() > 0) {
                getBookFromUrlResult(result);
                listener.onBookFetched(book);
            } else if (book.getBookSynopsis() == null) {
                book.setBookSynopsis("Cannot load page from Internet");
                listener.onBookFetched(book);
            }
        }
    }

    protected void getBookFromUrlResult(List<String> html) {}

    public static void parseBook(BookDTO book, BookParserListener listener) {
        if (book.getBookUrl().contains("comicbus")) {
            new ComicVIPBookParser(book, listener);
        } else if (book.getBookUrl().contains("cartoonmad")) {
            new CartoonMadBookParser(book, listener);
        } else if (book.getBookUrl().contains("dm5.com")) {
            new DM5BookParser(book, listener);
        }
    }
}

package com.jsoft.jcomic.helper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.jsoft.jcomic.praser.EpisodeParser;
import com.jsoft.jcomic.praser.EpisodeParserListener;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.google.gson.Gson;

public class Downloader implements EpisodeParserListener {
    BookDTO book;

    public Downloader(BookDTO book) {
        this.book = book;
    }

    public void downloadEpisode(int position) {
        Log.d("jComics", "downloadEpisode");
        if (book != null && book.getEpisodes().size() > position) {
            EpisodeParser.parseEpisode(book.getEpisodes().get(position), this);
        }
    }

    public void onEpisodeFetched(EpisodeDTO episode) {
        Gson gson = new Gson();
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/jComics");
        myDir.mkdirs();
        myDir = new File(root + "/jComics/" + Utils.getHashCode(book.getBookUrl()));
        myDir.mkdirs();

        try {
            File nomediaFile = new File(myDir, ".nomedia");
            nomediaFile.createNewFile();
            Utils.writeToFile(gson.toJson(book), myDir, "book.json");
        } catch (Exception e) {
            Log.e("jComics", "Create book Folder Error", e);
        }

        myDir = new File(root + "/jComics/" + Utils.getHashCode(book.getBookUrl()) + "/" + Utils.getHashCode(episode.getEpisodeUrl()));
        myDir.mkdirs();

        try {
            File nomediaFile = new File(myDir, ".nomedia");
            nomediaFile.createNewFile();
            Utils.writeToFile(gson.toJson(episode), myDir, "episode.json");
        } catch (Exception e) {
            Log.e("jComics", "Create episode Folder Error", e);
        }

        Executor downloadImageTaskExecutor = Executors.newFixedThreadPool(3);
        for (int i=0;i<episode.getImageUrl().size();i++) {
            new DownloadImageTask(book, episode).executeOnExecutor(downloadImageTaskExecutor, episode.getImageUrl().get(i));
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Integer, Bitmap> {
        EpisodeDTO episode;
        BookDTO book;
        String imgUrl;

        public DownloadImageTask(BookDTO book, EpisodeDTO episode) {
            this.book = book;
            this.episode = episode;
        }

        protected Bitmap doInBackground(String... urls) {
            this.imgUrl = urls[0];
            Bitmap bitmap = null;
            try {
                HttpURLConnection conn = (HttpURLConnection) new java.net.URL(this.imgUrl).openConnection();
                conn.setReadTimeout(5000);
                conn.setUseCaches(true);
                conn.setRequestProperty("Referer", episode.getEpisodeUrl());
                Log.e("jComics", "Downloading "+this.imgUrl);
                InputStream in = new BufferedInputStream(conn.getInputStream());
                bitmap= BitmapFactory.decodeStream(in);

                int length=conn.getContentLength();
                int len=0,total_length=0,value=0;
                byte[] data=new byte[1024];

                while((len = in.read(data)) != -1){
                    total_length += len;
                    value = (int)((total_length/(float)length)*100);
                    publishProgress(value);
                }
                in.close();
                conn.disconnect();
            } catch (Exception e) {
                Log.e("jComic", "Exception caught in Downloader.DownloadImageTask", e);
            }
            //Log.e("jComic", "Finish Get Image: " + urldisplay);
            return bitmap;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
        }

        protected void onPostExecute(Bitmap result) {
            saveImage(result);
        }

        private void saveImage(Bitmap finalBitmap) {
            String root = Environment.getExternalStorageDirectory().toString();
            File myDir = new File(root + "/jComics/" + Utils.getHashCode(book.getBookUrl()) + "/" + Utils.getHashCode(episode.getEpisodeUrl()));
            int pageNum = 0;
            for (int i=0; i<episode.getImageUrl().size(); i++) {
                if (episode.getImageUrl().get(i).equals(this.imgUrl)) {
                    pageNum = i;
                    break;
                }
            }
            String fname = String.format("%04d", pageNum) + ".jpg";
            File file = new File (myDir, fname);

            //Log.e("jComics", "Saving to " + myDir + "/" + fname);
            if (file.exists ()) file.delete ();
            try {
                FileOutputStream out = new FileOutputStream(file);
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                out.flush();
                out.close();
            } catch (Exception e) {
                Log.e("jComics", "Write File Error", e);
            }
        }

    }

}

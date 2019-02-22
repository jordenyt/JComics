package com.jsoft.jcomic.helper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.jsoft.jcomic.praser.EpisodeParser;
import com.jsoft.jcomic.praser.EpisodeParserListener;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Random;

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
        for (int i=0;i<episode.getImageUrl().size();i++) {
            new DownloadImageTask(book, episode).execute(episode.getImageUrl().get(i));
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

            File myDir = new File(root + "/jComics");
            myDir.mkdirs();
            myDir = new File(root + "/jComics/" + Utils.getHashCode(book.getBookUrl()));
            myDir.mkdirs();
            myDir = new File(root + "/jComics/" + Utils.getHashCode(book.getBookUrl()) + "/" + Utils.getHashCode(episode.getEpisodeUrl()));
            myDir.mkdirs();

            int pageNum = 0;
            for (int i=0; i<episode.getImageUrl().size(); i++) {
                if (episode.getImageUrl().get(i).equals(this.imgUrl)) {
                    pageNum = i;
                    break;
                }
            }
            try {
                File nomediafile = new File(myDir, ".nomedia");
                nomediafile.createNewFile();
            } catch (Exception e) {
                Log.e("jComics", "Write .noMedia File Error", e);
            }
            String fname = String.format("%04d", pageNum) + ".jpg";
            File file = new File (myDir, fname);
            Log.e("jComics", "Saving to " + myDir + "/" + fname);
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

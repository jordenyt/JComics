package com.jsoft.jcomic.helper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.jsoft.jcomic.EpisodeListActivity;
import com.jsoft.jcomic.praser.EpisodeParser;
import com.jsoft.jcomic.praser.EpisodeParserListener;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.google.gson.Gson;

public class Downloader implements EpisodeParserListener {
    BookDTO book;
    EpisodeDTO episode;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private Context activity;
    private int pageTotal;
    private int pageDownloaded;
    private int notificationID;
    private int numMissingPage;

    public Downloader(BookDTO book, Context activity) {
        this.book = book;
        this.activity = activity;
    }

    public void downloadEpisode(int position) {
        Log.d("jComics", "downloadEpisode");
        if (book != null && book.getEpisodes().size() > position) {
            EpisodeParser.parseEpisode(book.getEpisodes().get(position), this);

            setNotification(activity);
            pageDownloaded = 0;
        }
    }

    public void setNotification(Context mContext) {
        mBuilder =
                new NotificationCompat.Builder(mContext.getApplicationContext(), "notify_001");
        Intent ii = new Intent(mContext.getApplicationContext(), EpisodeListActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, ii, 0);

        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setSmallIcon(android.R.drawable.stat_sys_download);

        mNotifyManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "DOWNLOAD_EPISODE";
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Downloading Episode",
                    NotificationManager.IMPORTANCE_LOW);
            mNotifyManager.createNotificationChannel(channel);
            mBuilder.setChannelId(channelId);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("HHmmss");
        notificationID = Integer.parseInt(sdf.format(new Date()));

        mNotifyManager.notify(notificationID, mBuilder.build());
    }

    public void onEpisodeFetched(EpisodeDTO episode) {
        pageTotal = episode.getImageUrl().size();
        numMissingPage = 0;
        this.episode = episode;
        mBuilder.setContentTitle("正在下載" + book.getBookTitle() + "-" + episode.getEpisodeTitle());
        mBuilder.setContentText("0%");
        mBuilder.setProgress(100, 0, false);
        mNotifyManager.notify(notificationID, mBuilder.build());

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



        Executor downloadImageTaskExecutor = Executors.newFixedThreadPool(5);
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
                Thread.sleep(1000);
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
            if (result != null) {
                saveImage(episode, result);
            } else {
                numMissingPage += 1;
            }
            pageDownloaded += 1;

            if (pageDownloaded == pageTotal) {
                mBuilder.setContentTitle("已完成下載" + book.getBookTitle() + "-" + episode.getEpisodeTitle());
                mBuilder.setContentText("");
                mBuilder.setSmallIcon(android.R.drawable.stat_sys_download_done);
                if (numMissingPage > 0) {
                    mBuilder.setContentText("有" + numMissingPage + "/" + pageTotal + "部份未能下載");
                    mBuilder.setSmallIcon(android.R.drawable.stat_sys_warning);
                }

                mBuilder.setProgress(0,0,false);
                mNotifyManager.notify(notificationID, mBuilder.build());
            } else {
                mBuilder.setContentText(pageDownloaded * 100 / pageTotal + "%");
                mBuilder.setProgress(pageTotal, pageDownloaded, false);
                mNotifyManager.notify(notificationID, mBuilder.build());
            }
        }

        private void saveImage(EpisodeDTO episode, Bitmap finalBitmap) {
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

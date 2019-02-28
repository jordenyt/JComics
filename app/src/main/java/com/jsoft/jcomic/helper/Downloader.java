package com.jsoft.jcomic.helper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.jsoft.jcomic.DownloadListActivity;
import com.jsoft.jcomic.praser.EpisodeParser;
import com.jsoft.jcomic.praser.EpisodeParserListener;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
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
    private static Executor downloadImageTaskExecutor;
    private Timer notifyTimer;


    public Downloader(BookDTO book, Context activity) {
        this.book = book;
        this.activity = activity;
        if (downloadImageTaskExecutor == null) {
            downloadImageTaskExecutor = Executors.newFixedThreadPool(5);
        }
    }

    public void downloadEpisode(int position) {
        Log.d("jComics", "downloadEpisode");
        if (book != null && book.getEpisodes().size() > position) {
            EpisodeParser.parseEpisode(book.getEpisodes().get(position), this);
        }
    }

    public void setNotification(Context mContext) {
        mBuilder = new NotificationCompat.Builder(mContext.getApplicationContext(), "notify_001");
        Intent ii = new Intent(mContext.getApplicationContext(), DownloadListActivity.class);
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
        setNotification(activity);
        pageDownloaded = 0;

        pageTotal = episode.getImageUrl().size();
        numMissingPage = 0;
        this.episode = episode;
        mBuilder.setContentTitle("正在下載" + book.getBookTitle() + "-" + episode.getEpisodeTitle());
        mBuilder.setContentText("0%");
        mBuilder.setProgress(100, 0, false);
        mNotifyManager.notify(notificationID, mBuilder.build());

        Gson gson = new Gson();
        File myDir = Utils.getRootFile();
        myDir.mkdirs();
        myDir = Utils.getBookFile(book);
        myDir.mkdirs();

        try {
            File nomediaFile = new File(myDir, ".nomedia");
            nomediaFile.createNewFile();
            BookDTO cloneBook = book.clone();
            for (int i=0; i< cloneBook.getEpisodes().size(); i++) {
                cloneBook.getEpisodes().get(i).setImageUrl(null);
            }
            cloneBook.setBookImg(null);
            Utils.writeToFile(gson.toJson(cloneBook), myDir, "book.json");

            File bookImg = new File(myDir, "book.jpg");
            saveImage(bookImg, book.getBookImg());
        } catch (Exception e) {
            Log.e("jComics", "Create book Folder Error", e);
        }

        myDir = Utils.getEpisodeFile(book, episode);
        myDir.mkdirs();

        try {
            File nomediaFile = new File(myDir, ".nomedia");
            nomediaFile.createNewFile();
            Utils.writeToFile(gson.toJson(episode), myDir, "episode.json");
        } catch (Exception e) {
            Log.e("jComics", "Create episode Folder Error", e);
        }



        for (int i=0;i<episode.getImageUrl().size();i++) {
            File file = Utils.getImgFile(book, episode, i);
            if (file.exists()) {
                Log.e("jComics", "Found "+episode.getImageUrl().get(i));
                pageDownloaded += 1;
            } else {
                new DownloadImageTask(episode).executeOnExecutor(downloadImageTaskExecutor, episode.getImageUrl().get(i));
            }
        }
        notifyTimer = new Timer();
        notifyTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateDownloadNotification();
            }
        }, 0, 1000);
    }

    private class DownloadImageTask extends AsyncTask<String, Integer, Bitmap> {
        EpisodeDTO episode;
        String imgUrl;

        public DownloadImageTask(EpisodeDTO episode) {
            this.episode = episode;
        }

        protected Bitmap doInBackground(String... urls) {
            this.imgUrl = urls[0];
            Bitmap bitmap = null;
            try {
                bitmap = Utils.downloadImage(imgUrl, episode.getEpisodeUrl());
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
                saveImage(this.imgUrl, episode, result);
            } else {
                numMissingPage += 1;
            }
            pageDownloaded += 1;
            updateDownloadNotification();
        }
    }

    private void updateDownloadNotification() {
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
            notifyTimer.cancel();
        }
    }

    private void saveImage(File file, Bitmap finalBitmap) {
        if (!file.exists ()) {
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

    private void saveImage(String imgUrl, EpisodeDTO episode, Bitmap finalBitmap) {
        int pageNum = episode.getPageNumByURL(imgUrl);
        File file = Utils.getImgFile(book, episode, pageNum);
        saveImage(file, finalBitmap);
    }

}

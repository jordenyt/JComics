package com.jsoft.jcomic.helper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.AsyncTask
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.util.Log

import com.jsoft.jcomic.DownloadListActivity
import com.jsoft.jcomic.praser.EpisodeParser
import com.jsoft.jcomic.praser.EpisodeParserListener

import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.Executor
import java.util.concurrent.Executors

import com.google.gson.Gson

class Downloader(internal var book: BookDTO?, private val activity: Context) : EpisodeParserListener {
    internal var episode: EpisodeDTO = EpisodeDTO("","")
    private var mNotifyManager: NotificationManager? = null
    private var mBuilder: NotificationCompat.Builder? = null
    private var pageTotal: Int = 0
    private var pageDownloaded: Int = 0
    private var notificationID: Int = 0
    private var numMissingPage: Int = 0
    private var notifyTimer: Timer? = null


    init {
        if (downloadImageTaskExecutor == null) {
            downloadImageTaskExecutor = Executors.newFixedThreadPool(5)
        }
    }

    fun downloadEpisode(position: Int) {
        Log.d("jComics", "downloadEpisode")
        if (book != null && book!!.episodes!!.size > position) {
            EpisodeParser.parseEpisode(book!!.episodes!![position], this)
        }
    }

    private fun setNotification(mContext: Context) {
        mBuilder = NotificationCompat.Builder(mContext.applicationContext, "notify_001")
        val ii = Intent(mContext.applicationContext, DownloadListActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(mContext, 0, ii, 0)

        mBuilder!!.setContentIntent(pendingIntent)
        mBuilder!!.setSmallIcon(android.R.drawable.stat_sys_download)

        mNotifyManager = mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "DOWNLOAD_EPISODE"
            val channel = NotificationChannel(channelId,
                    "Downloading Episode",
                    NotificationManager.IMPORTANCE_LOW)
            mNotifyManager!!.createNotificationChannel(channel)
            mBuilder!!.setChannelId(channelId)
        }

        val sdf = SimpleDateFormat("HHmmss")
        notificationID = Integer.parseInt(sdf.format(Date()))

        mNotifyManager!!.notify(notificationID, mBuilder!!.build())
    }

    override fun onEpisodeFetched(episode: EpisodeDTO) {
        setNotification(activity)
        pageDownloaded = 0

        pageTotal = episode.imageUrl!!.size
        numMissingPage = 0
        this.episode = episode
        mBuilder!!.setContentTitle("正在下載" + book!!.bookTitle + "-" + episode.episodeTitle)
        mBuilder!!.setContentText("0%")
        mBuilder!!.setProgress(100, 0, false)
        mNotifyManager!!.notify(notificationID, mBuilder!!.build())

        val gson = Gson()
        var myDir = Utils.rootFile
        myDir.mkdirs()
        myDir = Utils.getBookFile(book!!)
        myDir.mkdirs()

        try {
            val nomediaFile = File(myDir, ".nomedia")
            nomediaFile.createNewFile()
            val cloneBook = book!!.clone()
            for (i in 0 until cloneBook.episodes!!.size) {
                cloneBook.episodes!![i].imageUrl = null
            }
            cloneBook.bookImg = null
            Utils.writeToFile(gson.toJson(cloneBook), myDir, "book.json")

            val bookImg = File(myDir, "book.jpg")
            saveImage(bookImg, book!!.bookImg)
        } catch (e: Exception) {
            Log.e("jComics", "Create book Folder Error", e)
        }

        myDir = Utils.getEpisodeFile(book!!, episode)
        myDir.mkdirs()

        try {
            val nomediaFile = File(myDir, ".nomedia")
            nomediaFile.createNewFile()
            Utils.writeToFile(gson.toJson(episode), myDir, "episode.json")
        } catch (e: Exception) {
            Log.e("jComics", "Create episode Folder Error", e)
        }



        for (i in 0 until episode.imageUrl!!.size) {
            val file = Utils.getImgFile(book!!, episode, i)
            if (file.exists()) {
                Log.e("jComics", "Found " + episode.imageUrl!![i])
                pageDownloaded += 1
            } else {
                DownloadImageTask(episode).executeOnExecutor(downloadImageTaskExecutor, episode.imageUrl!![i])
            }
        }
        notifyTimer = Timer()
        notifyTimer!!.schedule(object : TimerTask() {
            override fun run() {
                updateDownloadNotification()
            }
        }, 0, 1000)
    }

    private inner class DownloadImageTask(internal var episode: EpisodeDTO) : AsyncTask<String, Int, Bitmap>() {
        internal var imgUrl: String = ""

        override fun doInBackground(vararg urls: String): Bitmap? {
            this.imgUrl = urls[0]
            var bitmap: Bitmap? = null
            try {
                bitmap = Utils.downloadImage(imgUrl, episode.episodeUrl)
                Thread.sleep(1000)
            } catch (e: Exception) {
                Log.e("jComic", "Exception caught in Downloader.DownloadImageTask", e)
            }

            //Log.e("jComic", "Finish Get Image: " + urldisplay);
            return bitmap
        }

        override fun onPostExecute(result: Bitmap?) {
            if (result != null) {
                saveImage(this.imgUrl, episode, result)
            } else {
                numMissingPage += 1
            }
            pageDownloaded += 1
            updateDownloadNotification()
        }
    }

    private fun updateDownloadNotification() {
        if (pageDownloaded == pageTotal) {
            mBuilder!!.setContentTitle("已完成下載" + book!!.bookTitle + "-" + episode.episodeTitle)
            mBuilder!!.setContentText("")
            mBuilder!!.setSmallIcon(android.R.drawable.stat_sys_download_done)
            if (numMissingPage > 0) {
                mBuilder!!.setContentText("有" + numMissingPage + "/" + pageTotal + "部份未能下載")
                mBuilder!!.setSmallIcon(android.R.drawable.stat_sys_warning)
            }

            mBuilder!!.setProgress(0, 0, false)
            mNotifyManager!!.notify(notificationID, mBuilder!!.build())
        } else {
            mBuilder!!.setContentText((pageDownloaded * 100 / pageTotal).toString() + "%")
            mBuilder!!.setProgress(pageTotal, pageDownloaded, false)
            mNotifyManager!!.notify(notificationID, mBuilder!!.build())
            notifyTimer!!.cancel()
        }
    }

    private fun saveImage(file: File, finalBitmap: Bitmap?) {
        if (!file.exists()) {
            try {
                val out = FileOutputStream(file)
                finalBitmap!!.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
                out.close()
            } catch (e: Exception) {
                Log.e("jComics", "Write File Error", e)
            }

        }
    }

    private fun saveImage(imgUrl: String, episode: EpisodeDTO, finalBitmap: Bitmap) {
        val pageNum = episode.getPageNumByURL(imgUrl)
        val file = Utils.getImgFile(book!!, episode, pageNum)
        saveImage(file, finalBitmap)
    }

    companion object {
        private var downloadImageTaskExecutor: Executor? = null
    }

}

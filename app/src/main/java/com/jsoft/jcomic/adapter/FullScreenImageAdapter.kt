package com.jsoft.jcomic.adapter

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.os.AsyncTask
import android.support.v4.view.PagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.jsoft.jcomic.R
import com.jsoft.jcomic.helper.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class FullScreenImageAdapter// constructor
(private val _activity: Activity, private val pager: ComicsViewPager, var episode: EpisodeDTO?, private val book: BookDTO) : PagerAdapter() {

    init {
        if (downloadImageTaskExecutor == null) {
            downloadImageTaskExecutor = Executors.newFixedThreadPool(5)
        }
    }

    override fun getCount(): Int {
        return if (this.episode!!.pageCount > 0) this.episode!!.pageCount else 1
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val imgDisplay: TouchImageView

        val inflater = _activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val viewLayout = inflater.inflate(R.layout.layout_fullscreen_image, container, false)

        imgDisplay = viewLayout.findViewById(R.id.imgDisplay)
        val progressText = viewLayout.findViewById<TextView>(R.id.progress_textview)
        val statusText = viewLayout.findViewById<TextView>(R.id.status_textview)

        if (episode!!.pageCount > 0) {
            progressText.text = "Downloading..."
            statusText.text = episode!!.bookTitle + " - " + episode!!.episodeTitle + "    Page: " + (position + 1) + " / " + episode!!.pageCount

            val file = Utils.getImgFile(book, episode!!, episode!!.getPageNumByURL(episode!!.imageUrl!![position]))
            if (file.exists()) {
                imgDisplay.setImageBitmap(Utils.imageFromFile(file))
                progressText.text = ""
            } else {
                DownloadImageTask(imgDisplay, progressText).executeOnExecutor(downloadImageTaskExecutor, episode!!.imageUrl!![position])
            }
        } else {
            progressText.text = "No Image"
            statusText.text = episode!!.bookTitle + " - " + episode!!.episodeTitle
        }
        imgDisplay.setPager(pager)

        container.addView(viewLayout)

        return viewLayout
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as RelativeLayout)

    }

    private inner class DownloadImageTask(internal var bmImage: ImageView, internal var progressText: TextView) : AsyncTask<String, Int, Bitmap>() {

        override fun doInBackground(vararg urls: String): Bitmap? {
            return Utils.downloadImage(urls[0], episode!!.episodeUrl)
        }

        override fun onProgressUpdate(vararg progress: Int?) {
            progressText.text = "Downloading..." + progress[0] + "%"
            super.onProgressUpdate(*progress)
        }

        override fun onPostExecute(result: Bitmap?) {
            if (result != null) {
                bmImage.setImageBitmap(result)
                progressText.text = ""
            } else {
                progressText.text = "Error"
            }
        }

    }

    companion object {
        var downloadImageTaskExecutor: Executor? = null
    }
}

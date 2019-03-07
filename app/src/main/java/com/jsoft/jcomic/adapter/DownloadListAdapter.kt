package com.jsoft.jcomic.adapter

import android.graphics.Typeface
import android.support.constraint.ConstraintLayout
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.jsoft.jcomic.DownloadListActivity
import com.jsoft.jcomic.R
import com.jsoft.jcomic.helper.DownloadItemDTO
import com.jsoft.jcomic.helper.Utils

class DownloadListAdapter(private var items: ArrayList<DownloadItemDTO>, private val activity: DownloadListActivity) : BaseAdapter() {

    override fun getCount(): Int {
        return items.size
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var myConvertView = convertView
        if (myConvertView == null) {
            val inflater = activity.layoutInflater
            myConvertView = inflater.inflate(R.layout.content_download_list, parent, false)
        }

        val downloadItem = myConvertView!!.findViewById<ConstraintLayout>(R.id.downloadItem)
        val textViewBookTitle = myConvertView.findViewById<TextView>(R.id.downloadBookTitle)
        val textViewEpisodeTitle = myConvertView.findViewById<TextView>(R.id.downloadEpisodeTitle)
        val imageView = myConvertView.findViewById<ImageView>(R.id.downloadBookImage)
        val textViewPageStatus = myConvertView.findViewById<TextView>(R.id.page_status)
        val textViewEpisodeSize = myConvertView.findViewById<TextView>(R.id.episode_size)

        val item = items[position]
        var jpgCount = 0
        val episodeFile = Utils.getEpisodeFile(item.book, item.episode)
        for (file in episodeFile.listFiles()) {
            if (file.isFile && file.name.endsWith(".jpg"))
                jpgCount += 1
        }

        textViewEpisodeSize.text = Utils.formatSize(Utils.calFolderSize(episodeFile))
        textViewEpisodeSize.setTypeface(null, Typeface.BOLD)
        textViewPageStatus.text = jpgCount.toString() + " / " + item.episode.imageUrl!!.size
        textViewBookTitle.text = item.book.bookTitle
        textViewBookTitle.setTypeface(null, Typeface.BOLD)
        textViewEpisodeTitle.text = item.episode.episodeTitle
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        if (item.book.bookImg != null) {
            imageView.setImageBitmap(item.book.bookImg)
        } else {
            imageView.setImageBitmap(null)
        }

        val btnPlay = myConvertView.findViewById<ImageButton>(R.id.btnPlayDownload)
        val btnDelete = myConvertView.findViewById<ImageButton>(R.id.btnDeleteDownload)

        btnPlay.setOnClickListener(object : ImgageClickListener(item) {
            override fun onClick(v: View) {
                activity.startReading(item.book, item.episodeIndex)
            }
        })
        btnDelete.setOnClickListener(object : ImgageClickListener(item) {
            override fun onClick(v: View) {
                activity.deleteEpisode(item)
            }
        })
        downloadItem.setOnClickListener(object : ImgageClickListener(item) {
            override fun onClick(v: View) {
                activity.viewBook(item.book)
            }
        })

        return myConvertView
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItem(position: Int): Any {
        return items[position]
    }

    internal open inner class ImgageClickListener(var item: DownloadItemDTO) : View.OnClickListener {

        override fun onClick(v: View) {}
    }
}

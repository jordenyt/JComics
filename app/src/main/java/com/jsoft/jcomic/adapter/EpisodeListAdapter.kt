package com.jsoft.jcomic.adapter

import android.app.AlertDialog
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import com.jsoft.jcomic.EpisodeListActivity
import com.jsoft.jcomic.R
import com.jsoft.jcomic.helper.BookDTO
import com.jsoft.jcomic.helper.BookmarkDb
import com.jsoft.jcomic.helper.Utils
import java.io.File

class EpisodeListAdapter(private val activity: EpisodeListActivity, private var book: BookDTO?) : BaseAdapter() {
    private val bookmarkDb: BookmarkDb = BookmarkDb(activity)

    override fun getCount(): Int {
        return book!!.episodes!!.size
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView

        if (convertView == null) {
            val inflater = activity.layoutInflater
            convertView = inflater.inflate(R.layout.content_episode_list, parent, false)
        }

        // object item based on the position
        val episode = book!!.episodes!![position]

        // get the TextView and then set the text (item name) and tag (item ID) values
        val textViewItem = convertView!!.findViewById<TextView>(R.id.episodeTitle)
        textViewItem.text = episode.episodeTitle
        textViewItem.tag = episode.episodeUrl
        val lastEpisode = bookmarkDb.getLastEpisode(book!!)


        var offlineAvailable = false
        val episodeFile = File(Utils.getEpisodeFile(book!!, episode), "episode.json")
        if (episodeFile.exists()) {
            offlineAvailable = true
        }
        val isOnline = Utils.isInternetAvailable

        if (episode.episodeTitle == lastEpisode) {
            textViewItem.setTextColor(Color.RED)
        } else if (offlineAvailable) {
            textViewItem.setTextColor(Color.WHITE)
        } else if (isOnline) {
            textViewItem.setTextColor(Color.LTGRAY)
        } else {
            textViewItem.setTextColor(Color.DKGRAY)
        }

        if (isOnline || offlineAvailable) {
            val episodeClickListener = EpisodeClickListener(position)
            convertView.setOnClickListener(episodeClickListener)
            convertView.setOnLongClickListener(episodeClickListener)
        } else {
            convertView.setOnClickListener(null)
            convertView.setOnLongClickListener(null)
        }

        return convertView
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun setBook(book: BookDTO) {
        this.book = book
    }

    override fun getItem(position: Int): Any {
        return this.book!!.episodes!![position]
    }

    internal inner class EpisodeClickListener// constructor
    (var position: Int) : View.OnClickListener, View.OnLongClickListener {

        override fun onClick(v: View) {
            activity.startReading(position)
        }

        override fun onLongClick(view: View): Boolean {
            if (Utils.isInternetAvailable) {
                AlertDialog.Builder(activity)
                        .setTitle("要下載嗎?")
                        .setMessage("下載後, 離線時可看")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes) { dialog, whichButton ->
                            activity.downloadEpisode(position)
                            Toast.makeText(activity, "下載中", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton(android.R.string.no, null).show()
                return true
            }
            return false
        }
    }
}

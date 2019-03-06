package com.jsoft.jcomic

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.ListView
import com.google.gson.Gson
import com.jsoft.jcomic.adapter.DownloadListAdapter
import com.jsoft.jcomic.helper.BookDTO
import com.jsoft.jcomic.helper.DownloadItemDTO
import com.jsoft.jcomic.helper.EpisodeDTO
import com.jsoft.jcomic.helper.Utils
import java.io.File
import java.io.FileReader
import java.util.*

class DownloadListActivity : AppCompatActivity() {

    private var listView: ListView? = null
    private var adapter: DownloadListAdapter? = null

    private val downloadItemList: ArrayList<DownloadItemDTO>
        get() {
            val items = ArrayList<DownloadItemDTO>()
            val gson = Gson()
            val rootFolder = Utils.rootFile
            if (rootFolder.isDirectory) {
                val rootFolderList = rootFolder.listFiles()
                for (rootFile in rootFolderList) {
                    if (rootFile.isDirectory) {
                        val bookFile = File(rootFile, "book.json")
                        if (bookFile.exists()) {
                            try {
                                val book = gson.fromJson<BookDTO>(FileReader(bookFile.absolutePath), BookDTO::class.java)
                                val bookImgFile = File(rootFile, "book.jpg")
                                if (bookImgFile.exists()) {
                                    book.bookImg = Utils.imageFromFile(bookImgFile)
                                }

                                val bookFolderList = rootFile.listFiles()
                                for (bookFolderFile in bookFolderList) {
                                    if (bookFolderFile.isDirectory) {
                                        val episodeFile = File(bookFolderFile, "episode.json")
                                        val episode = gson.fromJson<EpisodeDTO>(FileReader(episodeFile.absolutePath), EpisodeDTO::class.java)
                                        val item = DownloadItemDTO(book, episode)
                                        items.add(item)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("jComics", "Exception caught in getDownloadItemList", e)
                            }

                        }
                    }
                }
            }
            return items
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download_list)
        listView = this.findViewById(R.id.download_list_view)
    }

    public override fun onResume() {
        super.onResume()
        listView!!.setBackgroundColor(Color.BLACK)
        if (adapter == null) {
            adapter = DownloadListAdapter(downloadItemList, this)
        }
        listView!!.adapter = adapter
        adapter = DownloadListAdapter(downloadItemList, this)
    }

    fun startReading(book: BookDTO, position: Int) {
        val intent = Intent(this, FullscreenActivity::class.java)
        intent.putExtra("position", position)
        val b = Bundle()
        b.putSerializable("book", book.serializable)
        intent.putExtras(b)
        this.startActivity(intent)
    }

    fun viewBook(book: BookDTO) {
        val i = Intent(this, EpisodeListActivity::class.java)
        i.putExtra("bookUrl", book.bookUrl)
        startActivity(i)
    }

    fun deleteEpisode(item: DownloadItemDTO) {
        var dir = Utils.getEpisodeFile(item.book, item.episode)
        if (dir.isDirectory) {
            Utils.deleteRecursive(dir)
        }
        dir = Utils.getBookFile(item.book)
        var episodeCount = 0
        for (f in dir.listFiles()) {
            if (f.isDirectory) {
                episodeCount += 1
            }
        }
        if (episodeCount == 0) {
            Utils.deleteRecursive(dir)
        }
        adapter!!.items!!.clear()
        adapter!!.notifyDataSetChanged()
        adapter!!.items!!.addAll(downloadItemList)
        adapter!!.notifyDataSetChanged()
    }
}

package com.jsoft.jcomic

import android.content.Intent
import android.graphics.Bitmap
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import com.google.gson.Gson
import com.jsoft.jcomic.adapter.EpisodeListAdapter
import com.jsoft.jcomic.helper.*
import com.jsoft.jcomic.praser.BookParser
import com.jsoft.jcomic.praser.BookParserListener
import java.io.File
import java.io.FileReader


class EpisodeListActivity : AppCompatActivity(), BookParserListener {

    private var gridView: GridView? = null
    private var utils: Utils? = null
    private var book = BookDTO("")
    private var bookmarkDb: BookmarkDb? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        utils = Utils(this)

        val i = intent
        //Log.e("jComics", "get Intent!");
        val bookUrl: String
        val data = i.data
        bookUrl = data?.toString() ?: i.getStringExtra("bookUrl")
        bookmarkDb = BookmarkDb(this)
        loadBook(bookUrl)
        if (Utils.isInternetAvailable) {
            BookParser.parseBook(book, this)
        }

    }

    private fun loadBook(bookUrl: String) {
        try {
            val bookFile = File(Utils.getBookFile(BookDTO(bookUrl)), "book.json")
            val dbBook = bookmarkDb!!.getBook(bookUrl)
            if (bookFile.exists()) {
                val gson = Gson()
                val savedBook = gson.fromJson(FileReader(bookFile.absolutePath), BookDTO::class.java)

                val bookImgFile = File(Utils.getBookFile(BookDTO(bookUrl)), "book.jpg")
                if (bookImgFile.exists()) {
                    savedBook.bookImg = Utils.imageFromFile(bookImgFile)
                }
                onBookFetched(savedBook)
            } else if (dbBook != null) {
                onBookFetched(dbBook)
            } else {
                val newBook = BookDTO(bookUrl)
                newBook.bookTitle = "Loading..."
                onBookFetched(newBook)
            }
        } catch (e: Exception) {
            Log.e("jComics", "Error caught in reading saved book.", e)
        }

    }

    public override fun onResume() {
        super.onResume()
        if (gridView != null) {
            val adapter = EpisodeListAdapter(this, book)
            gridView!!.adapter = adapter
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.episode_list_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (book != null && book.episodes.isNotEmpty()) {
            if (!bookmarkDb!!.bookIsBookmarked(book)) {
                menu.getItem(1).isVisible = false
                menu.getItem(0).isVisible = true
            } else {
                menu.getItem(0).isVisible = false
                menu.getItem(1).isVisible = true
            }
        } else {
            menu.getItem(0).isVisible = false
            menu.getItem(1).isVisible = false
            menu.getItem(2).isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        when (item.itemId) {
            R.id.menu_item_add_bookmark -> {
                //Log.e("jComics", "Add Bookmark");
                if (!bookmarkDb!!.bookInDb(book)) {
                    bookmarkDb!!.insertBookIntoDb(book)
                }
                bookmarkDb!!.updateIsBookmark(book, "Y")
                invalidateOptionsMenu()
                return true
            }
            R.id.menu_item_delete_bookmark -> {
                //Log.e("jComics", "Delete Bookmark");
                if (!bookmarkDb!!.bookInDb(book)) {
                    bookmarkDb!!.insertBookIntoDb(book)
                }
                bookmarkDb!!.updateIsBookmark(book, "N")
                invalidateOptionsMenu()
                return true
            }
            R.id.menu_play_book -> {
                if (!bookmarkDb!!.bookInDb(book)) {
                    bookmarkDb!!.insertBookIntoDb(book)
                }
                val lastEpisode = bookmarkDb!!.getLastEpisode(book)
                for (i in 0 until book.episodes.size) {
                    if (lastEpisode == book.episodes[i].episodeTitle) {
                        startReading(i)
                        return true
                    }
                }
                startReading(book.episodes.size - 1)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }


    override fun onBookFetched(book: BookDTO) {
        this.book = book
        title = book.bookTitle
        setContentView(R.layout.activity_episode_list)
        gridView = findViewById(R.id.episode_list_view)
        initilizeGridLayout()

        if (gridView!!.adapter == null) {
            gridView!!.adapter = EpisodeListAdapter(this, book)
        } else {
            val adapter = gridView!!.adapter as EpisodeListAdapter
            adapter.setBook(book)
            adapter.notifyDataSetChanged()
        }

        val textView = findViewById<TextView>(R.id.book_description)
        textView.text = book.bookSynopsis
        val imageView = findViewById<ImageView>(R.id.book_image)
        if (Utils.isInternetAvailable && book.bookImg == null && book.bookImgUrl != null) {
            DownloadImageTask(book, imageView).execute(book.bookImgUrl)
        } else if (book.bookImg != null) {
            imageView.setImageBitmap(book.bookImg)
        }
        invalidateOptionsMenu()
    }

    private inner class DownloadImageTask internal constructor(internal var book: BookDTO, internal var imageView: ImageView) : AsyncTask<String, Void, Bitmap>() {

        override fun doInBackground(vararg urls: String): Bitmap? {
            return Utils.downloadImage(urls[0], null)
        }

        override fun onPostExecute(result: Bitmap) {
            book.bookImg = result
            imageView.setImageBitmap(result)
        }
    }

    private fun initilizeGridLayout() {
        val r = resources
        val padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                AppConstant.GRID_PADDING.toFloat(), r.displayMetrics)

        val columnWidth = ((utils!!.screenWidth - (AppConstant.NUM_OF_COLUMNS + 1) * padding) / AppConstant.NUM_OF_COLUMNS).toInt()

        gridView!!.numColumns = AppConstant.NUM_OF_COLUMNS
        gridView!!.columnWidth = columnWidth
        gridView!!.stretchMode = GridView.NO_STRETCH
        gridView!!.setPadding(padding.toInt(), padding.toInt(), padding.toInt(),
                padding.toInt())
        gridView!!.horizontalSpacing = padding.toInt()
        gridView!!.verticalSpacing = padding.toInt()
    }

    fun startReading(position: Int) {
        val intent = Intent(this, FullscreenActivity::class.java)
        intent.putExtra("position", position)
        val b = Bundle()
        b.putSerializable("book", book.serializable)
        intent.putExtras(b)
        this.startActivityForResult(intent, 0)
    }

    fun downloadEpisode(position: Int) {
        Downloader(book, this).downloadEpisode(position)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

    }
}

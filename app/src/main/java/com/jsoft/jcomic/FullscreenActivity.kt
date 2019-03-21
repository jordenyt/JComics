package com.jsoft.jcomic

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import com.google.gson.Gson
import com.jsoft.jcomic.adapter.FullScreenImageAdapter
import com.jsoft.jcomic.helper.*
import com.jsoft.jcomic.praser.*
import java.io.File
import java.io.FileReader

class FullscreenActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener, EpisodeParserListener {
    private var pageTurn: Int = 0
    private var gotoLastPage: Boolean = false
    private var book: BookDTO? = null
    private var currEpisode: Int = 0
    private var bookmarkDb: BookmarkDb? = null
    private var pager: ComicsViewPager? = null
    private var seekBarLayout: LinearLayout? = null
    private var seekBar: SeekBar? = null
    private var hideSeekBarTask: Runnable? = null
    private var isSeeking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bookmarkDb = BookmarkDb(this)
        val i = intent
        currEpisode = i.getIntExtra("position", 0)
        val b = this.intent.extras
        book = if (b != null)
            b.getSerializable("book") as BookDTO
        else {
            BookDTO("")
        }
        //Log.d("jComic", "EpisodeUrl: " + episode.getEpisodeUrl());
        val episode = book!!.episodes[currEpisode]

        try {
            val episodeFile = File(Utils.getEpisodeFile(book!!, episode), "episode.json")
            if (episodeFile.exists()) {
                val gson = Gson()
                val savedEpisode = gson.fromJson(FileReader(episodeFile.absolutePath), EpisodeDTO::class.java)
                onEpisodeFetched(savedEpisode)
            } else if (Utils.isInternetAvailable) {
                EpisodeParser.parseEpisode(episode, this)
            } else {
                onEpisodeFetched(episode)
            }
        } catch (e: Exception) {
            Log.e("jComics", "Error caught in reading saved episode.", e)
        }

    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    // Shows the system bars by removing all the flags
// except for the ones that make the content appear under the system bars.
    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    override fun onEpisodeFetched(episode: EpisodeDTO) {
        setContentView(R.layout.activity_fullscreen)
        this.seekBarLayout = findViewById(R.id.seekbar_layout)
        this.seekBarLayout!!.visibility = View.GONE
        seekBar = findViewById(R.id.seekBar)
        seekBar!!.max = episode.pageCount - 1
        seekBar!!.setOnSeekBarChangeListener(this)
        pager = findViewById(R.id.pager)
        pager!!.setActivity(this)
        if (pager!!.adapter == null) {
            val adapter = FullScreenImageAdapter(this@FullscreenActivity, pager!!, episode, book!!)
            pager!!.adapter = adapter
        } else {
            (pager!!.adapter as FullScreenImageAdapter).episode = book!!.episodes[currEpisode]
        }
        var currentPage = 0
        val lastEpisode = bookmarkDb!!.getLastEpisode(book!!)
        if (book!!.episodes[currEpisode].episodeTitle == lastEpisode) {
            currentPage = bookmarkDb!!.getLastEpisodePage(book!!)
        }
        if (pageTurn == -1 && gotoLastPage) {
            currentPage = episode.pageCount - 1
        }
        pager!!.offscreenPageLimit = 2
        if (!bookmarkDb!!.bookInDb(book!!)) {
            bookmarkDb!!.insertBookIntoDb(book!!)
        }
        switchPageNum(currentPage)
    }

    fun showPageBar() {
        this.seekBarLayout!!.visibility = View.VISIBLE
        val view = this.seekBarLayout
        view!!.removeCallbacks(hideSeekBarTask)
        hideSeekBarTask = Runnable { view.visibility = View.GONE }
        view.postDelayed(hideSeekBarTask, 5000)
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (!isSeeking) {
            switchPageNum(progress)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        val view = this.seekBarLayout
        view!!.removeCallbacks(hideSeekBarTask)
        isSeeking = true
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        isSeeking = false
        switchPageNum(seekBar.progress)
        val view = this.seekBarLayout
        hideSeekBarTask = Runnable { view!!.visibility = View.GONE }
        view!!.postDelayed(hideSeekBarTask, 5000)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            pager!!.turnNext()
            return true
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            pager!!.turnPrev()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    @JvmOverloads
    fun episodeSwitch(pageTurn: Int, gotoLastPage: Boolean = true) {
        currEpisode -= pageTurn
        this.pageTurn = pageTurn
        this.gotoLastPage = gotoLastPage
        if (currEpisode >= 0 && currEpisode < book!!.episodes.size) {
            if (book!!.episodes[currEpisode].pageCount > 0) {
                onEpisodeFetched(book!!.episodes[currEpisode])
            } else {
                if (book!!.episodes[currEpisode].episodeUrl!!.contains("comicbus")) {
                    ComicVIPEpisodeParser(book!!.episodes[currEpisode], this)
                } else if (book!!.episodes[currEpisode].episodeUrl!!.contains("cartoonmad")) {
                    CartoonMadEpisodeParser(book!!.episodes[currEpisode], this)
                } else if (book!!.episodes[currEpisode].episodeUrl!!.contains("dm5.com")) {
                    DM5EpisodeParser(book!!.episodes[currEpisode], this)
                }
            }
        } else {
            this.finish()
        }
    }

    fun switchPageNum(pageNum: Int) {
        pager!!.currentItem = pageNum
        seekBar!!.progress = pageNum
        bookmarkDb!!.updateLastRead(book!!, currEpisode, pageNum)
    }

    fun goPrev(view: View) {
        episodeSwitch(-1, false)
    }

    fun goNext(view: View) {
        episodeSwitch(1)
    }

}

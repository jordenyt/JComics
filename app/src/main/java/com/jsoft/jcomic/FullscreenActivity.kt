package com.jsoft.jcomic

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.SeekBar
import com.google.gson.Gson
import com.jsoft.jcomic.adapter.FullScreenImageAdapter
import com.jsoft.jcomic.helper.BookDTO
import com.jsoft.jcomic.helper.BookmarkDb
import com.jsoft.jcomic.helper.EpisodeDTO
import com.jsoft.jcomic.helper.Utils
import com.jsoft.jcomic.praser.*
import kotlinx.android.synthetic.main.activity_fullscreen.*
import java.io.File
import java.io.FileReader

class FullscreenActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener, EpisodeParserListener {
    private var pageTurn: Int = 0
    private var gotoLastPage: Boolean = false
    private var book: BookDTO = BookDTO("")
    private var currEpisode: Int = 0
    private var bookmarkDb: BookmarkDb = BookmarkDb(this)
    private var hideSeekBarTask: Runnable? = null
    private var isSeeking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //bookmarkDb = BookmarkDb(this)
        val i = intent
        currEpisode = i.getIntExtra("position", 0)
        val b = this.intent.extras
        if (b != null) book = b.getSerializable("book") as BookDTO
        
        val episode = book.episodes[currEpisode]

        try {
            val episodeFile = File(Utils.getEpisodeFile(book, episode), "episode.json")
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

    override fun onEpisodeFetched(episode: EpisodeDTO) {
        setContentView(R.layout.activity_fullscreen)
        seekBarLayout.visibility = View.GONE
        seekBar.max = episode.pageCount - 1
        seekBar.setOnSeekBarChangeListener(this)
        imageViewPager.setActivity(this)
        if (imageViewPager.adapter == null) {
            val adapter = FullScreenImageAdapter(this@FullscreenActivity, imageViewPager, episode, book)
            imageViewPager.adapter = adapter
        } else {
            (imageViewPager.adapter as FullScreenImageAdapter).episode = book.episodes[currEpisode]
        }
        var currentPage = 0
        val lastEpisode = bookmarkDb.getLastEpisode(book)
        if (book.episodes[currEpisode].episodeTitle == lastEpisode) {
            currentPage = bookmarkDb.getLastEpisodePage(book)
        }
        if (pageTurn == -1 && gotoLastPage) {
            currentPage = episode.pageCount - 1
        }
        imageViewPager.offscreenPageLimit = 2
        if (!bookmarkDb.bookInDb(book)) {
            bookmarkDb.insertBookIntoDb(book)
        }
        switchPageNum(currentPage)
    }

    fun showPageBar() {
        seekBarLayout.visibility = View.VISIBLE
        seekBarLayout.removeCallbacks(hideSeekBarTask)
        hideSeekBarTask = Runnable { seekBarLayout.visibility = View.GONE }
        seekBarLayout.postDelayed(hideSeekBarTask, 5000)
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (!isSeeking) {
            switchPageNum(progress)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        seekBarLayout.removeCallbacks(hideSeekBarTask)
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
            imageViewPager.turnNext()
            return true
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            imageViewPager.turnPrev()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    @JvmOverloads
    fun episodeSwitch(pageTurn: Int, gotoLastPage: Boolean = true) {
        currEpisode -= pageTurn
        this.pageTurn = pageTurn
        this.gotoLastPage = gotoLastPage
        if (currEpisode >= 0 && currEpisode < book.episodes.size) {
            if (book.episodes[currEpisode].pageCount > 0) {
                onEpisodeFetched(book.episodes[currEpisode])
            } else {
                if (book.episodes[currEpisode].episodeUrl.contains("comicbus")) {
                    ComicVIPEpisodeParser(book.episodes[currEpisode], this)
                } else if (book.episodes[currEpisode].episodeUrl.contains("cartoonmad")) {
                    CartoonMadEpisodeParser(book.episodes[currEpisode], this)
                } else if (book.episodes[currEpisode].episodeUrl.contains("dm5.com")) {
                    DM5EpisodeParser(book.episodes[currEpisode], this)
                }
            }
        } else {
            this.finish()
        }
    }

    fun switchPageNum(pageNum: Int) {
        imageViewPager.currentItem = pageNum
        seekBar.progress = pageNum
        bookmarkDb.updateLastRead(book, currEpisode, pageNum)
    }

    fun goPrev(view: View) {
        episodeSwitch(-1, false)
    }

    fun goNext(view: View) {
        episodeSwitch(1)
    }

}

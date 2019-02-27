package com.jsoft.jcomic;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import com.google.gson.Gson;
import com.jsoft.jcomic.adapter.FullScreenImageAdapter;
import com.jsoft.jcomic.helper.BookDTO;
import com.jsoft.jcomic.helper.BookmarkDb;
import com.jsoft.jcomic.helper.ComicsViewPager;
import com.jsoft.jcomic.helper.EpisodeDTO;
import com.jsoft.jcomic.helper.Utils;
import com.jsoft.jcomic.praser.CartoonMadEpisodeParser;
import com.jsoft.jcomic.praser.ComicVIPEpisodeParser;
import com.jsoft.jcomic.praser.DM5EpisodeParser;
import com.jsoft.jcomic.praser.EpisodeParser;
import com.jsoft.jcomic.praser.EpisodeParserListener;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

public class FullscreenActivity extends AppCompatActivity implements
        SeekBar.OnSeekBarChangeListener, EpisodeParserListener {
    private int pageTurn;
    private boolean gotoLastPage;
    private BookDTO book;
    private int currEpisode;
    private BookmarkDb bookmarkDb;
    private ComicsViewPager pager;
    private LinearLayout seekBarLayout;
    private SeekBar seekBar;
    private Runnable hideSeekBarTask;
    private boolean isSeeking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bookmarkDb = new BookmarkDb(this);
        Intent i = getIntent();
        currEpisode = i.getIntExtra("position", 0);
        Bundle b = this.getIntent().getExtras();
        if(b!=null)
            book = (BookDTO) b.getSerializable("book");
        else {
            book = new BookDTO("");
            book.setEpisodes(new ArrayList<EpisodeDTO>());
        }
        //Log.d("jComic", "EpisodeUrl: " + episode.getEpisodeUrl());
        EpisodeDTO episode = book.getEpisodes().get(currEpisode);
        if (Utils.isInternetAvailable()) {
            EpisodeParser.parseEpisode(episode, this);
        } else {
            try {
                File episodeFile = new File(Utils.getEpisodeFile(book, episode), "episode.json");
                if (episodeFile.exists()) {
                    Gson gson = new Gson();
                    EpisodeDTO savedEpisode = gson.fromJson(new FileReader(episodeFile.getAbsolutePath()), EpisodeDTO.class);
                    onEpisodeFetched(savedEpisode);
                } else {
                    onEpisodeFetched(episode);
                }
            } catch (Exception e) {
                Log.e("jComics", "Error caught in reading saved episode.", e);
            }
        }
    }

    public void onEpisodeFetched(EpisodeDTO episode) {
        setContentView(R.layout.activity_fullscreen);
        this.seekBarLayout = (LinearLayout) findViewById(R.id.seekbar_layout);
        this.seekBarLayout.setVisibility(View.GONE);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setMax(episode.getPageCount() - 1);
        seekBar.setOnSeekBarChangeListener(this);
        pager = (ComicsViewPager) findViewById(R.id.pager);
        pager.setActivity(this);
        if (pager.getAdapter() == null) {
            FullScreenImageAdapter adapter = new FullScreenImageAdapter(FullscreenActivity.this, pager, episode, book);
            pager.setAdapter(adapter);
        } else {
            ((FullScreenImageAdapter)pager.getAdapter()).setEpisode(book.getEpisodes().get(currEpisode));
        }
        int currentPage = 0;
        String lastEpisode = bookmarkDb.getLastEpisode(book);
        if (book.getEpisodes().get(currEpisode).getEpisodeTitle().equals(lastEpisode)) {
            currentPage = bookmarkDb.getLastEpisodePage(book);
        }
        if (pageTurn == -1 && gotoLastPage) {
            currentPage = episode.getPageCount()-1;
        }
        pager.setOffscreenPageLimit(2);
        if (!bookmarkDb.bookInDb(book)) {
            bookmarkDb.insertBookIntoDb(book);
        }
        switchPageNum(currentPage);
    }

    public void showPageBar() {
        this.seekBarLayout.setVisibility(View.VISIBLE);
        final View view = this.seekBarLayout;
        view.removeCallbacks(hideSeekBarTask);
        hideSeekBarTask = new Runnable() {
            public void run() {
                view.setVisibility(View.GONE);
            }
        };
        view.postDelayed(hideSeekBarTask, 5000);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!isSeeking) {
            switchPageNum(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        final View view = this.seekBarLayout;
        view.removeCallbacks(hideSeekBarTask);
        isSeeking = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        isSeeking = false;
        switchPageNum(seekBar.getProgress());
        final View view = this.seekBarLayout;
        hideSeekBarTask = new Runnable() {
            public void run() {
                view.setVisibility(View.GONE);
            }
        };
        view.postDelayed(hideSeekBarTask, 5000);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)){
            pager.turnNext();
            return true;
        } else if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)){
            pager.turnPrev();
            return true;
        }
        return false;
    }

    public void episodeSwitch(int pageTurn) {
        episodeSwitch(pageTurn, true);
    }

    public void episodeSwitch(int pageTurn, boolean gotoLastPage) {
        currEpisode = currEpisode - pageTurn;
        this.pageTurn = pageTurn;
        this.gotoLastPage = gotoLastPage;
        if (currEpisode >=0 && currEpisode < book.getEpisodes().size()) {
            if (book.getEpisodes().get(currEpisode).getPageCount() > 0) {
                onEpisodeFetched(book.getEpisodes().get(currEpisode));
            } else {
                if (book.getEpisodes().get(currEpisode).getEpisodeUrl().contains("comicbus")) {
                    new ComicVIPEpisodeParser(book.getEpisodes().get(currEpisode), this);
                } else if (book.getEpisodes().get(currEpisode).getEpisodeUrl().contains("cartoonmad")) {
                    new CartoonMadEpisodeParser(book.getEpisodes().get(currEpisode), this);
                } else if (book.getEpisodes().get(currEpisode).getEpisodeUrl().contains("dm5.com")) {
                    new DM5EpisodeParser(book.getEpisodes().get(currEpisode), this);
                }
            }
        } else {
            this.finish();
        }
    }

    public void switchPageNum(int pageNum) {
        pager.setCurrentItem(pageNum);
        seekBar.setProgress(pageNum);
        bookmarkDb.updateLastRead(book, currEpisode, pageNum);
    }

    public void goPrev(View view) {
        episodeSwitch(-1, false);
    }

    public void goNext(View view) {
        episodeSwitch(1);
    }

}

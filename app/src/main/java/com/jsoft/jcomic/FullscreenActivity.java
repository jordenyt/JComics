package com.jsoft.jcomic;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import com.jsoft.jcomic.adapter.FullScreenImageAdapter;
import com.jsoft.jcomic.helper.BookDTO;
import com.jsoft.jcomic.helper.BookmarkDb;
import com.jsoft.jcomic.helper.ComicsViewPager;
import com.jsoft.jcomic.helper.EpisodeDTO;
import com.jsoft.jcomic.praser.CartoonMadEpisodeParser;
import com.jsoft.jcomic.praser.ComicVIPEpisodeParser;
import com.jsoft.jcomic.praser.DM5EpisodeParser;

import java.util.ArrayList;

public class FullscreenActivity extends AppCompatActivity implements
        SeekBar.OnSeekBarChangeListener {
    private int pageTurn;
    private BookDTO book;
    private int currEpisode;
    private BookmarkDb bookmarkDb;
    private ComicsViewPager pager;
    private LinearLayout seekBarLayout;
    private SeekBar seekBar;
    private Runnable hideSeekBarTask;

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
        EpisodeDTO episode = book.getEpisodes().get(currEpisode);
        //Log.d("jComic", "EpisodeUrl: " + episode.getEpisodeUrl());
        if (episode.getEpisodeUrl().contains("comicbus")) {
            new ComicVIPEpisodeParser(episode, this);
        } else if (episode.getEpisodeUrl().contains("cartoonmad")) {
            new CartoonMadEpisodeParser(episode, this);
        } else if (episode.getEpisodeUrl().contains("dm5.com")) {
            new DM5EpisodeParser(episode, this);
        }
        //new CartoonMadEpisodeParser(episode, this);
    }

    public void onEpisodeFetched(EpisodeDTO episode) {
        setContentView(R.layout.activity_fullscreen);
        this.seekBarLayout = (LinearLayout) findViewById(R.id.seekbar_layout);
        this.seekBarLayout.setVisibility(View.GONE);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setMax(episode.getPageCount() - 1);
        seekBar.setOnSeekBarChangeListener(this);
        ComicsViewPager viewPager = (ComicsViewPager) findViewById(R.id.pager);
        viewPager.setActivity(this);
        this.pager = viewPager;
        if (viewPager.getAdapter() == null) {
            FullScreenImageAdapter adapter = new FullScreenImageAdapter(FullscreenActivity.this, viewPager, episode);
            viewPager.setAdapter(adapter);
        } else {
            ((FullScreenImageAdapter)viewPager.getAdapter()).setEpisode(book.getEpisodes().get(currEpisode));
        }
        int currentPage = 0;
        String lastEpisode = bookmarkDb.getLastEpisode(book);
        if (book.getEpisodes().get(currEpisode).getEpisodeTitle().equals(lastEpisode)) {
            currentPage = bookmarkDb.getLastEpisodePage(book);
        }
        if (pageTurn == -1) {
            currentPage = episode.getPageCount()-1;
        }
        viewPager.setCurrentItem(currentPage);
        viewPager.setOffscreenPageLimit(2);
        if (!bookmarkDb.bookInDb(book)) {
            bookmarkDb.insertBookIntoDb(book);
        }
        updateLastRead(currentPage);
        //bookmarkDb.updateLastRead(book, currEpisode, currentPage);
    }

    public void showPageBar() {
        if (this.seekBarLayout.getVisibility() == View.GONE) {
            this.seekBarLayout.setVisibility(View.VISIBLE);
            final View view = this.seekBarLayout;
            hideSeekBarTask = new Runnable() {
                public void run() {
                    view.setVisibility(View.GONE);
                }
            };
            view.postDelayed(hideSeekBarTask, 5000);
        } else {
            this.seekBarLayout.removeCallbacks(hideSeekBarTask);
            this.seekBarLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        final View view = this.seekBarLayout;
        view.removeCallbacks(hideSeekBarTask);
        hideSeekBarTask = new Runnable() {
            public void run() {
                view.setVisibility(View.GONE);
            }
        };
        view.postDelayed(hideSeekBarTask, 5000);

        this.pager.setCurrentItem(progress);
        updateLastRead(progress);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    public void episodeSwitch(int pageTurn) {
        currEpisode = currEpisode - pageTurn;
        this.pageTurn = pageTurn;
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

    public void updateLastRead(int pageNum) {
        updateSeekBar(pageNum);
        bookmarkDb.updateLastRead(book, currEpisode, pageNum);
    }

    public void updateSeekBar(int pageNum) {
        seekBar.setProgress(pageNum);
    }

    public void goPrev(View view) {
        episodeSwitch(-1);
    }

    public void goNext(View view) {
        episodeSwitch(1);
    }

}

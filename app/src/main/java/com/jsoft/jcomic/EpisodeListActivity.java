package com.jsoft.jcomic;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.jsoft.jcomic.adapter.EpisodeListAdapter;
import com.jsoft.jcomic.helper.AppConstant;
import com.jsoft.jcomic.helper.BookDTO;
import com.jsoft.jcomic.helper.BookmarkDb;
import com.jsoft.jcomic.helper.Downloader;
import com.jsoft.jcomic.helper.EpisodeDTO;
import com.jsoft.jcomic.helper.Utils;
import com.jsoft.jcomic.praser.BookParser;
import com.jsoft.jcomic.praser.BookParserListener;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;


public class EpisodeListActivity extends AppCompatActivity implements BookParserListener {

    private GridView gridView;
    private Utils utils;
    private BookDTO book;
    private BookmarkDb bookmarkDb;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        utils = new Utils(this);

        Intent i = getIntent();
        //Log.e("jComics", "get Intent!");
        String bookUrl;
        Uri data = i.getData();
        if (data != null) {
            bookUrl = data.toString();
        } else {
            bookUrl = i.getStringExtra("bookUrl");
        }
        bookmarkDb = new BookmarkDb(this);
        loadBook(bookUrl);
        if (Utils.isInternetAvailable()) {
            BookParser.parseBook(book, this);
        }

    }

    public void loadBook(String bookUrl) {
        try {
            File bookFile = new File(Utils.getBookFile(new BookDTO(bookUrl)), "book.json");
            BookDTO dbBook = bookmarkDb.getBook(bookUrl);
            if (bookFile.exists()) {
                Gson gson = new Gson();
                BookDTO savedBook = gson.fromJson(new FileReader(bookFile.getAbsolutePath()), BookDTO.class);

                File bookImgFile = new File(Utils.getBookFile(new BookDTO(bookUrl)),"book.jpg");
                if (bookImgFile.exists()) {
                    savedBook.setBookImg(Utils.imageFromFile(bookImgFile));
                }
                onBookFetched(savedBook);
            } else if (dbBook != null){
                onBookFetched(dbBook);
            } else {
                BookDTO newBook = new BookDTO(bookUrl);
                newBook.setBookTitle("Loading...");
                onBookFetched(newBook);
            }
        } catch (Exception e) {
            Log.e("jComics", "Error caught in reading saved book.", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (gridView != null) {
            EpisodeListAdapter adapter = new EpisodeListAdapter(this, book);
            gridView.setAdapter(adapter);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.episode_list_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (book != null && book.getEpisodes().size() > 0) {
            if (!bookmarkDb.bookIsBookmarked(book)) {
                menu.getItem(1).setVisible(false);
                menu.getItem(0).setVisible(true);
            } else {
                menu.getItem(0).setVisible(false);
                menu.getItem(1).setVisible(true);
            }
        } else {
            menu.getItem(0).setVisible(false);
            menu.getItem(1).setVisible(false);
            menu.getItem(2).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_item_add_bookmark:
                //Log.e("jComics", "Add Bookmark");
                if (!bookmarkDb.bookInDb(book)) {
                    bookmarkDb.insertBookIntoDb(book);
                }
                bookmarkDb.updateIsBookmark(book, "Y");
                invalidateOptionsMenu();
                return true;
            case R.id.menu_item_delete_bookmark:
                //Log.e("jComics", "Delete Bookmark");
                if (!bookmarkDb.bookInDb(book)) {
                    bookmarkDb.insertBookIntoDb(book);
                }
                bookmarkDb.updateIsBookmark(book, "N");
                invalidateOptionsMenu();
                return true;
            case R.id.menu_play_book:
                if (!bookmarkDb.bookInDb(book)) {
                    bookmarkDb.insertBookIntoDb(book);
                }
                String lastEpisode = bookmarkDb.getLastEpisode(book);
                if (lastEpisode != null) {
                    for (int i = 0; i < book.getEpisodes().size(); i++) {
                        if (lastEpisode.equals(book.getEpisodes().get(i).getEpisodeTitle())) {
                            startReading(i);
                            return true;
                        }
                    }
                }
                startReading(book.getEpisodes().size() - 1);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public void onBookFetched(BookDTO book) {
        this.book = book;
        setTitle(book.getBookTitle());
        setContentView(R.layout.activity_episode_list);
        gridView = findViewById(R.id.episode_list_view);
        InitilizeGridLayout();

        if (gridView.getAdapter() == null) {
            gridView.setAdapter(new EpisodeListAdapter(this, book));
        } else {
            EpisodeListAdapter adapter = (EpisodeListAdapter) gridView.getAdapter();
            adapter.setBook(book);
            adapter.notifyDataSetChanged();
        }

        TextView textView = findViewById(R.id.book_description);
        textView.setText(book.getBookSynopsis());
        ImageView imageView = findViewById(R.id.book_image);
        if (Utils.isInternetAvailable() && book.getBookImg() == null && book.getBookImgUrl() != null) {
            (new DownloadImageTask(book, imageView)).execute(book.getBookImgUrl());
        } else if (book.getBookImg() != null) {
            imageView.setImageBitmap(book.getBookImg());
        }
        invalidateOptionsMenu();
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        BookDTO book;
        ImageView imageView;

        DownloadImageTask(BookDTO book, ImageView imageView) {
            this.book = book;
            this.imageView = imageView;
        }

        protected Bitmap doInBackground(String... urls) {
            return Utils.downloadImage(urls[0], null);
        }

        protected void onPostExecute(Bitmap result) {
            book.setBookImg(result);
            imageView.setImageBitmap(result);
        }
    }

    private void InitilizeGridLayout() {
        Resources r = getResources();
        float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                AppConstant.GRID_PADDING, r.getDisplayMetrics());

        int columnWidth = (int) ((utils.getScreenWidth() - ((AppConstant.NUM_OF_COLUMNS + 1) * padding)) / AppConstant.NUM_OF_COLUMNS);

        gridView.setNumColumns(AppConstant.NUM_OF_COLUMNS);
        gridView.setColumnWidth(columnWidth);
        gridView.setStretchMode(GridView.NO_STRETCH);
        gridView.setPadding((int) padding, (int) padding, (int) padding,
                (int) padding);
        gridView.setHorizontalSpacing((int) padding);
        gridView.setVerticalSpacing((int) padding);
    }

    public void startReading(int position) {
        Intent intent = new Intent(this, FullscreenActivity.class);
        intent.putExtra("position", position);
        Bundle b = new Bundle();
        b.putSerializable("book", book.getSerializable());
        intent.putExtras(b);
        this.startActivityForResult(intent, 0);
    }

    public void downloadEpisode(int position) {
        new Downloader(book, this).downloadEpisode(position);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    }
}

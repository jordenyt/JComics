package com.jsoft.jcomic;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import java.io.InputStream;
import java.util.ArrayList;


public class EpisodeListActivity extends AppCompatActivity implements BookParserListener {

    private GridView gridView;
    private TextView textView;
    private ImageView imageView;
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
        if (Utils.isInternetAvailable()) {
            BookParser.parseBook(bookUrl, this);
        } else {
            try {
                File bookFile = new File(Environment.getExternalStorageDirectory().toString() + "/jComics/" + Utils.getHashCode(bookUrl) + "/book.json");
                if (bookFile.exists()) {
                    Gson gson = new Gson();
                    BookDTO savedBook = gson.fromJson(new FileReader(bookFile.getAbsolutePath()), BookDTO.class);
                    onBookFetched(savedBook);
                } else {
                    BookDTO newBook = new BookDTO(bookUrl);
                    newBook.setEpisodes(new ArrayList<EpisodeDTO>());
                    newBook.setBookTitle("No Internet");
                    onBookFetched(newBook);
                }
            } catch (Exception e) {
                Log.e("jComics", "Error caught in reading saved book.", e);
            }
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
        gridView = (GridView) findViewById(R.id.episode_list_view);
        InitilizeGridLayout();
        EpisodeListAdapter adapter = new EpisodeListAdapter(this, book);
        gridView.setAdapter(adapter);
        textView = (TextView) findViewById(R.id.book_description);
        textView.setText(book.getBookSynopsis());
        imageView = (ImageView) findViewById(R.id.book_image);
        executeAsyncTask(new DownloadImageTask(), book.getBookImgUrl());
        invalidateOptionsMenu();
    }

    public static <T> void executeAsyncTask(AsyncTask<T, ?, ?> asyncTask, T... params) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        else
            asyncTask.execute(params);
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        public DownloadImageTask() {
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
                in.close();
            } catch (Exception e) {
                Log.e("jComic", "Failed in getting book cover: " + urldisplay);
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
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
        b.putSerializable("book", book);
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

package com.jsoft.jcomic;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.net.http.HttpResponseCache;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.WebView;
import android.widget.GridView;

import com.jsoft.jcomic.adapter.GridViewImageAdapter;
import com.jsoft.jcomic.helper.AppConstant;
import com.jsoft.jcomic.helper.BookDTO;
import com.jsoft.jcomic.helper.BookmarkDb;
import com.jsoft.jcomic.helper.Utils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class GridViewActivity extends AppCompatActivity {

    private Utils utils;
    private ArrayList<String> imagePaths = new ArrayList<>();
    private GridViewImageAdapter adapter;
    private GridView gridView;
    private WebView webView;
    private int columnWidth;
    private List<BookDTO> books;
    private BookmarkDb bookmarkDb;
    private GridViewActivity gridViewActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bookmarkDb = new BookmarkDb(this);
        books = bookmarkDb.getBookmarkedList();
        //books = new ArrayList<BookDTO>();
        enableHttpCaching();
        gridViewActivity = this;
        setContentView(R.layout.activity_grid_view);

        utils = new Utils(this);
        gridView = initGridLayout();
        webView = (WebView) findViewById(R.id.web_view);
        webView.setVisibility(View.INVISIBLE);
        adapter = new GridViewImageAdapter(GridViewActivity.this, columnWidth, books);
        gridView.setAdapter(adapter);
        bookmarkDb.clearDb();
    }

    @Override
    public void onResume() {
        super.onResume();
        books = bookmarkDb.getBookmarkedList();
        adapter = new GridViewImageAdapter(GridViewActivity.this, columnWidth, books);
        gridView.setAdapter(adapter);
    }

    private GridView initGridLayout() {
        Resources r = getResources();
        gridView = (GridView) findViewById(R.id.grid_view);
        float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                AppConstant.GRID_PADDING, r.getDisplayMetrics());

        int numColumns = 3;
        columnWidth = (int) ((utils.getScreenWidth() - ((numColumns + 1) * padding)) / numColumns);

        gridView.setNumColumns(numColumns);
        gridView.setColumnWidth(columnWidth);
        gridView.setStretchMode(GridView.NO_STRETCH);
        gridView.setPadding((int) padding, (int) padding, (int) padding,
                (int) padding);
        gridView.setHorizontalSpacing((int) padding);
        gridView.setVerticalSpacing((int) padding);

        return gridView;
    }

    public void getEpisodeList(int position) {
        Intent i = new Intent(this, EpisodeListActivity.class);
        i.putExtra("bookUrl", books.get(position).getBookUrl());
        startActivityForResult(i, position);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    public void goToCartoonMad(View view) {
        openWebView("http://www.cartoonmad.com/m/");
    }

    public void goTo8Comic(View view) {
        openWebView("http://m.comicbus.com/");
    }

    public void goToDM5(View view) {
        //goToUrl("http://m.dm5.com/manhua-list/");
        openWebView("http://m.dm5.com/manhua-list/");
    }

    public void goToHome(View view) {
        webView.setVisibility(View.GONE);
        gridView.setVisibility(View.VISIBLE);
    }

    private void openWebView(String url) {
        webView.setVisibility(View.VISIBLE);
        gridView.setVisibility(View.GONE);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        //setContentView(webView);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                try {
                    Log.e("jComics", "url=" + url);
                    Log.e("jComics", "current url=" + webView.getUrl());
                    Uri uri = Uri.parse(url);
                    if ((uri.getHost().contains("dm5.com") && uri.getPath().startsWith("/manhua-") && (uri.getQueryParameter("from") != null || webView.getUrl().contains("search?title=")))
                        || (uri.getHost().contains("cartoonmad.com") && uri.getPath().startsWith("/m/comic/"))
                        || (uri.getHost().contains("comicbus.com") && uri.getPath().startsWith("/comic/"))) {
                        Intent i = new Intent(gridViewActivity, EpisodeListActivity.class);
                        i.putExtra("bookUrl", url);
                        startActivity(i);
                        return true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        });
        webView.loadUrl(url);
    }

    private void goToUrl (String url) {
        //Uri uriUrl = Uri.parse(url);
        Uri uri = Uri.parse("googlechrome://navigate?url=" + url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        //Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
        startActivity(intent);
    }

    private void enableHttpCaching() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        {
            try {
                File httpCacheDir = new File(getApplicationContext().getCacheDir()
                        , "http");
                long httpCacheSize = 100 * 1024 * 1024; // 10 MiB
                HttpResponseCache.install(httpCacheDir, httpCacheSize);
            } catch (IOException e) {

            }
        }
        else
        {
            File httpCacheDir = new File(getApplicationContext().getCacheDir()
                    , "http");
            try {
                HttpResponseCache.install(httpCacheDir, 100 * 1024 * 1024);
            } catch (IOException e) {

            }
        }
    }
}

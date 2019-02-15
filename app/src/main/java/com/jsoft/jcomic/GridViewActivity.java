package com.jsoft.jcomic;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.net.http.HttpResponseCache;
import android.os.AsyncTask;
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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

        initWebView();

        gridView = initGridLayout();
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

    @Override
    public void onBackPressed() {
        if (webView.getVisibility() == View.VISIBLE && webView.canGoBack()) {
            webView.goBack();
            return;
        } else if (webView.getVisibility() == View.VISIBLE) {
            webView.setVisibility(View.GONE);
            gridView.setVisibility(View.VISIBLE);
            return;
        }
        // Otherwise defer to system default behavior.
        super.onBackPressed();
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

    private void initWebView() {
        webView = (WebView) findViewById(R.id.web_view);
        webView.setVisibility(View.INVISIBLE);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                try {
                    Uri uri = Uri.parse(url);
                    if (/*(uri.getHost().contains("dm5.com") && uri.getPath().startsWith("/manhua-") && (uri.getQueryParameter("from") != null || webView.getUrl().contains("search?title=")))
                            || */(uri.getHost().contains("cartoonmad.com") && uri.getPath().startsWith("/m/comic/"))
                            || (uri.getHost().contains("comicbus.com") && uri.getPath().startsWith("/comic/"))) {
                        Intent i = new Intent(gridViewActivity, EpisodeListActivity.class);
                        i.putExtra("bookUrl", url);
                        startActivity(i);
                        return true;
                    } else if (uri.getHost().contains("dm5.com")) {
                        new InterceptDM5Task("UTF-8", view).execute(new URL(uri.toString()));
                        return true;
                    }
                } catch (Exception e) {
                    Log.e("jComics", "Caught by shouldOverrideUrlLoading", e);
                }
                return false;
            }
        });
    }

    public class InterceptDM5Task extends AsyncTask<URL, Integer, List<String>> {
        private String encoding;
        private WebView wv;
        private String url;

        public InterceptDM5Task(String encoding, WebView wv) {
            this.encoding = encoding;
            this.wv = wv;
        }

        protected List<String> doInBackground(URL... urls) {
            List<String> result = new ArrayList<String>();
            URL urlConn = urls[0];
            this.url = urls[0].toString();
            try {
                HttpURLConnection conn = (HttpURLConnection) urlConn.openConnection();
                conn.setReadTimeout(5000);
                conn.setUseCaches(true);
                conn.setRequestProperty("Referer", "/manhua-list/");
                InputStream is = new BufferedInputStream(conn.getInputStream());
                BufferedReader in = new BufferedReader(new InputStreamReader(is, encoding));
                String readLine;
                while ((readLine = in.readLine()) != null) {
                    result.add(readLine);
                }
                in.close();
                is.close();
            } catch (Exception e) {
                Log.e("jComics", "Caught by InterceptDM5Task", e);
            }

            return result;
        }

        protected void onProgressUpdate(Integer... progress) {

        }

        protected void onPostExecute(List<String> result) {
            String data = "";
            if (result.size() > 0) {
                for (int i=0;i<result.size();i++) {
                    data = data + "\n" + result.get(i).replaceAll("\\s{4,}", "\n");
                }
            }
            if (data.contains("chapteritem")) {
                Intent i = new Intent(gridViewActivity, EpisodeListActivity.class);
                i.putExtra("bookUrl", url);
                gridViewActivity.startActivity(i);
            } else {
                data = data.replaceAll("var tagid = \"(.*)\";", "var tagid = \"$1\"; var categoryid = \"0\"");
                wv.loadDataWithBaseURL(url, data, "text/html; charset=utf-8", "utf-8", null);
            }
        }
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
        webView.setVisibility(View.VISIBLE);
        gridView.setVisibility(View.GONE);
        try {
            new InterceptDM5Task("UTF-8", webView).execute(new URL("http://m.dm5.com/manhua-list/"));
        } catch (Exception e) {
            Log.e("jComics", "Caught by goToDM5", e);
        }
    }

    public void goToHome(View view) {
        webView.setVisibility(View.GONE);
        gridView.setVisibility(View.VISIBLE);
    }

    private void openWebView(String url) {
        webView.setVisibility(View.VISIBLE);
        gridView.setVisibility(View.GONE);
        webView.loadUrl(url);
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

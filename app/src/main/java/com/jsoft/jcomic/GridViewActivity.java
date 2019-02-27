package com.jsoft.jcomic;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.net.http.HttpResponseCache;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.WebView;
import android.widget.GridView;
import android.widget.LinearLayout;

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
    private GridView gridView;
    private WebView webView;
    private List<BookDTO> books;
    private BookmarkDb bookmarkDb;
    private GridViewActivity gridViewActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bookmarkDb = new BookmarkDb(this);

        enableHttpCaching();
        gridViewActivity = this;
        setContentView(R.layout.activity_grid_view);

        utils = new Utils(this);

        initWebView();

        gridView = initGridLayout();
        //bookmarkDb.clearDb();
        isReadStoragePermissionGranted();
        isWriteStoragePermissionGranted();
    }

    @Override
    public void onResume() {
        super.onResume();
        books = bookmarkDb.getBookmarkedList();
        LinearLayout view = findViewById(R.id.button_list_link);
        if (!Utils.isInternetAvailable()) {
            view.setVisibility(View.GONE);
        } else {
            view.setVisibility(View.VISIBLE);
        }
        GridViewImageAdapter adapter = new GridViewImageAdapter(GridViewActivity.this, books);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.book_list_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_download:
                Intent intent = new Intent(this, DownloadListActivity.class);
                this.startActivityForResult(intent, 0);
                invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private GridView initGridLayout() {
        Resources r = getResources();
        gridView = findViewById(R.id.grid_view);
        float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                AppConstant.GRID_PADDING, r.getDisplayMetrics());

        int numColumns = 3;
        int columnWidth = (int) ((utils.getScreenWidth() - ((numColumns + 1) * padding)) / numColumns);

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
        webView = findViewById(R.id.web_view);
        webView.setVisibility(View.INVISIBLE);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                try {
                    Uri uri = Uri.parse(url);
                    if ((uri.getHost().contains("cartoonmad.com") && uri.getPath().startsWith("/m/comic/"))
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
                conn.setRequestProperty("Referer", "http://m.dm5.com/manhua-list/");
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
        openWebView("https://www.cartoonmad.com/m/?act=2");
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
        try {
            File httpCacheDir = new File(getApplicationContext().getCacheDir()
                    , "http");
            long httpCacheSize = 100 * 1024 * 1024; // 10 MiB
            HttpResponseCache.install(httpCacheDir, httpCacheSize);
        } catch (IOException e) {
            Log.e("jComics", "Exception caught in enableHttpCaching", e);
        }
    }

    public  boolean isReadStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 3);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            return true;
        }
    }

    public  boolean isWriteStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            return true;
        }
    }
}

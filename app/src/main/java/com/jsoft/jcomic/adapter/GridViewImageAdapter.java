package com.jsoft.jcomic.adapter;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jsoft.jcomic.GridViewActivity;
import com.jsoft.jcomic.R;
import com.jsoft.jcomic.helper.BookDTO;
import com.jsoft.jcomic.helper.Utils;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GridViewImageAdapter extends BaseAdapter {
    private GridViewActivity activity;
    private int imageWidth;
    private List<BookDTO> books;
    private Map<Integer, View> viewMap;

    public GridViewImageAdapter(GridViewActivity activity, int imageWidth, List<BookDTO> books) {
        this.activity = activity;
        this.imageWidth = imageWidth;
        this.books = books;
        viewMap = new HashMap<Integer, View>();
        //Log.e("jComics", "GridViewAdapter constructor");
    }

    @Override
    public int getCount() {
        return books.size();
    }

    @Override
    public Object getItem(int position) {
        return this.books.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = activity.getLayoutInflater();
            convertView = inflater.inflate(R.layout.content_book_list, parent, false);
        }

        TextView textViewItem = (TextView) convertView.findViewById(R.id.bookTitle);

        ImageView imageView = (ImageView) convertView.findViewById(R.id.bookImage);

        boolean offlineAvailable = false;
        File bookFile = new File(Environment.getExternalStorageDirectory().toString() + "/jComics/" + Utils.getHashCode(books.get(position).getBookUrl()) + "/book.json");
        if (bookFile.exists()) {
            offlineAvailable = true;
        }

        // get screen dimensions
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        if (Utils.isInternetAvailable() || offlineAvailable) {
            imageView.setOnClickListener(new OnImageClickListener(position));
        } else {
            imageView.setOnClickListener(null);
        }
        textViewItem.setText(books.get(position).getBookTitle());
        if (!Utils.isInternetAvailable() && !offlineAvailable) {
            textViewItem.setTextColor(Color.DKGRAY);
            imageView.setAlpha(0.5f);
        } else {
            textViewItem.setTextColor(Color.WHITE);
            imageView.setAlpha(1f);
        }
        if (books.get(position).getBookImg() == null) {
            executeAsyncTask(new DownloadImageTask(imageView, books.get(position), position), books.get(position).getBookImgUrl());
        } else {
            imageView.setImageBitmap(books.get(position).getBookImg());
        }

        return convertView;
    }

    class OnImageClickListener implements OnClickListener {

        int position;

        // constructor
        public OnImageClickListener(int position) {
            this.position = position;
        }

        @Override
        public void onClick(View v) {
            activity.getEpisodeList(position);
        }

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB) // API 11
    public static <T> void executeAsyncTask(AsyncTask<T, ?, ?> asyncTask, T... params) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        else
            asyncTask.execute(params);
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;
        BookDTO book;
        int position;

        public DownloadImageTask(ImageView bmImage, BookDTO book, int position) {
            this.bmImage = bmImage;
            this.book = book;
            this.position = position;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
                in.close();
            } catch (Exception e) {
                Log.e("jComic", "GridViewImageAdapter:doInBackground " + e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            book.setBookImg(result);
            bmImage.setImageBitmap(result);
        }
    }
}

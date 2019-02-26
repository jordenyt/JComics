package com.jsoft.jcomic.adapter;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
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
import com.jsoft.jcomic.helper.DownloadImageTask;
import com.jsoft.jcomic.helper.DownloadTaskListener;
import com.jsoft.jcomic.helper.Utils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GridViewImageAdapter extends BaseAdapter {
    private GridViewActivity activity;
    //private int imageWidth;
    private List<BookDTO> books;
    //private Map<Integer, View> viewMap;

    public GridViewImageAdapter(GridViewActivity activity, int imageWidth, List<BookDTO> books) {
        this.activity = activity;
        //this.imageWidth = imageWidth;
        this.books = books;
        //viewMap = new HashMap<Integer, View>();
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

        TextView textViewItem = (TextView) convertView.findViewById(R.id.downloadBookTitle);

        final ImageView imageView = (ImageView) convertView.findViewById(R.id.bookImage);

        boolean offlineAvailable = false;
        File bookFile = new File(Utils.getBookFile(books.get(position)),"book.json");
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
            //executeAsyncTask(new DownloadImageTask(imageView, books.get(position), position), books.get(position).getBookImgUrl());
            final int bookPosition = position;
            DownloadImageTask task = new DownloadImageTask(new DownloadTaskListener() {
                public void onDownloadImagePostExecute(String imgUrl, Bitmap result) {
                    books.get(bookPosition).setBookImg(result);
                    imageView.setImageBitmap(result);
                }
            }, null);
            task.execute(books.get(position).getBookImgUrl());
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
}

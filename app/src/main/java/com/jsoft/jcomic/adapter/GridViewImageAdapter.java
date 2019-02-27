package com.jsoft.jcomic.adapter;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GridViewImageAdapter extends BaseAdapter {
    private GridViewActivity activity;
    private List<BookDTO> books;

    public GridViewImageAdapter(GridViewActivity activity, List<BookDTO> books) {
        this.activity = activity;
        this.books = books;
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

        TextView textViewItem = convertView.findViewById(R.id.downloadBookTitle);

        ImageView imageView = convertView.findViewById(R.id.bookImage);

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
            (new DownloadImageTask(imageView, books.get(position))).execute(books.get(position).getBookImgUrl());
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

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;
        BookDTO book;

        public DownloadImageTask(ImageView bmImage, BookDTO book) {
            this.bmImage = bmImage;
            this.book = book;
        }

        protected Bitmap doInBackground(String... urls) {
            return Utils.downloadImage(urls[0], null);
        }

        protected void onPostExecute(Bitmap result) {
            book.setBookImg(result);
            bmImage.setImageBitmap(result);
        }
    }
}

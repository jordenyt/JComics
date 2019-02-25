package com.jsoft.jcomic.adapter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jsoft.jcomic.DownloadListActivity;
import com.jsoft.jcomic.R;
import com.jsoft.jcomic.helper.BookDTO;
import com.jsoft.jcomic.helper.DownloadItemDTO;
import com.jsoft.jcomic.helper.Utils;

import java.util.List;

public class DownloadListAdapter extends BaseAdapter {

    private List<DownloadItemDTO> items;
    private DownloadListActivity activity;

    public DownloadListAdapter(List<DownloadItemDTO> items, DownloadListActivity activity) {
        this.items = items;
        this.activity = activity;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = activity.getLayoutInflater();
            convertView = inflater.inflate(R.layout.content_download_list, parent, false);
        }

        TextView textViewBookTitle = (TextView) convertView.findViewById(R.id.downloadBookTitle);
        TextView textViewEpisodeTitle = (TextView) convertView.findViewById(R.id.downloadEpisodeTitle);
        ImageView imageView = (ImageView) convertView.findViewById(R.id.downloadBookImage);

        DownloadItemDTO item = items.get(position);

        textViewBookTitle.setText(item.book.getBookTitle());
        textViewEpisodeTitle.setText(item.episode.getEpisodeTitle());
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        if (item.book.getBookImg() != null) {
            imageView.setImageBitmap(item.book.getBookImg());
        } else {
            imageView.setImageBitmap(null);
        }

        ImageButton btnPlay = (ImageButton) convertView.findViewById(R.id.btnPlayDownload);
        ImageButton btnDelete = (ImageButton) convertView.findViewById(R.id.btnDeleteDownload);

        btnPlay.setOnClickListener(new EpisodePlayListener(item));
        btnDelete.setOnClickListener(new EpisodeDeleteListener(item));

        return convertView;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    class EpisodePlayListener implements View.OnClickListener {

        DownloadItemDTO item;
        // constructor
        public EpisodePlayListener(DownloadItemDTO item) {
            this.item = item;
        }

        @Override
        public void onClick(View v) {
            activity.startReading(item.book, item.getEpisodeIndex());
        }

    }

    class EpisodeDeleteListener implements View.OnClickListener {
        DownloadItemDTO item;
        // constructor
        public EpisodeDeleteListener(DownloadItemDTO item) {
            this.item = item;
        }

        @Override
        public void onClick(View v) {
            activity.deleteEpisode(item);
        }
    }
}

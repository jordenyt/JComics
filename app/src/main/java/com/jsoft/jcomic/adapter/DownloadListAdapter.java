package com.jsoft.jcomic.adapter;

import android.graphics.Typeface;
import android.support.constraint.ConstraintLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.jsoft.jcomic.DownloadListActivity;
import com.jsoft.jcomic.R;
import com.jsoft.jcomic.helper.DownloadItemDTO;
import com.jsoft.jcomic.helper.Utils;

import java.io.File;
import java.util.List;

public class DownloadListAdapter extends BaseAdapter {

    private List<DownloadItemDTO> items;
    private DownloadListActivity activity;

    public DownloadListAdapter(List<DownloadItemDTO> items, DownloadListActivity activity) {
        this.items = items;
        this.activity = activity;
    }

    public void setItems(List<DownloadItemDTO> items) {
        this.items = items;
    }

    public List<DownloadItemDTO> getItems() {
        return this.items;
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

        ConstraintLayout downloadItem = convertView.findViewById(R.id.downloadItem);
        TextView textViewBookTitle = convertView.findViewById(R.id.downloadBookTitle);
        TextView textViewEpisodeTitle = convertView.findViewById(R.id.downloadEpisodeTitle);
        ImageView imageView = convertView.findViewById(R.id.downloadBookImage);
        TextView textViewPageStatus = convertView.findViewById(R.id.page_status);
        TextView textViewEpisodeSize = convertView.findViewById(R.id.episode_size);

        DownloadItemDTO item = items.get(position);
        int jpgCount = 0;
        File episodeFile = Utils.getEpisodeFile(item.book, item.episode);
        for (File file : episodeFile.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".jpg"))
                jpgCount += 1;
        }

        textViewEpisodeSize.setText(Utils.formatSize(Utils.calFolderSize(episodeFile)));
        textViewEpisodeSize.setTypeface(null, Typeface.BOLD);
        textViewPageStatus.setText(jpgCount + " / " + item.episode.getImageUrl().size());
        textViewBookTitle.setText(item.book.getBookTitle());
        textViewBookTitle.setTypeface(null, Typeface.BOLD);
        textViewEpisodeTitle.setText(item.episode.getEpisodeTitle());
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        if (item.book.getBookImg() != null) {
            imageView.setImageBitmap(item.book.getBookImg());
        } else {
            imageView.setImageBitmap(null);
        }

        ImageButton btnPlay = convertView.findViewById(R.id.btnPlayDownload);
        ImageButton btnDelete = convertView.findViewById(R.id.btnDeleteDownload);

        btnPlay.setOnClickListener(new ImgageClickListener(item) {
            public void onClick(View v) {activity.startReading(item.book, item.getEpisodeIndex());}
        });
        btnDelete.setOnClickListener(new ImgageClickListener(item) {
            public void onClick(View v) {
                activity.deleteEpisode(item);
            }
        });
        downloadItem.setOnClickListener(new ImgageClickListener(item) {
            public void onClick(View v) {
                activity.viewBook(item.book);
            }
        });

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

    class ImgageClickListener implements View.OnClickListener {
        DownloadItemDTO item;
        public ImgageClickListener(DownloadItemDTO item) {
            this.item = item;
        }

        @Override
        public void onClick(View v) {}
    }
}

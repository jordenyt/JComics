package com.jsoft.jcomic.adapter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.jsoft.jcomic.EpisodeListActivity;
import com.jsoft.jcomic.R;
import com.jsoft.jcomic.helper.BookDTO;
import com.jsoft.jcomic.helper.BookmarkDb;
import com.jsoft.jcomic.helper.EpisodeDTO;
import com.jsoft.jcomic.helper.Utils;

import java.io.File;

public class EpisodeListAdapter extends BaseAdapter {

    private BookDTO book;
    private EpisodeListActivity activity;
    private BookmarkDb bookmarkDb;

    public EpisodeListAdapter(EpisodeListActivity activity, BookDTO book) {
        this.book = book;
        this.activity = activity;
        bookmarkDb = new BookmarkDb(activity);
    }

    @Override
    public int getCount() {
        return book.getEpisodes().size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater inflater = activity.getLayoutInflater();
            convertView = inflater.inflate(R.layout.content_episode_list, parent, false);
        }

        // object item based on the position
        EpisodeDTO episode = book.getEpisodes().get(position);

        // get the TextView and then set the text (item name) and tag (item ID) values
        TextView textViewItem = convertView.findViewById(R.id.episodeTitle);
        textViewItem.setText(episode.getEpisodeTitle());
        textViewItem.setTag(episode.getEpisodeUrl());
        String lastEpisode = bookmarkDb.getLastEpisode(book);


        boolean offlineAvailable = false;
        File episodeFile = new File(Utils.getEpisodeFile(book, episode), "episode.json");
        if (episodeFile.exists()) {
            offlineAvailable = true;
        }
        boolean isOnline = Utils.isInternetAvailable();

        if (episode.getEpisodeTitle().equals(lastEpisode)) {
            textViewItem.setTextColor(Color.RED);
        } else {
            if (isOnline || offlineAvailable) {
                textViewItem.setTextColor(Color.WHITE);
            } else {
                textViewItem.setTextColor(Color.DKGRAY);
            }
        }
        if (isOnline || offlineAvailable) {
            EpisodeClickListener episodeClickListener = new EpisodeClickListener(position);
            convertView.setOnClickListener(episodeClickListener);
            convertView.setOnLongClickListener(episodeClickListener);
        } else {
            convertView.setOnClickListener(null);
            convertView.setOnLongClickListener(null);
        }

        if (isOnline && offlineAvailable) {
            textViewItem.setTypeface(null, Typeface.BOLD);
        } else {
            textViewItem.setTypeface(null, Typeface.NORMAL);
        }

        return convertView;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Object getItem(int position) {
        return this.book.getEpisodes().get(position);
    }

    class EpisodeClickListener implements View.OnClickListener, View.OnLongClickListener {

        int position;

        // constructor
        public EpisodeClickListener(int position) {
            this.position = position;
        }

        @Override
        public void onClick(View v) {
            activity.startReading(position);
        }

        @Override
        public boolean onLongClick(View view) {
            if (Utils.isInternetAvailable()) {
                new AlertDialog.Builder(activity)
                        .setTitle("要下載嗎?")
                        .setMessage("下載後, 離線時可看")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                activity.downloadEpisode(position);
                                Toast.makeText(activity, "下載中", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
                return true;
            }
            return false;
        }
    }
}

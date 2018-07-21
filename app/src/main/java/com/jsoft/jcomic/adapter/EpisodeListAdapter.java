package com.jsoft.jcomic.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.jsoft.jcomic.EpisodeListActivity;
import com.jsoft.jcomic.R;
import com.jsoft.jcomic.helper.BookDTO;
import com.jsoft.jcomic.helper.BookmarkDb;
import com.jsoft.jcomic.helper.EpisodeDTO;

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
        TextView textViewItem = (TextView) convertView.findViewById(R.id.episodeTitle);
        textViewItem.setText(episode.getEpisodeTitle());
        textViewItem.setTag(episode.getEpisodeUrl());
        String lastEpisode = bookmarkDb.getLastEpisode(book);
        if (episode.getEpisodeTitle().equals(lastEpisode)) {
            textViewItem.setTextColor(Color.RED);
        } else {
            textViewItem.setTextColor(Color.WHITE);
        }

        convertView.setOnClickListener(new OnEpisodeClickListener(position));

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

    class OnEpisodeClickListener implements View.OnClickListener {

        int position;

        // constructor
        public OnEpisodeClickListener(int position) {
            this.position = position;
        }

        @Override
        public void onClick(View v) {
            activity.startReading(position);
        }

    }
}

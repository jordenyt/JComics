package com.jsoft.jcomic.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jsoft.jcomic.R;
import com.jsoft.jcomic.helper.BookDTO;
import com.jsoft.jcomic.helper.ComicsViewPager;
import com.jsoft.jcomic.helper.EpisodeDTO;
import com.jsoft.jcomic.helper.TouchImageView;
import com.jsoft.jcomic.helper.Utils;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class FullScreenImageAdapter extends PagerAdapter {
    private Activity _activity;
    private ComicsViewPager pager;
    private EpisodeDTO episode;
    private BookDTO book;
    public static Executor downloadImageTaskExecutor;

    // constructor
    public FullScreenImageAdapter(Activity activity, ComicsViewPager viewPager, EpisodeDTO episode, BookDTO book) {
        this._activity = activity;
        this.pager = viewPager;
        this.episode = episode;
        this.book = book;
        if (downloadImageTaskExecutor == null) {
            downloadImageTaskExecutor = Executors.newFixedThreadPool(5);
        }
    }

    @Override
    public int getCount() {
        return (this.episode.getPageCount() > 0 ? this.episode.getPageCount() : 1);
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        TouchImageView imgDisplay;

        LayoutInflater inflater = (LayoutInflater) _activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View viewLayout = inflater.inflate(R.layout.layout_fullscreen_image, container,false);

        imgDisplay = viewLayout.findViewById(R.id.imgDisplay);
        TextView progressText = viewLayout.findViewById(R.id.progress_textview);
        TextView statusText = viewLayout.findViewById(R.id.status_textview);

        if (episode.getPageCount() > 0) {
            progressText.setText("Downloading...");
            statusText.setText(episode.getBookTitle() + " - " + episode.getEpisodeTitle() + "    Page: " + (position + 1) + " / " + episode.getPageCount());

            File file = Utils.getImgFile(book, episode, episode.getPageNumByURL(episode.getImageUrl().get(position)));
            if (file.exists()) {
                imgDisplay.setImageBitmap(Utils.imageFromFile(file));
                progressText.setText("");
            } else {
                (new DownloadImageTask(imgDisplay, progressText)).executeOnExecutor(downloadImageTaskExecutor, episode.getImageUrl().get(position));
            }
        } else {
            progressText.setText("No Image");
            statusText.setText(episode.getBookTitle() + " - " + episode.getEpisodeTitle());
        }
        imgDisplay.setPager(pager);

        container.addView(viewLayout);

        return viewLayout;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((RelativeLayout) object);

    }

    public EpisodeDTO getEpisode() {
        return episode;
    }

    public void setEpisode(EpisodeDTO episode) {
        this.episode = episode;
    }

    private class DownloadImageTask extends AsyncTask<String, Integer, Bitmap> {
        ImageView bmImage;
        TextView progressText;

        public DownloadImageTask(ImageView bmImage, TextView progressText) {
            this.bmImage = bmImage;
            this.progressText = progressText;
        }

        protected Bitmap doInBackground(String... urls) {
            return Utils.downloadImage(urls[0], getEpisode().getEpisodeUrl());
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            progressText.setText("Downloading..." + progress[0] + "%");
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
            progressText.setText("");
        }

    }
}

package com.jsoft.jcomic.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jsoft.jcomic.R;
import com.jsoft.jcomic.helper.BookDTO;
import com.jsoft.jcomic.helper.ComicsViewPager;
import com.jsoft.jcomic.helper.DownloadImageTask;
import com.jsoft.jcomic.helper.DownloadTaskListener;
import com.jsoft.jcomic.helper.EpisodeDTO;
import com.jsoft.jcomic.helper.TouchImageView;
import com.jsoft.jcomic.helper.Utils;

import java.io.File;

public class FullScreenImageAdapter extends PagerAdapter {
    private Activity _activity;
    private ComicsViewPager pager;
    private EpisodeDTO episode;
    private BookDTO book;

    // constructor
    public FullScreenImageAdapter(Activity activity, ComicsViewPager viewPager, EpisodeDTO episode, BookDTO book) {
        this._activity = activity;
        //this._imagePaths = imagePaths;
        this.pager = viewPager;
        this.episode = episode;
        this.book = book;
    }

    @Override
    public int getCount() {
        //return this._imagePaths.size();
        return (this.episode.getPageCount() > 0 ? this.episode.getPageCount() : 1);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        LayoutInflater inflater = (LayoutInflater) _activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View viewLayout = inflater.inflate(R.layout.layout_fullscreen_image, container,
                false);

        final TouchImageView imgDisplay = (TouchImageView) viewLayout.findViewById(R.id.imgDisplay);
        final TextView progressText = (TextView)viewLayout.findViewById(R.id.progress_textview);
        TextView statusText = (TextView)viewLayout.findViewById(R.id.status_textview);

        if (episode.getPageCount() > 0) {
            progressText.setText("Downloading...");
            statusText.setText(episode.getBookTitle() + " - " + episode.getEpisodeTitle() + "    Page: " + (position + 1) + " / " + episode.getPageCount());

            File file = Utils.getImgFile(book, episode, position);
            if (file !=null && file.exists()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                imgDisplay.setImageBitmap(bitmap);
                progressText.setText("");
            } else {
                DownloadImageTask task = new DownloadImageTask(new DownloadTaskListener() {
                    public void onDownloadImagePostExecute(String imgUrl, Bitmap result) {
                        imgDisplay.setImageBitmap(result);
                        progressText.setText("");
                    }
                }, episode.getEpisodeUrl());
                task.execute(episode.getImageUrl().get(position));
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
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((RelativeLayout) object);
    }

    public EpisodeDTO getEpisode() {
        return episode;
    }

    public void setEpisode(EpisodeDTO episode) {
        this.episode = episode;
    }
}

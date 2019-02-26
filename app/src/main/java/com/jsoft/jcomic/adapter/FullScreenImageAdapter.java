package com.jsoft.jcomic.adapter;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
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
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.net.HttpURLConnection;

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
        TouchImageView imgDisplay;

        LayoutInflater inflater = (LayoutInflater) _activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View viewLayout = inflater.inflate(R.layout.layout_fullscreen_image, container,
                false);

        imgDisplay = (TouchImageView) viewLayout.findViewById(R.id.imgDisplay);
        TextView progressText = (TextView)viewLayout.findViewById(R.id.progress_textview);
        TextView statusText = (TextView)viewLayout.findViewById(R.id.status_textview);

        if (episode.getPageCount() > 0) {
            progressText.setText("Downloading...");
            statusText.setText(episode.getBookTitle() + " - " + episode.getEpisodeTitle() + "    Page: " + (position + 1) + " / " + episode.getPageCount());
            executeAsyncTask(new DownloadImageTask(imgDisplay, progressText), episode.getImageUrl().get(position));
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB) // API 11
    public static <T> void executeAsyncTask(AsyncTask<T, ?, ?> asyncTask, T... params) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        else
            asyncTask.execute(params);
    }

    private class DownloadImageTask extends AsyncTask<String, Integer, Bitmap> {
        ImageView bmImage;
        TextView progressText;

        public DownloadImageTask(ImageView bmImage, TextView progressText) {
            this.bmImage = bmImage;
            this.progressText = progressText;
        }

        protected Bitmap doInBackground(String... urls) {
            String imgUrl = urls[0];

            int pageNum = 0;
            for (int i=0; i<episode.getImageUrl().size(); i++) {
                if (episode.getImageUrl().get(i).equals(imgUrl)) {
                    pageNum = i;
                    break;
                }
            }
            File myDir = Utils.getEpisodeFile(book,episode);
            String fname = String.format("%04d", pageNum) + ".jpg";
            File file = new File (myDir, fname);

            if (myDir.exists() && file.exists()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            }

            Bitmap bitmap = null;
            try {
                HttpURLConnection conn = (HttpURLConnection) new java.net.URL(imgUrl).openConnection();
                conn.setReadTimeout(5000);
                conn.setUseCaches(true);
                //if (urldisplay.indexOf("cartoonmad") > -1) {
                //    conn.setRequestProperty("Referer", "https://www.cartoonmad.com/m/comic/");
                //}
                conn.setRequestProperty("Referer", getEpisode().getEpisodeUrl());
                /*InputStream in = conn.getInputStream();
                bitmap = BitmapFactory.decodeStream(in);*/

                InputStream in = new BufferedInputStream(conn.getInputStream());
                bitmap= BitmapFactory.decodeStream(in);

                int length=conn.getContentLength();
                int len=0,total_length=0,value=0;
                byte[] data=new byte[1024];

                while((len = in.read(data)) != -1){
                    total_length += len;
                    value = (int)((total_length/(float)length)*100);
                    publishProgress(value);
                    //Log.e("jComic", "Percentage: " + value + "%");
                }
                in.close();
                conn.disconnect();
            } catch (Exception e) {
                Log.e("jComic", "" + e.getMessage());
                e.printStackTrace();
            }
            //Log.e("jComic", "Finish Get Image: " + urldisplay);
            return bitmap;
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

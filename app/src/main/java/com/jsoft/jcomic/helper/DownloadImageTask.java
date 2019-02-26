package com.jsoft.jcomic.helper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;

public class DownloadImageTask extends AsyncTask<String, Integer, Bitmap> {
    DownloadTaskListener listener;
    String refererUrl;
    String imgUrl;

    public DownloadImageTask(DownloadTaskListener listener, String refererUrl) {
        this.listener = listener;
        this.refererUrl = refererUrl;
    }

    protected Bitmap doInBackground(String... urls) {
        this.imgUrl = urls[0];

        Bitmap bitmap = null;
        try {
            HttpURLConnection conn = (HttpURLConnection) new java.net.URL(imgUrl).openConnection();
            conn.setReadTimeout(5000);
            conn.setUseCaches(true);
            if (refererUrl != null) {
                conn.setRequestProperty("Referer", refererUrl);
            }

            InputStream in = new BufferedInputStream(conn.getInputStream());
            bitmap= BitmapFactory.decodeStream(in);

            int length=conn.getContentLength();
            int len=0,total_length=0,value=0;
            byte[] data=new byte[1024];

            while((len = in.read(data)) != -1){
                total_length += len;
                value = (int)((total_length/(float)length)*100);
                publishProgress(value);
            }
            in.close();
            conn.disconnect();
        } catch (Exception e) {
            Log.e("jComic", "Exception caught in DownloadImageTask", e);
        }
        return bitmap;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        super.onProgressUpdate(progress);
        listener.onDownloadImageProgressUpdate(progress);
    }

    protected void onPostExecute(Bitmap result) {
        listener.onDownloadImagePostExecute(imgUrl, result);
    }
}

package com.jsoft.jcomic.helper;

import android.graphics.Bitmap;

public abstract class DownloadTaskListener {
    void onDownloadImageProgressUpdate(Integer... progress){

    }
    void onDownloadImagePostExecute(String imgUrl, Bitmap result){

    }
}

package com.jsoft.jcomic.helper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * Created by Jorden on 1/10/15.
 */
public class Utils {
    private Context _context;
    private static Date lastOnlineCheck;
    private static boolean isOnline;

    // constructor
    public Utils(Context context) {
        this._context = context;
    }

    // Reading file paths from SDCard
    public ArrayList<String> getFilePaths() {
        ArrayList<String> filePaths = new ArrayList<String>();
        String tempPath = "/storage/extSdCard"
                + File.separator + AppConstant.PHOTO_ALBUM;
        File directory = new File(tempPath);

        // check for directory
        if (directory.isDirectory()) {
            // getting list of file paths
            File[] listFiles = directory.listFiles();

            // Check for count
            if (listFiles.length > 0) {

                // loop through all files
                for (int i = 0; i < listFiles.length; i++) {

                    // get file path
                    String filePath = listFiles[i].getAbsolutePath();

                    // check for supported file extension
                    if (IsSupportedFile(filePath)) {
                        // Add image path to array list
                        filePaths.add(filePath);
                    }
                }
            } else {
                // image directory is empty
                Toast.makeText(
                        _context,
                        AppConstant.PHOTO_ALBUM
                                + " is empty. Please load some images in it !",
                        Toast.LENGTH_LONG).show();
            }

        } else {
            AlertDialog.Builder alert = new AlertDialog.Builder(_context);
            alert.setTitle("Error!");
            alert.setMessage(tempPath
                    + " is not a directory path! Please set the image directory name AppConstant.java class");
            alert.setPositiveButton("OK", null);
            alert.show();
        }

        return filePaths;
    }

    // Check supported file extensions
    private boolean IsSupportedFile(String filePath) {
        String ext = filePath.substring((filePath.lastIndexOf(".") + 1),
                filePath.length());

        return (AppConstant.FILE_EXTN
                .contains(ext.toLowerCase(Locale.getDefault())));
    }

    /*
     * getting screen width
     */
    public int getScreenWidth() {
        int columnWidth;
        WindowManager wm = (WindowManager) _context
                .getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        final Point point = new Point();
        try {
            display.getSize(point);
        } catch (java.lang.NoSuchMethodError ignore) { // Older device
            point.x = display.getWidth();
            point.y = display.getHeight();
        }
        columnWidth = point.x;
        return columnWidth;
    }

    public static String getHashCode(String s) {
        return getHashCode(s, 16);
    }

    public static String getHashCode(String s, int length) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(s.getBytes(StandardCharsets.UTF_8));
            byte[] digest = messageDigest.digest();
            String hex = String.format("%064x", new BigInteger(1, digest));
            return hex.substring(0, length);
        } catch (Exception e) {
            Log.e("jComics", "Error in getHashCode", e);
        }
        return "error";
    }

    public static boolean isInternetAvailable() {
        if (lastOnlineCheck == null || (new Date()).getTime() - lastOnlineCheck.getTime() > 2000) {
            try {
                InetAddress address = getInetAddressByName("baidu.com");
                lastOnlineCheck = new Date();
                isOnline = (address != null);
                return isOnline;
            } catch (Exception e) {
                Log.e("jComics", "Exception caught by isInternetAvailable", e);
            }
        }
        return isOnline;
    }

    public static InetAddress getInetAddressByName(String name)
    {
        AsyncTask<String, Void, InetAddress> task = new AsyncTask<String, Void, InetAddress>() {
            @Override
            protected InetAddress doInBackground(String... params){
                try{
                    return InetAddress.getByName(params[0]);
                }catch (Exception e){
                    return null;
                }
            }
        };
        try{
            return task.execute(name).get();
        }catch (InterruptedException e){
            return null;
        }catch (ExecutionException e){
            return null;
        }
    }

    public static void writeToFile(String data, File path, String filename)
    {
        // Get the directory for the user's public pictures directory.
        final File file = new File(path, filename);

        // Save your stream, don't forget to flush() it before closing it.

        try
        {
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(data);

            myOutWriter.close();

            fOut.flush();
            fOut.close();
        }
        catch (Exception e)
        {
            Log.e("jComics", "File write failed: " + e.toString());
        }
    }

    public static File getRootFile() {
        return new File(Environment.getExternalStorageDirectory().toString() + "/jComics");
    }

    public static File getBookFile(BookDTO book) {
        return new File(getRootFile(), Utils.getHashCode(book.getBookUrl()));
    }

    public static File getEpisodeFile(BookDTO book, EpisodeDTO episode) {
        return new File(getBookFile(book), Utils.getHashCode(episode.getEpisodeUrl()));
    }

    public static File getImgFile(BookDTO book, EpisodeDTO episode, int pageNum) {
        return new File(getEpisodeFile(book, episode), String.format("%04d", pageNum) + ".jpg");
    }

    public static Bitmap downloadImage(String imgUrl, String referer) {
        Bitmap bitmap = null;
        try {
            HttpURLConnection conn = (HttpURLConnection) new java.net.URL(imgUrl).openConnection();
            conn.setReadTimeout(5000);
            conn.setUseCaches(true);
            if (referer != null) {
                conn.setRequestProperty("Referer", referer);
            }

            InputStream in = new BufferedInputStream(conn.getInputStream());
            bitmap= BitmapFactory.decodeStream(in);

            in.close();
            conn.disconnect();
        } catch (Exception e) {
            Log.e("jComics", "Exception caught in downloadImage", e);
        }
        return bitmap;
    }

    public static Bitmap imageFromFile(File file) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }

    public static long calFolderSize(File directory) {
        long length = 0;
        for (File file : directory.listFiles()) {
            if (file.isFile())
                length += file.length();
            else
                length += calFolderSize(file);
        }
        return length;
    }

    public static String formatSize(long v) {
        if (v < 1024) return v + " B";
        int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
        return String.format("%.1f %sB", (double)v / (1L << (z*10)), " KMGTPE".charAt(z));
    }

}

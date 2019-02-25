package com.jsoft.jcomic;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import com.google.gson.Gson;
import com.jsoft.jcomic.adapter.DownloadListAdapter;
import com.jsoft.jcomic.helper.BookDTO;
import com.jsoft.jcomic.helper.DownloadItemDTO;
import com.jsoft.jcomic.helper.EpisodeDTO;
import com.jsoft.jcomic.helper.Utils;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class DownloadListActivity extends AppCompatActivity {

    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_list);
        listView = this.findViewById(R.id.download_list_view);
    }

    @Override
    public void onResume() {
        super.onResume();
        List<DownloadItemDTO> items = new ArrayList<DownloadItemDTO>();
        Gson gson = new Gson();
        File rootFolder = Utils.getRootFile();
        if (rootFolder.isDirectory()) {
            File[] rootFolderList = rootFolder.listFiles();
            for (File rootFile : rootFolderList) {
                if (rootFile.isDirectory()) {
                    File bookFile = new File(rootFile, "book.json");
                    if (bookFile.exists()) {
                        try {
                            BookDTO book = gson.fromJson(new FileReader(bookFile.getAbsolutePath()), BookDTO.class);
                            File bookImgFile = new File(rootFile, "book.jpg");
                            if (bookImgFile.exists()) {
                                BitmapFactory.Options options = new BitmapFactory.Options();
                                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                                Bitmap bookImg =  BitmapFactory.decodeFile(bookImgFile.getAbsolutePath(), options);
                                book.setBookImg(bookImg);
                            }

                            File[] bookFolderList = rootFile.listFiles();
                            for (File bookFolderFile : bookFolderList) {
                                if (bookFolderFile.isDirectory()) {
                                    File episodeFile = new File(bookFolderFile, "episode.json");
                                    EpisodeDTO episode = gson.fromJson(new FileReader(episodeFile.getAbsolutePath()), EpisodeDTO.class);
                                    File[] episodeFolderList = bookFolderFile.listFiles();
                                    int jpgCount = 0;
                                    for (File episodeFolderFile : episodeFolderList) {
                                        if (episodeFolderFile.isFile() && episodeFolderFile.getName().contains(".jpg")) {
                                            jpgCount += 1;
                                        }
                                    }
                                    DownloadItemDTO item = new DownloadItemDTO(book, episode, jpgCount);
                                    items.add(item);
                                }
                            }
                        } catch (Exception e) {

                        }
                    }
                }
            }
        }

        DownloadListAdapter adapter = new DownloadListAdapter(items, this);
        listView.setAdapter(adapter);

    }

    public void startReading(BookDTO book, int position) {
        Intent intent = new Intent(this, FullscreenActivity.class);
        intent.putExtra("position", position);
        Bundle b = new Bundle();
        b.putSerializable("book", book.getSerializable());
        intent.putExtras(b);
        this.startActivityForResult(intent, 0);
    }

    public void deleteEpisode(DownloadItemDTO item) {
        File dir = Utils.getEpisodeFile(item.book, item.episode);
        if (dir.isDirectory()) {
            deleteRecursive(dir);
        }
        dir = Utils.getBookFile(item.book);
        int episodeCount = 0;
        for (File f: dir.listFiles()) {
            if (f.isDirectory()) {
                episodeCount += 1;
            }
        }
        if (episodeCount == 0) {
            deleteRecursive(dir);
        }
        finish();
        startActivity(getIntent());
    }

    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }
}

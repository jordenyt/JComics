package com.jsoft.jcomic.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.jsoft.jcomic.helper.BookmarkDbHelper.BookmarkEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Jorden on 4/10/15.
 */
public class BookmarkDb {

    private BookmarkDbHelper mDbHelper;
    private SQLiteDatabase db;

    public BookmarkDb(Context context) {
        mDbHelper = new BookmarkDbHelper(context);
        db = mDbHelper.getWritableDatabase();
    }

    private String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    public long insertBookIntoDb(BookDTO book) {
        //db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(BookmarkEntry.COLUMN_NAME_BOOK_URL, book.getBookUrl());
        values.put(BookmarkEntry.COLUMN_NAME_TITLE, book.getBookTitle());
        values.put(BookmarkEntry.COLUMN_NAME_BOOK_IMG_URL, book.getBookImgUrl());
        long result = db.insert(BookmarkEntry.TABLE_NAME,null,values);
        //db.close();
        return result;
    }

    public boolean bookInDb(BookDTO book) {
        //db = mDbHelper.getReadableDatabase();
        String queryString =
                "SELECT " + BookmarkEntry.COLUMN_NAME_BOOK_URL
                        + " FROM " + BookmarkEntry.TABLE_NAME
                        + " WHERE " + BookmarkEntry.COLUMN_NAME_BOOK_URL + " = ?";
        Cursor c = db.rawQuery(queryString, new String[] {book.getBookUrl()});
        boolean result = (c.getCount() > 0);
        c.close();
        //db.close();
        return result;
    }

    public void updateLastRead(BookDTO book, int currEpisode, int currPage) {
        //db = mDbHelper.getWritableDatabase();
        // New value for one column
        ContentValues values = new ContentValues();
        values.put(BookmarkEntry.COLUMN_NAME_LAST_READ_EPISODE, book.getEpisodes().get(currEpisode).getEpisodeTitle());
        values.put(BookmarkEntry.COLUMN_NAME_LAST_READ_EPISODE_PAGE, currPage);
        values.put(BookmarkEntry.COLUMN_NAME_LAST_READ_TIME, getDateTime());

        // Which row to update, based on the ID
        String selection = BookmarkEntry.COLUMN_NAME_BOOK_URL + " LIKE ?";
        String[] selectionArgs = { book.getBookUrl() };

        int count = db.update(
                BookmarkEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);
        //db.close();
    }

    public boolean bookIsBookmarked(BookDTO book) {
        //db = mDbHelper.getReadableDatabase();
        String queryString =
                "SELECT " + BookmarkEntry.COLUMN_NAME_IS_BOOKMARK
                        + " FROM " + BookmarkEntry.TABLE_NAME
                        + " WHERE " + BookmarkEntry.COLUMN_NAME_BOOK_URL + " = ?";
        Cursor c = db.rawQuery(queryString, new String[] {book.getBookUrl()});
        boolean result = (c.getCount() > 0);
        if (result) {
            c.moveToFirst();
            if (c.getString(0) == null) {
                result = false;
            } else if ("Y".equals(c.getString(0))) {
                result = true;
            } else {
                result = false;
            }
        }
        c.close();
        //db.close();
        return result;
    }

    public void updateIsBookmark(BookDTO book, String isBookmarked) {
        //db = mDbHelper.getWritableDatabase();
        // New value for one column
        ContentValues values = new ContentValues();
        values.put(BookmarkEntry.COLUMN_NAME_IS_BOOKMARK, isBookmarked);
        values.put(BookmarkEntry.COLUMN_NAME_BOOK_IMG_URL, book.getBookImgUrl());
        values.put(BookmarkEntry.COLUMN_NAME_TITLE, book.getBookTitle());

        // Which row to update, based on the ID
        String selection = BookmarkEntry.COLUMN_NAME_BOOK_URL + " LIKE ?";
        String[] selectionArgs = { book.getBookUrl() };

        int count = db.update(
                BookmarkEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);
        //db.close();
    }

    public List<BookDTO> getBookmarkedList() {
        //db = mDbHelper.getReadableDatabase();
        String queryString =
                "SELECT " + BookmarkEntry.COLUMN_NAME_BOOK_URL
                        + ", " + BookmarkEntry.COLUMN_NAME_BOOK_IMG_URL
                        + ", " + BookmarkEntry.COLUMN_NAME_TITLE
                        + " FROM " + BookmarkEntry.TABLE_NAME
                        + " WHERE " + BookmarkEntry.COLUMN_NAME_IS_BOOKMARK + " = 'Y'"
                        + " ORDER BY " + BookmarkEntry.COLUMN_NAME_LAST_READ_TIME + " DESC";
        Cursor c = db.rawQuery(queryString, new String[] {});
        List<BookDTO> books = new ArrayList<BookDTO>();
        while (c.moveToNext()) {
            BookDTO book = new BookDTO(c.getString(c.getColumnIndexOrThrow(BookmarkEntry.COLUMN_NAME_BOOK_URL)));
            book.setBookImgUrl(c.getString(c.getColumnIndexOrThrow(BookmarkEntry.COLUMN_NAME_BOOK_IMG_URL)));
            book.setBookTitle(c.getString(c.getColumnIndexOrThrow(BookmarkEntry.COLUMN_NAME_TITLE)));
            books.add(book);
        }
        c.close();
        //db.close();
        return books;
    }

    public String getLastEpisode(BookDTO book) {
        String queryString =
                "SELECT " + BookmarkDbHelper.BookmarkEntry.COLUMN_NAME_LAST_READ_EPISODE
                        + " FROM " + BookmarkDbHelper.BookmarkEntry.TABLE_NAME
                        + " WHERE " + BookmarkDbHelper.BookmarkEntry.COLUMN_NAME_BOOK_URL + " = ?";
        Cursor c = db.rawQuery(queryString, new String[] {book.getBookUrl()});
        String result = "NOT FOUND";
        if (c.getCount() > 0) {
            c.moveToFirst();
            result = c.getString(
                    c.getColumnIndexOrThrow(BookmarkDbHelper.BookmarkEntry.COLUMN_NAME_LAST_READ_EPISODE)
            );
        }
        c.close();
        return result;
    }

    public int getLastEpisodePage(BookDTO book) {
        String queryString =
                "SELECT " + BookmarkDbHelper.BookmarkEntry.COLUMN_NAME_LAST_READ_EPISODE_PAGE
                        + " FROM " + BookmarkDbHelper.BookmarkEntry.TABLE_NAME
                        + " WHERE " + BookmarkDbHelper.BookmarkEntry.COLUMN_NAME_BOOK_URL + " = ?";
        Cursor c = db.rawQuery(queryString, new String[] {book.getBookUrl()});
        int result = 0;
        if (c.getCount() > 0) {
            c.moveToFirst();
            result = c.getInt(
                    c.getColumnIndexOrThrow(BookmarkDbHelper.BookmarkEntry.COLUMN_NAME_LAST_READ_EPISODE_PAGE)
            );
        }
        c.close();
        return result;
    }

    public void clearDb() {
        String selection = BookmarkEntry.COLUMN_NAME_IS_BOOKMARK + " NOT LIKE ?";
        String[] selectionArgs = { "Y" };
        db.delete(BookmarkEntry.TABLE_NAME, selection, selectionArgs);
    }
}

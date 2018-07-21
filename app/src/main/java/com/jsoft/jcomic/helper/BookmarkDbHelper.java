package com.jsoft.jcomic.helper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;


public final class BookmarkDbHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "JCBookmark.db";

    public static abstract class BookmarkEntry implements BaseColumns {
        public static final String TABLE_NAME = "JC_BOOKMARK";
        public static final String COLUMN_NAME_BOOK_URL = "book_url";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_BOOK_IMG_URL = "book_img_url";
        public static final String COLUMN_NAME_IS_BOOKMARK = "is_bookmark";
        public static final String COLUMN_NAME_LAST_READ_EPISODE = "last_read_episode";
        public static final String COLUMN_NAME_LAST_READ_EPISODE_PAGE = "last_read_episode_page";
        public static final String COLUMN_NAME_LAST_READ_TIME = "last_read_time";
    }

    public BookmarkDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + BookmarkEntry.TABLE_NAME + " (" +
                    BookmarkEntry._ID + " INTEGER PRIMARY KEY," +
                    BookmarkEntry.COLUMN_NAME_BOOK_URL + TEXT_TYPE + COMMA_SEP +
                    BookmarkEntry.COLUMN_NAME_TITLE + TEXT_TYPE + COMMA_SEP +
                    BookmarkEntry.COLUMN_NAME_BOOK_IMG_URL + TEXT_TYPE + COMMA_SEP +
                    BookmarkEntry.COLUMN_NAME_IS_BOOKMARK + TEXT_TYPE + COMMA_SEP +
                    BookmarkEntry.COLUMN_NAME_LAST_READ_EPISODE + TEXT_TYPE + COMMA_SEP +
                    BookmarkEntry.COLUMN_NAME_LAST_READ_EPISODE_PAGE + INT_TYPE + COMMA_SEP +
                    BookmarkEntry.COLUMN_NAME_LAST_READ_TIME + TEXT_TYPE +
                    " )";
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + BookmarkEntry.TABLE_NAME;
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}

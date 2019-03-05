package com.jsoft.jcomic.helper

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns


class BookmarkDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    abstract class BookmarkEntry : BaseColumns {
        companion object {
            val TABLE_NAME = "JC_BOOKMARK"
            val COLUMN_NAME_BOOK_URL = "book_url"
            val COLUMN_NAME_TITLE = "title"
            val COLUMN_NAME_BOOK_IMG_URL = "book_img_url"
            val COLUMN_NAME_IS_BOOKMARK = "is_bookmark"
            val COLUMN_NAME_LAST_READ_EPISODE = "last_read_episode"
            val COLUMN_NAME_LAST_READ_EPISODE_PAGE = "last_read_episode_page"
            val COLUMN_NAME_LAST_READ_TIME = "last_read_time"
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    companion object {
        // If you change the database schema, you must increment the database version.
        val DATABASE_VERSION = 2
        val DATABASE_NAME = "JCBookmark.db"

        private val TEXT_TYPE = " TEXT"
        private val INT_TYPE = " INTEGER"
        private val COMMA_SEP = ","
        private val SQL_CREATE_ENTRIES = "CREATE TABLE " + BookmarkEntry.TABLE_NAME + " (" +
                BaseColumns._ID + " INTEGER PRIMARY KEY," +
                BookmarkEntry.COLUMN_NAME_BOOK_URL + TEXT_TYPE + COMMA_SEP +
                BookmarkEntry.COLUMN_NAME_TITLE + TEXT_TYPE + COMMA_SEP +
                BookmarkEntry.COLUMN_NAME_BOOK_IMG_URL + TEXT_TYPE + COMMA_SEP +
                BookmarkEntry.COLUMN_NAME_IS_BOOKMARK + TEXT_TYPE + COMMA_SEP +
                BookmarkEntry.COLUMN_NAME_LAST_READ_EPISODE + TEXT_TYPE + COMMA_SEP +
                BookmarkEntry.COLUMN_NAME_LAST_READ_EPISODE_PAGE + INT_TYPE + COMMA_SEP +
                BookmarkEntry.COLUMN_NAME_LAST_READ_TIME + TEXT_TYPE +
                " )"

        private val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + BookmarkEntry.TABLE_NAME
    }
}

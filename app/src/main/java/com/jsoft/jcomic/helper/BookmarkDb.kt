package com.jsoft.jcomic.helper

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.jsoft.jcomic.helper.BookmarkDbHelper.BookmarkEntry
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Jorden on 4/10/15.
 */
class BookmarkDb(context: Context) {

    private val db: SQLiteDatabase

    init {
        val mDbHelper = BookmarkDbHelper(context)
        db = mDbHelper.writableDatabase
    }

    private val dateTime: String
        get() {
            val dateFormat = SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = Date()
            return dateFormat.format(date)
        }

    //db = mDbHelper.getReadableDatabase();
    //db.close();
    val bookmarkedList: List<BookDTO>
        get() {
            val queryString = ("SELECT " + BookmarkEntry.COLUMN_NAME_BOOK_URL
                    + ", " + BookmarkEntry.COLUMN_NAME_BOOK_IMG_URL
                    + ", " + BookmarkEntry.COLUMN_NAME_TITLE
                    + " FROM " + BookmarkEntry.TABLE_NAME
                    + " WHERE " + BookmarkEntry.COLUMN_NAME_IS_BOOKMARK + " = 'Y'"
                    + " ORDER BY " + BookmarkEntry.COLUMN_NAME_LAST_READ_TIME + " DESC")
            val c = db.rawQuery(queryString, arrayOf())
            val books = ArrayList<BookDTO>()
            while (c.moveToNext()) {
                val book = BookDTO(c.getString(c.getColumnIndexOrThrow(BookmarkEntry.COLUMN_NAME_BOOK_URL)))
                book.bookImgUrl = c.getString(c.getColumnIndexOrThrow(BookmarkEntry.COLUMN_NAME_BOOK_IMG_URL))
                book.bookTitle = c.getString(c.getColumnIndexOrThrow(BookmarkEntry.COLUMN_NAME_TITLE))
                books.add(book)
            }
            c.close()
            return books
        }

    fun insertBookIntoDb(book: BookDTO): Long {
        //db = mDbHelper.getWritableDatabase();
        val values = ContentValues()
        values.put(BookmarkEntry.COLUMN_NAME_BOOK_URL, book.bookUrl)
        values.put(BookmarkEntry.COLUMN_NAME_TITLE, book.bookTitle)
        values.put(BookmarkEntry.COLUMN_NAME_BOOK_IMG_URL, book.bookImgUrl)
        //db.close();
        return db.insert(BookmarkEntry.TABLE_NAME, null, values)
    }

    fun bookInDb(book: BookDTO): Boolean {
        //db = mDbHelper.getReadableDatabase();
        val queryString = ("SELECT " + BookmarkEntry.COLUMN_NAME_BOOK_URL
                + " FROM " + BookmarkEntry.TABLE_NAME
                + " WHERE " + BookmarkEntry.COLUMN_NAME_BOOK_URL + " = ?")
        val c = db.rawQuery(queryString, arrayOf(book.bookUrl!!))
        val result = c.count > 0
        c.close()
        //db.close();
        return result
    }

    fun updateLastRead(book: BookDTO, currEpisode: Int, currPage: Int) {
        //db = mDbHelper.getWritableDatabase();
        // New value for one column
        val values = ContentValues()
        values.put(BookmarkEntry.COLUMN_NAME_LAST_READ_EPISODE, book.episodes[currEpisode].episodeTitle)
        values.put(BookmarkEntry.COLUMN_NAME_LAST_READ_EPISODE_PAGE, currPage)
        values.put(BookmarkEntry.COLUMN_NAME_LAST_READ_TIME, dateTime)

        // Which row to update, based on the ID
        val selection = BookmarkEntry.COLUMN_NAME_BOOK_URL + " LIKE ?"
        val selectionArgs = arrayOf(book.bookUrl!!)

        db.update(
                BookmarkEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs)
        //db.close();
    }

    fun bookIsBookmarked(book: BookDTO): Boolean {
        //db = mDbHelper.getReadableDatabase();
        val queryString = ("SELECT " + BookmarkEntry.COLUMN_NAME_IS_BOOKMARK
                + " FROM " + BookmarkEntry.TABLE_NAME
                + " WHERE " + BookmarkEntry.COLUMN_NAME_BOOK_URL + " = ?")
        val c = db.rawQuery(queryString, arrayOf(book.bookUrl!!))
        var result = c.count > 0
        if (result) {
            c.moveToFirst()
            result = if (c.getString(0) == null) {
                false
            } else
                "Y" == c.getString(0)
        }
        c.close()
        //db.close();
        return result
    }

    fun updateIsBookmark(book: BookDTO, isBookmarked: String) {
        //db = mDbHelper.getWritableDatabase();
        // New value for one column
        val values = ContentValues()
        values.put(BookmarkEntry.COLUMN_NAME_IS_BOOKMARK, isBookmarked)
        values.put(BookmarkEntry.COLUMN_NAME_BOOK_IMG_URL, book.bookImgUrl)
        values.put(BookmarkEntry.COLUMN_NAME_TITLE, book.bookTitle)

        // Which row to update, based on the ID
        val selection = BookmarkEntry.COLUMN_NAME_BOOK_URL + " LIKE ?"
        val selectionArgs = arrayOf(book.bookUrl!!)

        db.update(
                BookmarkEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs)
        //db.close();
    }

    fun getBook(bookUrl: String): BookDTO? {
        //db = mDbHelper.getReadableDatabase();
        val queryString = ("SELECT " + BookmarkEntry.COLUMN_NAME_BOOK_URL
                + ", " + BookmarkEntry.COLUMN_NAME_BOOK_IMG_URL
                + ", " + BookmarkEntry.COLUMN_NAME_TITLE
                + " FROM " + BookmarkEntry.TABLE_NAME
                + " WHERE " + BookmarkDbHelper.BookmarkEntry.COLUMN_NAME_BOOK_URL + " = ?")
        val c = db.rawQuery(queryString, arrayOf(bookUrl))
        val books = ArrayList<BookDTO>()
        while (c.moveToNext()) {
            val book = BookDTO(c.getString(c.getColumnIndexOrThrow(BookmarkEntry.COLUMN_NAME_BOOK_URL)))
            book.bookImgUrl = c.getString(c.getColumnIndexOrThrow(BookmarkEntry.COLUMN_NAME_BOOK_IMG_URL))
            book.bookTitle = c.getString(c.getColumnIndexOrThrow(BookmarkEntry.COLUMN_NAME_TITLE))
            books.add(book)
        }
        c.close()
        return if (books.size > 0) {
            books[0]
        } else {
            null
        }
    }

    fun getLastEpisode(book: BookDTO): String? {
        val queryString = ("SELECT " + BookmarkDbHelper.BookmarkEntry.COLUMN_NAME_LAST_READ_EPISODE
                + " FROM " + BookmarkDbHelper.BookmarkEntry.TABLE_NAME
                + " WHERE " + BookmarkDbHelper.BookmarkEntry.COLUMN_NAME_BOOK_URL + " = ?")
        val c = db.rawQuery(queryString, arrayOf(book.bookUrl!!))
        var result : String? = null
        if (c.count > 0) {
            c.moveToFirst()
            result = c.getString(c.getColumnIndexOrThrow(BookmarkDbHelper.BookmarkEntry.COLUMN_NAME_LAST_READ_EPISODE))
        }
        c.close()
        return result
    }

    fun getLastEpisodePage(book: BookDTO): Int {
        val queryString = ("SELECT " + BookmarkDbHelper.BookmarkEntry.COLUMN_NAME_LAST_READ_EPISODE_PAGE
                + " FROM " + BookmarkDbHelper.BookmarkEntry.TABLE_NAME
                + " WHERE " + BookmarkDbHelper.BookmarkEntry.COLUMN_NAME_BOOK_URL + " = ?")
        val c = db.rawQuery(queryString, arrayOf(book.bookUrl!!))
        var result = 0
        if (c.count > 0) {
            c.moveToFirst()
            result = c.getInt(
                    c.getColumnIndexOrThrow(BookmarkDbHelper.BookmarkEntry.COLUMN_NAME_LAST_READ_EPISODE_PAGE)
            )
        }
        c.close()
        return result
    }

    fun clearDb() {
        val selection = BookmarkEntry.COLUMN_NAME_IS_BOOKMARK + " NOT LIKE ?"
        val selectionArgs = arrayOf("Y")
        db.delete(BookmarkEntry.TABLE_NAME, selection, selectionArgs)
    }
}

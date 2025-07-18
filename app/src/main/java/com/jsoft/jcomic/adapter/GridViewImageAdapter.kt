package com.jsoft.jcomic.adapter

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Color
import android.os.AsyncTask
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.jsoft.jcomic.GridViewActivity
import com.jsoft.jcomic.R
import com.jsoft.jcomic.helper.BookDTO
import com.jsoft.jcomic.helper.Utils
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class GridViewImageAdapter(private val activity: GridViewActivity, private val books: List<BookDTO>) : BaseAdapter() {

    init {
        if (downloadImageTaskExecutor == null) {
            downloadImageTaskExecutor = Executors.newFixedThreadPool(3)
        }
    }

    override fun getCount(): Int {
        return books.size
    }

    override fun getItem(position: Int): Any {
        return this.books[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var myConvertView = convertView
        if (myConvertView == null) {
            val inflater = activity.layoutInflater
            myConvertView = inflater.inflate(R.layout.content_book_list, parent, false)
        }

        val textViewItem = myConvertView!!.findViewById<TextView>(R.id.downloadBookTitle)
        val originViewItem = myConvertView!!.findViewById<TextView>(R.id.bookOrigin)
        val imageView = myConvertView.findViewById<ImageView>(R.id.bookImage)

        var offlineAvailable = false
        val bookFile = File(Utils.getBookFile(books[position]), "book.json")
        if (bookFile.exists()) {
            offlineAvailable = true
        }

        // get screen dimensions
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        if (Utils.isInternetAvailable || offlineAvailable) {
            imageView.setOnClickListener(OnImageClickListener(position))
        } else {
            imageView.setOnClickListener(null)
        }
        imageView.setOnLongClickListener(OnImageClickListener(position))
        textViewItem.text = books[position].bookTitle
        if (!Utils.isInternetAvailable && !offlineAvailable) {
            textViewItem.setTextColor(Color.DKGRAY)
            imageView.alpha = 0.5f
        } else {
            textViewItem.setTextColor(Color.WHITE)
            imageView.alpha = 1f
        }

        when {
            books[position].bookUrl!!.contains("cartoonmad.com/m/comic/") -> {originViewItem.text = "動漫狂"}
            books[position].bookUrl!!.contains("8comic.com/html/") -> {originViewItem.text = "無限動漫"}
            books[position].bookUrl!!.contains("dm5.com") -> {originViewItem.text = "動漫屋"}
            books[position].bookUrl!!.contains("qimiaomh.com/manhua/") -> {originViewItem.text = "奇妙動漫"}
            books[position].bookUrl!!.contains("kuman5.com") -> {originViewItem.text = "酷漫屋"}
            else -> {originViewItem.text = ""}
        }
        val bookImgFile = File(Utils.getBookFile(books[position]), "book.jpg")
        if (bookImgFile.exists()) {
            imageView.setImageBitmap(Utils.imageFromFile(bookImgFile))
        }
        imageView.setImageResource(R.mipmap.ic_launcher)
        if (books[position].bookImg == null && books[position].bookImgUrl != null) {
            DownloadImageTask(imageView, books[position]).executeOnExecutor(downloadImageTaskExecutor, books[position].bookImgUrl)
        } else if (books[position].bookImg != null){
            imageView.setImageBitmap(books[position].bookImg)
        }

        return myConvertView
    }

    internal inner class OnImageClickListener// constructor
    (var position: Int) : OnClickListener, View.OnLongClickListener {

        override fun onClick(v: View) {
            activity.getEpisodeList(position)
        }

        override fun onLongClick(view: View): Boolean {
            AlertDialog.Builder(activity)
                    .setTitle("要刪除嗎?")
                    .setMessage("你可以隨時在網站加回")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes) { dialog, whichButton ->
                        val book : BookDTO = getItem(position) as BookDTO
                        activity.unBookmark(book)
                        activity.onResume()
                    }
                    .setNegativeButton(android.R.string.no, null).show()
            return true
        }

    }

    private inner class DownloadImageTask(internal var bmImage: ImageView, internal var book: BookDTO) : AsyncTask<String, Void, Bitmap>() {

        override fun doInBackground(vararg urls: String): Bitmap? {
            return Utils.downloadImage(urls[0], null)
        }

        override fun onPostExecute(result: Bitmap?) {
            if (result != null) {
                book.bookImg = result
                bmImage.setImageBitmap(result)
            }
        }
    }

    companion object {
        private var downloadImageTaskExecutor: Executor? = null
    }
}

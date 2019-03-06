package com.jsoft.jcomic.adapter

import android.graphics.Bitmap
import android.graphics.Color
import android.os.AsyncTask
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
        var convertView = convertView
        if (convertView == null) {
            val inflater = activity.layoutInflater
            convertView = inflater.inflate(R.layout.content_book_list, parent, false)
        }

        val textViewItem = convertView!!.findViewById<TextView>(R.id.downloadBookTitle)

        val imageView = convertView.findViewById<ImageView>(R.id.bookImage)

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
        textViewItem.text = books[position].bookTitle
        if (!Utils.isInternetAvailable && !offlineAvailable) {
            textViewItem.setTextColor(Color.DKGRAY)
            imageView.alpha = 0.5f
        } else {
            textViewItem.setTextColor(Color.WHITE)
            imageView.alpha = 1f
        }
        val bookImgFile = File(Utils.getBookFile(books[position]), "book.jpg")
        if (bookImgFile.exists()) {
            imageView.setImageBitmap(Utils.imageFromFile(bookImgFile))
        }
        if (books[position].bookImg == null) {
            DownloadImageTask(imageView, books[position]).executeOnExecutor(downloadImageTaskExecutor, books[position].bookImgUrl)
        } else {
            imageView.setImageBitmap(books[position].bookImg)
        }

        return convertView
    }

    internal inner class OnImageClickListener// constructor
    (var position: Int) : OnClickListener {

        override fun onClick(v: View) {
            activity.getEpisodeList(position)
        }

    }

    private inner class DownloadImageTask(internal var bmImage: ImageView, internal var book: BookDTO) : AsyncTask<String, Void, Bitmap>() {

        override fun doInBackground(vararg urls: String): Bitmap? {
            return Utils.downloadImage(urls[0], null)
        }

        override fun onPostExecute(result: Bitmap) {
            book.bookImg = result
            bmImage.setImageBitmap(result)
        }
    }

    companion object {
        private var downloadImageTaskExecutor: Executor? = null
    }
}

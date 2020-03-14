package com.jsoft.jcomic.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.AsyncTask
import android.os.Environment
import android.util.Log
import android.view.WindowManager
import com.google.gson.Gson
import java.io.*
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ProtocolException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Date
import java.util.concurrent.ExecutionException
import kotlin.collections.ArrayList

/**
 * Created by Jorden on 1/10/15.
 */
class Utils// constructor
(private val _context: Context) {

    /*
     * getting screen width
     */
    // Older device
    val screenWidth: Int
        get() {
            val columnWidth: Int
            val wm = _context
                    .getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = wm.defaultDisplay

            val point = Point()
            try {
                display.getSize(point)
            } catch (e: Exception) {
                Log.e("jComics", "Exception caught in getScreenWidth", e)
            }

            columnWidth = point.x
            return columnWidth
        }

    companion object {
        private var lastOnlineCheck: Date? = null
        private var isOnline: Boolean = false

        @JvmOverloads
        fun getHashCode(s: String?, length: Int = 16): String {
            try {
                val messageDigest = MessageDigest.getInstance("SHA-256")
                messageDigest.update(s!!.toByteArray(StandardCharsets.UTF_8))
                val digest = messageDigest.digest()
                val hex = String.format("%064x", BigInteger(1, digest))
                return hex.substring(0, length)
            } catch (e: Exception) {
                Log.e("jComics", "Error in getHashCode", e)
            }

            return "error"
        }

        val isInternetAvailable: Boolean
            get() {
                if (lastOnlineCheck == null || Date().time - lastOnlineCheck!!.time > 2000) {
                    try {
                        val address = getInetAddressByName("baidu.com")
                        lastOnlineCheck = Date()
                        isOnline = address != null
                        return isOnline
                    } catch (e: Exception) {
                        Log.e("jComics", "Exception caught by isInternetAvailable", e)
                    }

                }
                return isOnline
            }

        private fun getInetAddressByName(name: String): InetAddress? {
            val task = object : AsyncTask<String, Void, InetAddress>() {
                override fun doInBackground(vararg params: String): InetAddress? {
                    return try {
                        InetAddress.getByName(params[0])
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            return try {
                task.execute(name).get()
            } catch (e: InterruptedException) {
                null
            } catch (e: ExecutionException) {
                null
            }

        }

        fun writeToFile(data: String, path: File, filename: String) {
            // Get the directory for the user's public pictures directory.
            val file = File(path, filename)

            // Save your stream, don't forget to flush() it before closing it.

            try {
                file.createNewFile()
                val fOut = FileOutputStream(file)
                val myOutWriter = OutputStreamWriter(fOut)
                myOutWriter.append(data)

                myOutWriter.close()

                fOut.flush()
                fOut.close()
            } catch (e: Exception) {
                Log.e("jComics", "File write failed: $e")
            }

        }

        fun saveBook(book: BookDTO) {
            val gson = Gson()
            var myDir = Utils.rootFile
            if (!myDir.exists()) myDir.mkdirs()
            myDir = Utils.getBookFile(book)
            if (!myDir.exists()) myDir.mkdirs()
            try {
                val nomediaFile = File(myDir, ".nomedia")
                if (!nomediaFile.exists()) nomediaFile.createNewFile()
                val cloneBook = book.clone()
                for (i in 0 until cloneBook.episodes.size) {
                    cloneBook.episodes[i].imageUrl = ArrayList()
                }
                cloneBook.bookImg = null
                Utils.writeToFile(gson.toJson(cloneBook), myDir, "book.json")

                val bookImg = File(myDir, "book.jpg")
                if (!bookImg.exists() && book.bookImg != null) saveImage(bookImg, book.bookImg)
            } catch (e: Exception) {
                Log.e("jComics", "Utils.saveBook Error", e)
            }
        }

        val rootFile: File
            get() = File(Environment.getExternalStorageDirectory().toString() + "/jComics")

        fun getBookFile(book: BookDTO): File {
            return File(rootFile, Utils.getHashCode(book.bookUrl))
        }

        fun getEpisodeFile(book: BookDTO, episode: EpisodeDTO): File {
            return File(getBookFile(book), Utils.getHashCode(episode.episodeUrl))
        }

        fun getImgFile(book: BookDTO, episode: EpisodeDTO, pageNum: Int): File {
            return File(getEpisodeFile(book, episode), String.format("%04d", pageNum) + ".jpg")
        }

        fun downloadImage(imgUrl: String, referer: String? = null): Bitmap? {
            var bitmap: Bitmap? = null
            try {
                val conn = java.net.URL(imgUrl).openConnection() as HttpURLConnection
                conn.readTimeout = 5000
                conn.useCaches = true
                if (referer != null) {
                    conn.setRequestProperty("Referer", referer)
                }

                val status = conn.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP
                        || status == HttpURLConnection.HTTP_MOVED_PERM
                        || status == HttpURLConnection.HTTP_SEE_OTHER) {
                    val newUrl = conn.getHeaderField("Location")
                    return downloadImage(newUrl, referer)
                }

                val bis = BufferedInputStream(conn.inputStream)
                bitmap = BitmapFactory.decodeStream(bis)

                bis.close()
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("jComics", "Exception caught in downloadImage", e)
            }

            return bitmap
        }

        fun getURLResponse(url: URL, referer: String?, encoding: String, cookies: String? = null): ArrayList<String> {
            val result = ArrayList<String>()
            try {
                val httpConnect = url.openConnection() as HttpURLConnection
                httpConnect.readTimeout = 5000
                httpConnect.useCaches = true
                if (referer != null)
                    httpConnect.setRequestProperty("Referer", referer)
                if (cookies != null)
                    httpConnect.setRequestProperty("Cookie", cookies)
                httpConnect.doInput = true
                httpConnect.doOutput = true

                httpConnect.connect()

                val status = httpConnect.responseCode

                if (status == HttpURLConnection.HTTP_MOVED_TEMP
                        || status == HttpURLConnection.HTTP_MOVED_PERM
                        || status == HttpURLConnection.HTTP_SEE_OTHER) {
                    val newUrl = httpConnect.getHeaderField("Location")
                    return getURLResponse(URL(newUrl), referer, encoding, cookies)
                }

                val inputStream = httpConnect.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream, encoding))
                var line : String? = null
                line = try { reader.readLine() } catch (e: ProtocolException) { null }
                while (line != null) {
                    result.add(line)
                    line = try { reader.readLine() } catch (e: ProtocolException) { null }
                }
                inputStream.close()
                reader.close()

            } catch (e: Exception) {
                Log.e("jComics", "Caught by getURLResponse", e)
            }
            return result
        }

        fun imageFromFile(file: File): Bitmap {
            val options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            return BitmapFactory.decodeFile(file.absolutePath, options)
        }

        fun saveImage(file: File, finalBitmap: Bitmap?) {
            if (!file.exists()) {
                try {
                    val out = FileOutputStream(file)
                    finalBitmap!!.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    out.flush()
                    out.close()
                } catch (e: Exception) {
                    Log.e("jComics", "Write File Error", e)
                }

            }
        }

        fun calFolderSize(directory: File): Long {
            var length: Long = 0
            for (file in directory.listFiles()) {
                length += if (file.isFile)
                    file.length()
                else
                    calFolderSize(file)
            }
            return length
        }

        fun formatSize(v: Long): String {
            if (v < 1024) return "$v B"
            val z = (63 - java.lang.Long.numberOfLeadingZeros(v)) / 10
            return String.format("%.1f %sB", v.toDouble() / (1L shl z * 10), " KMGTPE"[z])
        }

        fun deleteRecursive(fileOrDirectory: File) {
            if (fileOrDirectory.isDirectory)
                for (child in fileOrDirectory.listFiles())
                    deleteRecursive(child)

            fileOrDirectory.delete()
        }
    }

}

package com.jsoft.jcomic.helper

import android.graphics.Bitmap
import java.io.Serializable
import java.util.*

class BookDTO(var bookUrl: String?) : Serializable, Cloneable {
    var bookTitle: String? = null
    var bookSynopsis: String? = null
    var lastUpdate: Date? = null
    var book: String? = null
    var bookCategory: String? = null
    var episodes = ArrayList<EpisodeDTO>()
    var bookImgUrl: String? = null
    var bookImg: Bitmap? = null

    val serializable: BookDTO
        get() {
            val cloneBook = this.clone()
            cloneBook.bookImg = null
            return cloneBook
        }

    public override fun clone(): BookDTO {

        val cloneBook = super.clone() as BookDTO
        val episodeList = ArrayList<EpisodeDTO>()
        try {
            for (episode in this.episodes) {
                episodeList.add(episode.clone())
            }
        } catch (e: CloneNotSupportedException) {
            episodeList.clear()
        }
        cloneBook.episodes = episodeList
        return cloneBook
    }

    init {
        this.episodes = ArrayList()
    }

}

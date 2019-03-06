package com.jsoft.jcomic.helper

import java.io.Serializable

class EpisodeDTO(var episodeTitle: String?, var episodeUrl: String) : Serializable, Cloneable {
    var bookTitle: String? = null
    var pageCount: Int = 0
    var imageUrl: MutableList<String>? = null

    public override fun clone(): EpisodeDTO {
        return super.clone() as EpisodeDTO
    }

    fun getPageNumByURL(imgUrl: String): Int {
        var pageNum = -1
        for (i in 0 until this.imageUrl!!.size) {
            if (this.imageUrl!![i] == imgUrl) {
                pageNum = i
                break
            }
        }
        return pageNum
    }
}

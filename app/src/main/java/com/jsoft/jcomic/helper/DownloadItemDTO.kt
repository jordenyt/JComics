package com.jsoft.jcomic.helper

class DownloadItemDTO(var book: BookDTO, var episode: EpisodeDTO) {

    val episodeIndex: Int
        get() {
            for (i in 0 until book.episodes.size) {
                if (book.episodes[i].episodeUrl == episode.episodeUrl) {
                    return i
                }
            }
            return -1
        }

    init {
        for (e in book.episodes) {
            if (e.episodeUrl == episode.episodeUrl) {
                e.imageUrl = episode.imageUrl
            }
        }
    }

}

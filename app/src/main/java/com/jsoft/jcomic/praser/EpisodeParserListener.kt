package com.jsoft.jcomic.praser

import com.jsoft.jcomic.helper.EpisodeDTO

interface EpisodeParserListener {
    fun onEpisodeFetched(episode: EpisodeDTO)
}

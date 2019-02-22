package com.jsoft.jcomic.praser;

import com.jsoft.jcomic.helper.EpisodeDTO;

public interface EpisodeParserListener {
    void onEpisodeFetched(EpisodeDTO episode);
}

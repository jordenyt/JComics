package com.jsoft.jcomic.helper;

public class DownloadItemDTO {
    public BookDTO book;
    public EpisodeDTO episode;
    public int downloadedPageNum;

    public DownloadItemDTO(BookDTO book, EpisodeDTO episode, int downloadedPageNum) {
        this.book = book;
        this.episode = episode;
        for (EpisodeDTO e: book.getEpisodes()) {
            if (e.getEpisodeUrl().equals(episode.getEpisodeUrl())) {
                e.setImageUrl(episode.getImageUrl());
            }
        }
        this.downloadedPageNum = downloadedPageNum;
    }

    public int getEpisodeIndex() {
        for (int i=0;i<book.getEpisodes().size(); i++) {
            if (book.getEpisodes().get(i).getEpisodeUrl().equals(episode.getEpisodeUrl())) {
                return i;
            }
        }
        return -1;
    }

}
package com.jsoft.jcomic.helper;

import java.io.Serializable;
import java.util.List;

public class EpisodeDTO implements Serializable, Cloneable {
    private String bookTitle;
    private int pageCount;
    private List<String> imageUrl;
    private String episodeUrl;
    private String episodeTitle;

    public EpisodeDTO clone()
    {
        try {
            return (EpisodeDTO) super.clone();
        } catch( CloneNotSupportedException e ) {
            return null;
        }
    }

    public int getPageNumByURL(String imgUrl) {
        int pageNum = -1;
        for (int i=0; i<this.getImageUrl().size(); i++) {
            if (this.getImageUrl().get(i).equals(imgUrl)) {
                pageNum = i;
                break;
            }
        }
        return pageNum;
    }

    public EpisodeDTO(String episodeTitle, String episodeUrl) {
        this.episodeUrl = episodeUrl;
        this.episodeTitle = episodeTitle;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public List<String> getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(List<String> imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getEpisodeUrl() {
        return episodeUrl;
    }

    public void setEpisodeUrl(String episodeUrl) {
        this.episodeUrl = episodeUrl;
    }

    public String getEpisodeTitle() {
        return episodeTitle;
    }

    public void setEpisodeTitle(String episodeTitle) {
        this.episodeTitle = episodeTitle;
    }
}

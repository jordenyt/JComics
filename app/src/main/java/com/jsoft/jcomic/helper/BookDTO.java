package com.jsoft.jcomic.helper;

import android.graphics.Bitmap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BookDTO implements Serializable, Cloneable {
    private String bookUrl;
    private String bookTitle;
    private String bookSynopsis;
    private Date lastUpdate;
    private String book;
    private String bookCategory;
    private List<EpisodeDTO> episodes;
    private String bookImgUrl;
    private Bitmap bookImg;

    public BookDTO clone()
    {
        try {
            BookDTO cloneBook = (BookDTO) super.clone();
            List<EpisodeDTO> episodeList = new ArrayList<>();
            for (EpisodeDTO episode: this.getEpisodes()) {
                episodeList.add(episode.clone());
            }
            cloneBook.setEpisodes(episodeList);
            return cloneBook;
        } catch( CloneNotSupportedException e ) {
            return null;
        }
    }

    public BookDTO getSerializable() {
        BookDTO cloneBook = this.clone();
        cloneBook.setBookImg(null);
        return cloneBook;
    }

    public BookDTO(String bookUrl) {
        this.bookUrl = bookUrl;
    }

    public String getBookUrl() {
        return bookUrl;
    }

    public void setBookUrl(String bookUrl) {
        this.bookUrl = bookUrl;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getBookSynopsis() {
        return bookSynopsis;
    }

    public void setBookSynopsis(String bookSynopsis) {
        this.bookSynopsis = bookSynopsis;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getBook() {
        return book;
    }

    public void setBook(String book) {
        this.book = book;
    }

    public String getBookCategory() {
        return bookCategory;
    }

    public void setBookCategory(String bookCategory) {
        this.bookCategory = bookCategory;
    }

    public List<EpisodeDTO> getEpisodes() {
        return episodes;
    }

    public void setEpisodes(List<EpisodeDTO> episodes) {
        this.episodes = episodes;
    }

    public String getBookImgUrl() {
        return bookImgUrl;
    }

    public void setBookImgUrl(String bookImgUrl) {
        this.bookImgUrl = bookImgUrl;
    }

    public Bitmap getBookImg() {
        return bookImg;
    }

    public void setBookImg(Bitmap bookImg) {
        this.bookImg = bookImg;
    }

}

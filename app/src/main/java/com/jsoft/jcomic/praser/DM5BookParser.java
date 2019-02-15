package com.jsoft.jcomic.praser;

import android.text.Html;
import android.util.Log;

import com.jsoft.jcomic.EpisodeListActivity;
import com.jsoft.jcomic.helper.BookDTO;
import com.jsoft.jcomic.helper.EpisodeDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DM5BookParser extends BookParser {
    public DM5BookParser (BookDTO book, EpisodeListActivity activity) {
        super(book, activity, "UTF-8");
    }

    //Call when URL is fetched
    protected void getBookFromUrlResult(List<String> html) {
        List<EpisodeDTO> episodes = new ArrayList<EpisodeDTO>();
        String s = "";
        for (int i=0;i<html.size();i++) {
            s = s + html.get(i);
        }

        Pattern p = Pattern.compile("<a href=\"([A-Za-z0-9\\/]+?)\" title=\"(.*?)\" class=\"chapteritem\">(.+?)</a>");
        Matcher m = p.matcher(s);
        List<String> allMatches = new ArrayList<String>();
        while (m.find()) {
            String episodeUrl = "http://m.dm5.com" + m.group(1);
            //Log.d("jComics", episodeUrl);
            episodes.add(new EpisodeDTO(m.group(3).trim() + " " + m.group(2).trim(), episodeUrl));
        }

        p = Pattern.compile("<a href=\"([a-zA-Z0-9/]*?)\" class=\"chapteritem\">.*?<p class=\"detail-list-2-info-title\">(.*?)</p>.+?</a>");
        m = p.matcher(s);
        while (m.find()) {
            String episodeUrl = "http://m.dm5.com" + m.group(1);
            episodes.add(0, new EpisodeDTO(m.group(2), episodeUrl));
        }

        p = Pattern.compile("<span class=\"normal-top-title\">(.+?)</span>");
        m = p.matcher(s);
        while (m.find()) {
            book.setBookTitle(m.group(1).replace("&nbsp;", " ").replaceAll("<.*?>", ""));
        }

        p = Pattern.compile("<p class=\"detail-desc\" id=\"detail-desc\".*?>(.*?)</p>");
        m = p.matcher(s);
        while (m.find()) {
            book.setBookSynopsis(Html.fromHtml(m.group(1)).toString());
        }

        if (book.getBookSynopsis() == null) {
            p = Pattern.compile("<p class=\"detail-desc\".*?>(.*?)</p>");
            m = p.matcher(s);
            while (m.find()) {
                book.setBookSynopsis(Html.fromHtml(m.group(1)).toString());
            }
        }

        p = Pattern.compile("<div class=\"detail-main-cover\"><img src=\"(.+?)\"></div>");
        m = p.matcher(s);
        while (m.find()) {
            book.setBookImgUrl(m.group(1));
        }

        List<EpisodeDTO> cleanEpisodes = new ArrayList<EpisodeDTO>();
        for (EpisodeDTO episode: episodes) {
            boolean found = false;
            for (EpisodeDTO cleanEpisode: cleanEpisodes) {
                if (cleanEpisode.getEpisodeUrl().trim().equals(episode.getEpisodeUrl().trim())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                cleanEpisodes.add(episode);
            }
        }
        episodes = cleanEpisodes;
        for (EpisodeDTO episode: episodes) {
            episode.setBookTitle(book.getBookTitle());
        }
        book.setEpisodes(episodes);
        activity.onBookFetched(book);
    }
}

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
        for (int i=0; i<html.size();i++) {
            s = s + html.get(i);
        }

        Pattern p = Pattern.compile("<a href=\"([A-Za-z0-9\\/]+?)\" title=\"(.*?)\" class=\"chapteritem\">(.+?)</a>");
        Matcher m = p.matcher(s);
        List<String> allMatches = new ArrayList<String>();
        while (m.find()) {
            String episodeUrl = "http://m.dm5.com" + m.group(1);
            Log.d("jComics", episodeUrl);
            episodes.add(new EpisodeDTO(m.group(3), episodeUrl));
        }

        p = Pattern.compile("<span class=\"normal-top-title\">(.+?)</span>");
        m = p.matcher(s);
        while (m.find()) {
            book.setBookTitle(m.group(1).replace("&nbsp;", " ").replaceAll("<.*?>", ""));
        }

        p = Pattern.compile("<p class=\"detail-desc\".*?>(.*?)</p>");
        m = p.matcher(s);
        while (m.find()) {
            book.setBookSynopsis(Html.fromHtml(m.group(1)).toString());
        }

        p = Pattern.compile("<div class=\"detail-main-cover\"><img src=\"(.+?)\"></div>");
        m = p.matcher(s);
        while (m.find()) {
            book.setBookImgUrl(m.group(1));
        }

        book.setEpisodes(episodes);
        activity.onBookFetched(book);
    }
}

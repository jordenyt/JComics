package com.jsoft.jcomic.praser;

import android.text.Html;

import com.jsoft.jcomic.EpisodeListActivity;
import com.jsoft.jcomic.helper.BookDTO;
import com.jsoft.jcomic.helper.EpisodeDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CartoonMadBookParser extends BookParser {

    public CartoonMadBookParser(BookDTO book, EpisodeListActivity activity) {
        super(book, activity, "BIG5");
    }

    //Call when URL is fetched
    protected void getBookFromUrlResult(List<String> html) {
        List<EpisodeDTO> episodes = new ArrayList<EpisodeDTO>();
        for (String s: html) {
            Pattern p = Pattern.compile("<td>.*?<a href=(.+?)>(.+?)</a></td>");
            Matcher m = p.matcher(s);
            List<String> allMatches = new ArrayList<String>();
            while (m.find()) {
                String episodeUrl = "http://www.cartoonmad.com" + m.group(1);
                episodes.add(0, new EpisodeDTO(m.group(2), episodeUrl));
            }

            //p=Pattern.compile(".*<title>(.+) - 動漫狂行動版</title>.*");
            p=Pattern.compile(".*<span class=\"covertxt\"><font color=.*>(.+)</font></span>.*");
            m = p.matcher(s);
            if (m.matches()) {
                String bookTitle = m.group(1).replace("&nbsp;", " ");
                bookTitle = Html.fromHtml(bookTitle).toString();
                book.setBookTitle(bookTitle);
            }

            p=Pattern.compile(".*<td style=\"font-size:11pt;\">(.+)</td>.*");
            m = p.matcher(s);
            if (m.matches()) {
                book.setBookSynopsis(Html.fromHtml(m.group(1)).toString());
            }

            //p=Pattern.compile(".*<link rel=\"image_src\" type=\"image/jpeg\" href=\"(.+)\">.*");
            p=Pattern.compile(".*<span class=\"covers\"></span><img src=\"(.+)\" width=\".*\" height=\".*\".*>.*");
            m = p.matcher(s);
            if (m.matches()) {
                book.setBookImgUrl("http://www.cartoonmad.com" + Html.fromHtml(m.group(1)).toString());
            }
        }
        for (EpisodeDTO episode: episodes) {
            episode.setBookTitle(book.getBookTitle());
        }
        book.setEpisodes(episodes);
        activity.onBookFetched(book);
    }
}

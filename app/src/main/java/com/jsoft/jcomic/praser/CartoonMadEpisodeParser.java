package com.jsoft.jcomic.praser;

import android.util.Log;

import com.jsoft.jcomic.FullscreenActivity;
import com.jsoft.jcomic.helper.EpisodeDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CartoonMadEpisodeParser extends EpisodeParser {
    public CartoonMadEpisodeParser(EpisodeDTO episode, FullscreenActivity activity) {
        super(episode, activity, "BIG5");
    }
    protected void getEpisodeFromUrlResult(List<String> result) {
        episode.setImageUrl(new ArrayList<String>());
        String imageUrl_0 = "";
        String imageUrl_1 = "";
        int numPage = 0;
        String bookTitle = "";
        for (String s: result) {
            Pattern p = Pattern.compile(".*<img src=\"(https?:\\/\\/[a-zA-Z0-9:\\.]+)?(.+)(\\d\\d\\d)\" border=\"0\" oncontextmenu='return false' width=\"\\d+\">.*");
            //<img src="comicpic.asp?file=/1152/932/002" border="0" oncontextmenu='return false' width="700">
            Matcher m = p.matcher(s);
            if (m.matches()) {
                imageUrl_0 = m.group(1);
                imageUrl_1 = m.group(2);
            }

            p=Pattern.compile(".*...<a class=pages href=(.+)>(\\d+)</a>.*");
            m = p.matcher(s);
            if (m.matches()) {
                numPage = Integer.parseInt(m.group(2));
            }

        }
        episode.setPageCount(numPage);

        List<String> imageUrlList = new ArrayList<String>();
        if (imageUrl_0 == null) {
            imageUrl_0 = "http://www.cartoonmad.com/m/comic/";
        }
        for (int k=1; k<= episode.getPageCount(); k++) {
            String imageUrl = imageUrl_0 + imageUrl_1 + String.format("%03d", k);
            imageUrlList.add(imageUrl);
        }
        episode.setImageUrl(imageUrlList);
        activity.onEpisodeFetched(episode);
    }
}

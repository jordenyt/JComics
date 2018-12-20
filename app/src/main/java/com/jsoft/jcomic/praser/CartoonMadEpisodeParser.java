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
        String imageUrl_2 = "";
        int numPage = 0;
        String bookTitle = "";
        for (String s: result) {
            Pattern p = Pattern.compile(".*<img src=\"(https?:\\/\\/[a-zA-Z0-9:\\.]+)?(.+)(\\d\\d\\d)\\.([a-zA-Z]+)\" border=\"0\" oncontextmenu='return false' width=\"\\d+\">.*");
            //<img src="http://web2.cartoonmad.com/c86es736z62/1221/089/001.jpg" border="0" oncontextmenu='return false' width="360">
            Matcher m = p.matcher(s);
            if (m.matches()) {
                imageUrl_0 = m.group(1);
                imageUrl_1 = m.group(2);
                imageUrl_2 = m.group(4);
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
            imageUrl_0 = "https://www.cartoonmad.com";
        }
        for (int k=1; k<= episode.getPageCount(); k++) {
            String imageUrl = imageUrl_0 + imageUrl_1 + String.format("%03d", k) + "." + imageUrl_2;
            imageUrlList.add(imageUrl);
        }
        episode.setImageUrl(imageUrlList);
        activity.onEpisodeFetched(episode);
    }
}

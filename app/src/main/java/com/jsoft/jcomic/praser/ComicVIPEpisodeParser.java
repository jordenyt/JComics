package com.jsoft.jcomic.praser;

import com.jsoft.jcomic.helper.EpisodeDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by 01333855 on 05/10/2015.
 */
public class ComicVIPEpisodeParser extends EpisodeParser{
    public ComicVIPEpisodeParser(EpisodeDTO episode, EpisodeParserListener listener) {
        super(episode, listener, "BIG5");
    }

    protected void getEpisodeFromUrlResult(List<String> result) {
        episode.setImageUrl(new ArrayList<String>());
        int episodeId = 0;
        int bookId = 0;
        String code = "";

        if (episode.getEpisodeUrl().contains("www.comicbus.com")) {
            Pattern p = Pattern.compile("https://www\\.comicbus\\.com/show/([a-z-]+)(\\d+)\\.html\\?ch=(\\d+)");
            Matcher m = p.matcher(episode.getEpisodeUrl());
            if (m.matches()) {
                bookId = Integer.parseInt(m.group(2));
                episodeId = Integer.parseInt(m.group(3));
            }
        } else if (episode.getEpisodeUrl().contains("m.comicbus.com")) {
            Pattern p = Pattern.compile("https://m\\.comicbus\\.com/comic/[a-z]+_(\\d+)\\.html\\?ch=(\\d+)");
            Matcher m = p.matcher(episode.getEpisodeUrl());
            if (m.matches()) {
                bookId = Integer.parseInt(m.group(1));
                episodeId = Integer.parseInt(m.group(2));
            }
        }

        for (String s : result) {
            Pattern p = Pattern.compile(".*<script>var chs=(\\d+);var ti=(\\d+);var cs='([a-z0-9]+)';eval.*");
            Matcher m = p.matcher(s);
            if (m.matches()) {
                code = m.group(3);
            }
        }

        String extractCode = "";
        if (code.length() > 0 && episodeId > 0 && bookId > 0) {
            for (int i = 0; i < code.length() / 50; i++) { // i = 0 to 399
                if (Integer.parseInt(ss(code, i * 50, 4, true)) == episodeId) {
                    extractCode = ss(code, i * 50, 50, false);
                    break;
                }
            }
            int numPage = Integer.parseInt(ss(extractCode, 7, 3, true));
            episode.setPageCount(numPage);
            for (int i = 1; i <= numPage; i++) {
                String pageUrl = "https://img" + ss(extractCode, 4, 2, true) + ".8comic.com/"
                        + ss(extractCode, 6, 1, true) + "/" + bookId + "/" + ss(extractCode, 0, 4, true) + "/"
                        + String.format("%03d", i) + '_' + ss(extractCode, mm(i) + 10, 3, false) + ".jpg";
                episode.getImageUrl().add(pageUrl);
            }
        }
    }

    private String ss(String code,int position,int size, boolean integerOnly) {
        String subCode=code.substring(position,position+size);
        return integerOnly ? subCode.replaceAll("[a-z]*","") : subCode;
    }

    private int mm(int p) {
        return ((int) Math.floor((p - 1) / 10)) % 10 + ((p - 1) % 10) * 3;
    }
}

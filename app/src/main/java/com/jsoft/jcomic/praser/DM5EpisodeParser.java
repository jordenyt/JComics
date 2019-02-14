package com.jsoft.jcomic.praser;

import android.util.Log;

import com.jsoft.jcomic.FullscreenActivity;
import com.jsoft.jcomic.helper.EpisodeDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DM5EpisodeParser extends EpisodeParser {
    public DM5EpisodeParser(EpisodeDTO episode, FullscreenActivity activity) {
        super(episode, activity, "UTF-8");
    }

    private String intCode(int i) {
        String s = "";
        String characters="0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        if (i > characters.length()) {
            s = s + characters.charAt(i/characters.length());
        }
        s = s + characters.charAt(i % characters.length());
        return s;
    }

    protected void getEpisodeFromUrlResult(List<String> result) {
        String s = "";
        for (int i=0; i<result.size();i++) {
            s = s + result.get(i);
        }

        List<String> imageUrlList = new ArrayList<String>();

        Pattern p = Pattern.compile("eval\\(function\\(p,a,c,k,e,d\\)\\{.+\\}\\((.+)\\)\\s*</script>");
        Matcher m = p.matcher(s);
        List<String> allMatches = new ArrayList<String>();
        while (m.find()) {

            String s1= m.group(1);
            Pattern p1 = Pattern.compile("\\d+,\\d+,\'(.*)\'\\.split");
            Matcher m1 = p1.matcher(s1);
            String[] replaceStrArray= new String[0];
            while (m1.find()) {
                String s3=m1.group(1);
                //Log.e("jComics 1", s3);
                replaceStrArray = s3.split("\\|");
            }
            String s2= m.group(1);
            Pattern p2 = Pattern.compile("=\\[(.+?)\\];");
            Matcher m2 = p2.matcher(s2);
            while (m2.find()) {
                String s3=m2.group(1);
                String[] urls = s3.split(",");
                for (int i=0; i<urls.length; i++) {

                    String imgUrl = urls[i];
                    imgUrl = imgUrl.replaceAll("\\\\\\'", "");
                    for (int j=0; j < replaceStrArray.length; j++) {
                        if (replaceStrArray[j].length() > 0) {
                            imgUrl = imgUrl.replaceAll("\\b" + intCode(j) + "\\b", replaceStrArray[j]);
                        }
                    }
                    imageUrlList.add(imgUrl);
                }
            }
        }
        episode.setPageCount(imageUrlList.size());
        episode.setImageUrl(imageUrlList);
        activity.onEpisodeFetched(episode);
    }
}

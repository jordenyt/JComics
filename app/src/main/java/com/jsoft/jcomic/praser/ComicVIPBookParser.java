package com.jsoft.jcomic.praser;

import android.text.Html;

import com.jsoft.jcomic.helper.BookDTO;
import com.jsoft.jcomic.helper.EpisodeDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComicVIPBookParser extends BookParser {
    public ComicVIPBookParser (BookDTO book, BookParserListener listener) {
        super(book, listener, "BIG5");
    }

    //Call when URL is fetched
    protected void getBookFromUrlResult(List<String> html) {
        List<EpisodeDTO> episodes = new ArrayList<EpisodeDTO>();
        for (int i=2; i<html.size();i++) {
            String s = html.get(i-2).trim() + html.get(i-1).trim() + html.get(i).trim();
            s=s.replace("\n", "");

            if (book.getBookUrl().contains("www.comicbus.com")) {
                Pattern p = Pattern.compile(".*<a href='#' onclick=\"cview\\('(.+)-(.+)\\.html',(.+)\\);return false;\" id=\".+\" class=\".+\">\\s*(.+)</a>.*");
                Matcher m = p.matcher(s);
                if (m.matches()) {
                    int catid = Integer.parseInt(m.group(3));
                    String baseurl = "https://www.comicbus.com";
                    if (catid == 4 || catid == 6 || catid == 12 || catid == 22)
                        baseurl += "/show/cool-";
                    else if (catid == 1 || catid == 17 || catid == 19 || catid == 21)
                        baseurl += "/show/cool-";
                    else if (catid == 2 || catid == 5 || catid == 7 || catid == 9)
                        baseurl += "/show/cool-";
                    else if (catid == 10 || catid == 11 || catid == 13 || catid == 14)
                        baseurl += "/show/best-manga-";
                    else if (catid == 3 || catid == 8 || catid == 15 || catid == 16 || catid == 18 || catid == 20)
                        baseurl += "/show/best-manga-";
                    String episodeUrl = baseurl + m.group(1) + ".html?ch=" + m.group(2);
                    String episodeTitle = m.group(4).replaceAll("<script>.*?</script>", "").replaceAll("<.*?>", "").replaceAll("(.*?)\\s.*", "$1");
                    episodes.add(0, new EpisodeDTO(episodeTitle, episodeUrl));
                }

                p = Pattern.compile(".*<font color=\"#006666\">(.+)<b>.*");
                m = p.matcher(s);
                if (m.matches()) {
                    book.setBookTitle(m.group(1).replace("&nbsp;", " "));
                }

                p = Pattern.compile(".*<td colspan=\"3\" valign=\"top\" bgcolor=\"f0f8ff\" style=\"padding:10px;line-height:25px\">(.+)</td>.*");
                m = p.matcher(s);
                if (m.matches()) {
                    book.setBookSynopsis(Html.fromHtml(m.group(1)).toString());
                }

                p = Pattern.compile(".*<img src='(.+)' hspace=\"10\" vspace=\"10\" border=\"0\" style=\"border:#CCCCCC solid 1px\" />.*");
                m = p.matcher(s);
                if (m.matches()) {
                    book.setBookImgUrl("https://www.comicbus.com" + m.group(1));
                }
            } else if (book.getBookUrl().contains("m.comicbus.com")) {
                //Pattern p = Pattern.compile(".*<a href='#' onclick=\"cview\\('(.+)-(.+)\\.html',(.+)\\);return false;\" id=\".+\" class=\".+\">\\s*(.+)</a>.*");
                String baseurl = "https://m.comicbus.com";

                Pattern p = Pattern.compile(".*<td style=\".*\">.*<a href='(.+)' class=\"(Vol|Ch)\"  id=\"(.+)\"  >\\s*(.+)</a>.*");
                Matcher m = p.matcher(s);
                if (m.matches()) {
                    String episodeUrl = baseurl + m.group(1);
                    String episodeTitle = m.group(4).replaceAll("<script>.*?</script>", "").replaceAll("<.*?>", "");
                    episodes.add(0, new EpisodeDTO(episodeTitle, episodeUrl));
                }

                p = Pattern.compile(".*<font color=\"#FF3300\" style=\"font:14pt;font-weight:bold;\">(.+)</font>.*");
                m = p.matcher(s);
                if (m.matches()) {
                    book.setBookTitle(m.group(1).replace("&nbsp;", " ").replaceAll("<.*?>", ""));
                }

                p = Pattern.compile(".*<td colspan=\"3\" valign=\"top\" bgcolor=\"f8f8f8\" style=\"padding:10px;line-height:25px\">(.+)</td>.*");
                m = p.matcher(s);
                if (m.matches()) {
                    book.setBookSynopsis(Html.fromHtml(m.group(1)).toString());
                }

                p = Pattern.compile(".*<td align=\"center\" bgcolor=\"fafafa\"><img src='(.+)' hspace=\"10\" vspace=\"10\" border=\"0\" />.*");
                //<td align="center" bgcolor="fafafa"><img src='/pics/0/3654s.jpg' hspace="10" vspace="10" border="0" />
                //<td align="center" bgcolor="fafafa"><img src='/pics/0/103s.jpg' hspace="10" vspace="10" border="0" />
                m = p.matcher(s);
                if (m.matches()) {
                    book.setBookImgUrl("https://m.comicbus.com" + m.group(1).replaceAll("(\\d+)s\\.", "$1."));
                }
            }
        }
        for (EpisodeDTO episode: episodes) {
            episode.setBookTitle(book.getBookTitle());
        }
        book.setEpisodes(episodes);
    }
}

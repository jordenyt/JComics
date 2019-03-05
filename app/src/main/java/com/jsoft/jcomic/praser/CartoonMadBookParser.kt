package com.jsoft.jcomic.praser

import android.text.Html

import com.jsoft.jcomic.helper.BookDTO
import com.jsoft.jcomic.helper.EpisodeDTO

import java.util.ArrayList
import java.util.regex.Matcher
import java.util.regex.Pattern

class CartoonMadBookParser(book: BookDTO, listener: BookParserListener) : BookParser(book, listener, "BIG5") {

    //Call when URL is fetched
    override fun getBookFromUrlResult(html: ArrayList<String>) {
        val episodes = ArrayList<EpisodeDTO>()
        for (s in html) {
            var p = Pattern.compile("<td>.*?<a href=(.+?)>(.+?)</a></td>")
            var m = p.matcher(s)
            while (m.find()) {
                val episodeUrl = "https://www.cartoonmad.com" + m.group(1)
                episodes.add(0, EpisodeDTO(m.group(2), episodeUrl))
            }

            p = Pattern.compile(".*<input type=\"hidden\" name=\"name\" value=\"(.+?)\">.*")
            m = p.matcher(s)
            if (m.matches()) {
                var bookTitle = m.group(1) //.replace("&nbsp;", " ");
                bookTitle = Html.fromHtml(bookTitle).toString()
                book.bookTitle = bookTitle
            }

            p = Pattern.compile(".*<td style=\"font-size:11pt;\">(.+)</td>.*")
            m = p.matcher(s)
            if (m.matches()) {
                book.bookSynopsis = Html.fromHtml(m.group(1)).toString()
            }

            //p=Pattern.compile(".*<link rel=\"image_src\" type=\"image/jpeg\" href=\"(.+)\">.*");
            p = Pattern.compile(".*<span class=\"covers\"></span><img src=\"(.+)\" width=\".*\" height=\".*\".*>.*")
            m = p.matcher(s)
            if (m.matches()) {
                book.bookImgUrl = "http://www.cartoonmad.com" + Html.fromHtml(m.group(1)).toString()
            }
        }
        for (episode in episodes) {
            episode.bookTitle = book.bookTitle
        }
        book.episodes = episodes
    }
}

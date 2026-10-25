package com.jsoft.jcomic.praser

import android.content.Context
import android.util.Log
import com.jsoft.jcomic.helper.BilibiliManga
import com.jsoft.jcomic.helper.BookDTO
import com.jsoft.jcomic.helper.EpisodeDTO
import com.jsoft.jcomic.helper.MangaWebView
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URL

/**
 * Parser for the comics of manga.bilibili.com (哔哩哔哩漫画).
 *
 * The site only hands out its data through a signed api, the javascript of the site is the only
 * thing that can read it. The page of the comic is therefore opened in a web view outside of the
 * screen and the chapter list is taken from the state of the site.
 */
class BilibiliMangaBookParser(
    book: BookDTO,
    listener: BookParserListener,
    private val context: Context
) : BookParser(book, listener, "UTF-8") {

    override fun getURLResponse(url: URL, encoding: String): ArrayList<String> {
        // The content of the page is read by the web view, a http request is not needed.
        return arrayListOf("")
    }

    override fun getBookFromUrlResult(html: ArrayList<String>): Boolean {
        val comicId = BilibiliManga.comicId(book.bookUrl)
        if (comicId == null) {
            Log.e("jComics", "bilibili manga: cannot read the comic id from " + book.bookUrl)
            return false
        }

        MangaWebView(context).load(
            BilibiliManga.DETAIL_URL + comicId,
            READY_SCRIPT,
            RESULT_SCRIPT,
            timeoutMs = 45000
        ) { result ->
            readResult(result, comicId)
            loadedAllEpisodes()
        }
        return false
    }

    private fun readResult(result: String?, comicId: String) {
        if (result.isNullOrEmpty()) {
            Log.e("jComics", "bilibili manga: the page of the comic was not read")
            if (book.bookSynopsis == null) {
                book.bookSynopsis = "Cannot load page from Internet"
            }
            return
        }
        try {
            val json = JSONObject(rawJson(result))
            book.bookTitle = json.optString("title")
            book.bookSynopsis = json.optString("intro")
            val cover = json.optString("cover")
            if (cover.isNotEmpty()) {
                book.bookImgUrl = cover
            }

            val episodes = json.optJSONArray("episodes")
            val list = ArrayList<EpisodeDTO>()
            if (episodes != null) {
                for (i in 0 until episodes.length()) {
                    val episode = episodes.getJSONObject(i)
                    val id = episode.optLong("id")
                    if (id <= 0) continue
                    val title = episode.optString("title")
                    val shortTitle = episode.optString("short")
                    val label = if (shortTitle.isNotEmpty()) "$shortTitle $title" else title
                    list.add(EpisodeDTO(label, BilibiliManga.READER_URL + comicId + "/" + id))
                }
            }
            book.episodes = list
            Log.d("jComics", "bilibili manga: " + book.bookTitle + " has " + list.size + " chapters")
        } catch (e: Exception) {
            Log.e("jComics", "bilibili manga: cannot read the result of the comic page", e)
            if (book.bookSynopsis == null) {
                book.bookSynopsis = "Cannot load page from Internet"
            }
        }
    }

    companion object {
        /**
         * The result of a script is a json string, it has to be unwrapped before it can be read.
         */
        private fun rawJson(result: String): String {
            val value = JSONTokener(result).nextValue()
            return if (value is String) value else result
        }

        private val READY_SCRIPT = """
            (function () {
              var els = document.querySelectorAll('*');
              for (var i = 0; i < els.length; i++) {
                var vm = els[i].__vue__;
                if (!vm) continue;
                var list = null;
                try { list = vm.episodeList; } catch (e) { continue; }
                if (!list || !list.length || !list[0] || !list[0].length) continue;
                if (list[0][0].id === undefined) continue;
                return true;
              }
              return false;
            })()
        """

        private val RESULT_SCRIPT = """
            (function () {
              var els = document.querySelectorAll('*');
              var episodes = [];
              for (var i = 0; i < els.length; i++) {
                var vm = els[i].__vue__;
                if (!vm) continue;
                var list = null;
                try { list = vm.episodeList; } catch (e) { continue; }
                if (!list || !list.length || !list[0] || !list[0].length) continue;
                if (list[0][0].id === undefined) continue;
                for (var p = 0; p < list.length; p++) {
                  for (var j = 0; j < list[p].length; j++) {
                    var e = list[p][j];
                    episodes.push({
                      id: e.id,
                      ord: e.ord,
                      title: e.title || '',
                      short: e._shortTitle || '',
                      locked: !!e.isLocked
                    });
                  }
                }
                break;
              }
              var titleEl = document.querySelector('.manga-title') || document.querySelector('h1');
              var title = titleEl ? titleEl.innerText.trim() : '';
              var cut = title.indexOf(' - ');
              if (cut > 0) title = title.substring(0, cut);
              var coverEl = document.querySelector('.manga-cover img');
              var introEl = document.querySelector('.introduction-text');
              return JSON.stringify({
                title: title,
                cover: coverEl ? coverEl.src : '',
                intro: introEl ? introEl.innerText.trim() : '',
                episodes: episodes
              });
            })()
        """
    }
}

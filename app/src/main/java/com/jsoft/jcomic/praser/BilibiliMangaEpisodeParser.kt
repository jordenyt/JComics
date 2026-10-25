package com.jsoft.jcomic.praser

import android.content.Context
import android.util.Log
import com.jsoft.jcomic.helper.BilibiliManga
import com.jsoft.jcomic.helper.EpisodeDTO
import com.jsoft.jcomic.helper.MangaWebView
import org.json.JSONArray
import org.json.JSONTokener
import java.net.URL

/**
 * Parser for a chapter of manga.bilibili.com (哔哩哔哩漫画).
 *
 * The pages of a chapter are not in the html, the site loads them with a signed api and gives every
 * page a token that is only valid for a short time. The reader of the site is therefore opened in a
 * web view outside of the screen, the pages are read from the state of the site. The web view pages
 * through the chapter so that the site loads every page - without a login of the site only the
 * first pages of a chapter are loaded.
 */
class BilibiliMangaEpisodeParser(
    episode: EpisodeDTO,
    listener: EpisodeParserListener,
    private val context: Context
) : EpisodeParser(episode, listener, "UTF-8") {

    override fun getURLResponse(url: URL, encoding: String): ArrayList<String> {
        // The content of the page is read by the web view, a http request is not needed.
        return arrayListOf("")
    }

    override fun getEpisodeFromUrlResult(result: List<String>) {
        val ids = BilibiliManga.episodeIds(episode.episodeUrl)
        if (ids == null) {
            Log.e("jComics", "bilibili manga: cannot read the chapter id from " + episode.episodeUrl)
            return
        }
        val comicId = ids.first
        val episodeId = ids.second

        postponeEpisodeResult()
        MangaWebView(context).load(
            BilibiliManga.READER_URL + comicId + "/" + episodeId,
            READY_SCRIPT,
            RESULT_SCRIPT,
            STEP_SCRIPT,
            timeoutMs = 60000
        ) { result ->
            val urls = readResult(result)
            if (urls.isEmpty()) {
                Log.e("jComics", "bilibili manga: no image found for " + episode.episodeUrl)
            } else {
                episode.imageUrl = urls
                episode.pageCount = urls.size
            }
            finishEpisodeResult()
        }
    }

    private fun readResult(result: String?): ArrayList<String> {
        val urls = ArrayList<String>()
        if (result.isNullOrEmpty()) return urls
        try {
            val value = JSONTokener(result).nextValue()
            val text = if (value is String) value else result
            val array = JSONArray(text)
            for (i in 0 until array.length()) {
                val url = array.optString(i)
                if (url.isNotEmpty()) {
                    urls.add(url)
                }
            }
        } catch (e: Exception) {
            Log.e("jComics", "bilibili manga: cannot read the result of the reader", e)
        }
        return urls
    }

    companion object {
        private val READY_SCRIPT = """
            (function () {
              var els = document.querySelectorAll('*');
              for (var i = 0; i < els.length; i++) {
                var vm = els[i].__vue__;
                if (!vm) continue;
                try {
                  var list = vm.episodeReaderImages;
                  if (list && list.length) return true;
                } catch (e) {}
              }
              return false;
            })()
        """

        /**
         * Pages the site through the chapter, every page that is shown is loaded by the site.
         */
        private val STEP_SCRIPT = """
            (function () {
              var els = document.querySelectorAll('*');
              var reader = null;
              var viewport = null;
              for (var i = 0; i < els.length; i++) {
                var vm = els[i].__vue__;
                if (!vm) continue;
                if (!reader) {
                  try {
                    if (vm.episodeReaderImages && vm.episodeReaderImages.length) reader = vm;
                  } catch (e) {}
                }
                if (!viewport) {
                  try {
                    if (vm.primaryImage !== undefined && typeof vm.goNextPage === 'function') viewport = vm;
                  } catch (e) {}
                }
              }
              if (!reader || !viewport) return false;
              var list = reader.episodeReaderImages;
              var perPage = 1;
              try { if (reader.isInDoublePageMode) perPage = 2; } catch (e) {}
              var pages = Math.ceil(list.length / perPage);
              if (viewport.pageIndex + perPage < pages) viewport.goNextPage();
              return true;
            })()
        """

        private val RESULT_SCRIPT = """
            (function () {
              var els = document.querySelectorAll('*');
              var urls = [];
              for (var i = 0; i < els.length; i++) {
                var vm = els[i].__vue__;
                if (!vm) continue;
                var list = null;
                try { list = vm.episodeReaderImages; } catch (e) { continue; }
                if (!list || !list.length) continue;
                for (var j = 0; j < list.length; j++) {
                  var item = list[j];
                  if (item._completeUrl) {
                    urls.push(item._completeUrl);
                  } else if (item._url && item._token) {
                    urls.push(item._url + '?token=' + item._token + '&code=' + (item._comicCode || 'DanmakuInfo'));
                  }
                }
                break;
              }
              return JSON.stringify(urls);
            })()
        """
    }
}

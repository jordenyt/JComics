package com.jsoft.jcomic.helper

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * The data of manga.bilibili.com is only handed out by a signed api, the javascript of the site is
 * the only thing that can read it. This helper loads a page of the site in a web view outside of the
 * screen and reads the result of a javascript script from it.
 *
 * The script that checks if the page is ready is called again and again until it is true or the
 * timeout is reached, the script of the step is called between two checks and can be used to make
 * the site do work, for example to page through a chapter.
 */
class MangaWebView(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private var done = false

    @SuppressLint("SetJavaScriptEnabled")
    fun load(
        url: String,
        readyScript: String,
        resultScript: String,
        stepScript: String? = null,
        timeoutMs: Long = 60000,
        callback: (String?) -> Unit
    ) {
        val view = WebView(context)
        view.settings.javaScriptEnabled = true
        view.settings.domStorageEnabled = true
        view.settings.userAgentString = USER_AGENT
        webView = view
        attach(view)

        val started = System.currentTimeMillis()
        view.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                check(view, readyScript, resultScript, stepScript, started, timeoutMs, callback)
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && request.isForMainFrame) {
                    Log.e("jComics", "MangaWebView could not load " + url)
                    finish(callback, null)
                }
            }

            @Suppress("DEPRECATION")
            override fun onReceivedError(
                view: WebView,
                errorCode: Int,
                description: String,
                failingUrl: String
            ) {
                // Android 5 calls this method instead of the one above.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    Log.e("jComics", "MangaWebView could not load " + url + " " + description)
                    finish(callback, null)
                }
            }
        }
        view.loadUrl(url)
    }

    private fun check(
        view: WebView,
        readyScript: String,
        resultScript: String,
        stepScript: String?,
        started: Long,
        timeoutMs: Long,
        callback: (String?) -> Unit
    ) {
        if (done) return
        view.evaluateJavascript(readyScript) { value ->
            if (done) return@evaluateJavascript
            if (value == "true") {
                readResult(view, resultScript, callback)
                return@evaluateJavascript
            }
            if (stepScript != null) {
                view.evaluateJavascript(stepScript, null)
            }
            if (System.currentTimeMillis() - started > timeoutMs) {
                readResult(view, resultScript, callback)
                return@evaluateJavascript
            }
            handler.postDelayed({
                check(view, readyScript, resultScript, stepScript, started, timeoutMs, callback)
            }, STEP_DELAY)
        }
    }

    private fun readResult(view: WebView, resultScript: String, callback: (String?) -> Unit) {
        view.evaluateJavascript(resultScript) { value ->
            finish(callback, value)
        }
    }

    private fun finish(callback: (String?) -> Unit, value: String?) {
        if (done) return
        done = true
        val view = webView
        webView = null
        try {
            view?.stopLoading()
            (view?.parent as? ViewGroup)?.removeView(view)
            view?.destroy()
        } catch (e: Exception) {
            Log.e("jComics", "Exception caught in MangaWebView", e)
        }
        callback(value)
    }

    /**
     * The web view is put next to the window, it still renders and runs its javascript but the
     * user does not see it.
     */
    private fun attach(view: WebView) {
        try {
            val activity = context as? Activity ?: return
            val group = activity.window?.decorView as? ViewGroup ?: return
            val metrics = activity.resources.displayMetrics
            val params = ViewGroup.LayoutParams(metrics.widthPixels, metrics.heightPixels)
            view.layoutParams = params
            view.translationX = -metrics.widthPixels * 2f
            group.addView(view, params)
        } catch (e: Exception) {
            Log.e("jComics", "Exception caught in MangaWebView.attach", e)
        }
    }

    companion object {
        private const val STEP_DELAY = 400L

        // The site only serves the pages of the reader to a desktop browser.
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Safari/537.36"
    }
}

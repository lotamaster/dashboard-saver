package com.lotamaster.dreampage

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * A WebView set up the same way for the screensaver and for the sign-in
 * screen, so a login made in one is seen by the other (they share cookies
 * and local storage).
 *
 * It stays on the configured site: links to other hosts are ignored.
 * If the page fails to load, it tries again every 30 seconds.
 */
@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
class PageView(context: Context, private val home: String) : WebView(context) {

    private val retry = Handler(Looper.getMainLooper())
    private val homeHost = Uri.parse(home).host

    init {
        setBackgroundColor(Color.BLACK)
        isFocusable = true
        isFocusableInTouchMode = true
        with(settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = false
            setSupportMultipleWindows(false)
            setSupportZoom(false)
            builtInZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
        }
        CookieManager.getInstance().setAcceptCookie(true)

        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                // true = don't load. Same host stays in the view; anything else is dropped.
                return request.url.host != homeHost
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) scheduleRetry()
            }
        }
    }

    fun start() {
        loadUrl(home)
    }

    private fun scheduleRetry() {
        retry.removeCallbacksAndMessages(null)
        retry.postDelayed({ loadUrl(home) }, RETRY_MS)
    }

    fun stop() {
        retry.removeCallbacksAndMessages(null)
        CookieManager.getInstance().flush()
        stopLoading()
        destroy()
    }

    private companion object {
        const val RETRY_MS = 30_000L
    }
}

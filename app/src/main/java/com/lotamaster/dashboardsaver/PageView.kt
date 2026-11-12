package com.lotamaster.dashboardsaver

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlin.random.Random

/**
 * A WebView set up the same way for the screensaver and for the sign-in
 * screen, so a login made in one is seen by the other (they share cookies
 * and local storage).
 *
 * It stays on the configured site plus any allowed hosts: other links are
 * ignored. If the page fails to load, it retries after retrySeconds.
 */
@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
class PageView(context: Context, private val s: Settings) : WebView(context) {

    private val handler = Handler(Looper.getMainLooper())
    private val retry = Runnable { loadUrl(s.url) }
    private val reload = object : Runnable {
        override fun run() {
            reload()
            handler.postDelayed(this, s.reloadMinutes * 60_000L)
        }
    }
    private val shift = object : Runnable {
        override fun run() {
            val max = SHIFT_DP * resources.displayMetrics.density
            translationX = Random.nextFloat() * 2 * max - max
            translationY = Random.nextFloat() * 2 * max - max
            handler.postDelayed(this, SHIFT_EVERY_MS)
        }
    }

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
            loadWithOverviewMode = s.zoomPercent == 100
        }
        if (s.zoomPercent != 100) setInitialScale(s.zoomPercent)
        CookieManager.getInstance().setAcceptCookie(true)

        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                // true = don't load. Allowed hosts stay in the view; anything else is dropped.
                return !Prefs.hostAllowed(request.url.host, s)
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) {
                    handler.removeCallbacks(retry)
                    handler.postDelayed(retry, s.retrySeconds * 1000L)
                }
            }
        }
    }

    /**
     * Loads the page. The periodic reload and the burn-in shift are only for
     * the screensaver, not the interactive sign-in screen.
     */
    fun start(screensaver: Boolean) {
        loadUrl(s.url)
        if (!screensaver) return
        if (s.reloadMinutes > 0) handler.postDelayed(reload, s.reloadMinutes * 60_000L)
        if (s.shiftPixels) handler.post(shift)
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        CookieManager.getInstance().flush()
        stopLoading()
        destroy()
    }

    private companion object {
        const val SHIFT_DP = 6f
        const val SHIFT_EVERY_MS = 2 * 60_000L
    }
}

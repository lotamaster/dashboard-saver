package com.lotamaster.dashboardsaver

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlin.random.Random

/**
 * A WebView set up the same way for the screensaver and for the sign-in
 * screen, so a login made in one is seen by the other (they share cookies
 * and local storage).
 *
 * It stays on the configured site plus any allowed hosts: links elsewhere are
 * ignored (embedded frames from other hosts still load). If the page fails to
 * load, or the server answers with a 5xx error, it retries after retrySeconds.
 */
@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
class PageView(context: Context, private val s: Settings) : WebView(context) {

    /**
     * Called when the WebView's renderer has died (crashed or killed for
     * memory). This view is unusable afterwards: the owner must stop() it and
     * replace it with a new one.
     */
    var onRendererGone: (() -> Unit)? = null

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
                // Embedded frames (cameras, maps, videos) may come from anywhere.
                if (!request.isForMainFrame) return false
                // true = don't load. Allowed hosts stay in the view; anything else is dropped.
                return !Prefs.hostAllowed(request.url.host, s)
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) scheduleRetry()
            }

            // A server that is restarting often answers 502/503 through its proxy.
            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                if (request.isForMainFrame && response.statusCode >= 500) scheduleRetry()
            }

            // Never proceed past a bad certificate, but say so instead of showing black.
            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                handler.cancel()
                Log.w(TAG, "certificate error ${error.primaryError} for ${error.url}")
                // A bad certificate on some other host's image or script just
                // drops that resource; only the page's own host replaces the page.
                val errorHost = Uri.parse(error.url).host
                if (errorHost != Uri.parse(view.url ?: s.url).host && errorHost != Uri.parse(s.url).host) return
                val host = TextUtils.htmlEncode(errorHost ?: error.url)
                val msg = TextUtils.htmlEncode(context.getString(R.string.ssl_error))
                loadDataWithBaseURL(
                    null,
                    "<body style=\"background:#000;color:#ccc;font:24px sans-serif;display:grid;place-items:center;height:90vh;margin:0\">" +
                        "<p>$msg<br><b>$host</b></p></body>",
                    "text/html", "utf-8", null,
                )
                scheduleRetry()
            }

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                Log.w(TAG, "WebView renderer gone (crashed: ${detail.didCrash()}); recreating")
                handler.removeCallbacksAndMessages(null)
                onRendererGone?.invoke()
                return true
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

    private fun scheduleRetry() {
        handler.removeCallbacks(retry)
        handler.postDelayed(retry, s.retrySeconds * 1000L)
    }

    fun stop() {
        // Stop loading first: it can fire onReceivedError, which would queue a retry.
        stopLoading()
        handler.removeCallbacksAndMessages(null)
        CookieManager.getInstance().flush()
        destroy()
    }

    private companion object {
        const val TAG = "DashboardSaver"
        const val SHIFT_DP = 6f
        const val SHIFT_EVERY_MS = 2 * 60_000L
    }
}

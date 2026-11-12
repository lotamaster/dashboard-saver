package com.lotamaster.dashboardsaver

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.view.Gravity
import android.widget.TextView

/** The screensaver: the saved page, full screen. Any button press ends it. */
class DashboardSaverDream : DreamService() {

    private var page: PageView? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val s = Prefs.load(this)
        isInteractive = false
        isFullscreen = true
        isScreenBright = !s.dim

        if (Prefs.isValidUrl(s.url)) {
            showPage(s)
        } else {
            setContentView(TextView(this).apply {
                setText(R.string.no_url)
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.BLACK)
                textSize = 24f
                gravity = Gravity.CENTER
            })
        }
    }

    private fun showPage(s: Settings) {
        page = PageView(this, s).also {
            it.onRendererGone = {
                // Drop the dead view now; bring up a new one shortly, so a page
                // that keeps crashing the renderer doesn't spin.
                page?.let { dead -> setContentView(blank()); dead.stop() }
                page = null
                handler.postDelayed({ showPage(s) }, RECREATE_DELAY_MS)
            }
            setContentView(it)
            it.start(screensaver = true)
        }
    }

    private fun blank() = TextView(this).apply { setBackgroundColor(Color.BLACK) }

    override fun onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null)
        page?.stop()
        page = null
        super.onDetachedFromWindow()
    }

    private companion object {
        const val RECREATE_DELAY_MS = 5_000L
    }
}

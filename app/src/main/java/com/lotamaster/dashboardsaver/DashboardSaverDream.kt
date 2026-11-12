package com.lotamaster.dashboardsaver

import android.graphics.Color
import android.service.dreams.DreamService
import android.view.Gravity
import android.widget.TextView

/** The screensaver: the saved page, full screen. Any button press ends it. */
class DashboardSaverDream : DreamService() {

    private var page: PageView? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val s = Prefs.load(this)
        isInteractive = false
        isFullscreen = true
        isScreenBright = !s.dim

        if (Prefs.isValidUrl(s.url)) {
            page = PageView(this, s).also {
                setContentView(it)
                it.start(screensaver = true)
            }
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

    override fun onDetachedFromWindow() {
        page?.stop()
        page = null
        super.onDetachedFromWindow()
    }
}

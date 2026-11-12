package com.lotamaster.dreampage

import android.graphics.Color
import android.service.dreams.DreamService
import android.view.Gravity
import android.widget.TextView

/** The screensaver: the saved page, full screen. Any button press ends it. */
class DreampageDream : DreamService() {

    private var page: PageView? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = true

        val url = Prefs.url(this)
        if (Prefs.isValid(url)) {
            page = PageView(this, url).also {
                setContentView(it)
                it.start()
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

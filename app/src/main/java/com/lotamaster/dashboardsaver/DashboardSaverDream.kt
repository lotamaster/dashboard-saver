package com.lotamaster.dashboardsaver

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.view.Gravity
import android.view.KeyEvent
import android.widget.TextView

/**
 * The screensaver: the saved page, full screen.
 *
 * Off (the default), any button ends it. With "interactive" on, the D-pad and
 * OK go to the page, and Back first asks the page to handle it through
 * window.dashboardSaver.onBack(), if the page defines it: true keeps the
 * screensaver up, anything else (or no answer within 500 ms) ends it. Every
 * other button still ends it. Home is handled by the system and always leaves.
 */
class DashboardSaverDream : DreamService() {

    private var page: PageView? = null
    private var interactive = false
    private val handler = Handler(Looper.getMainLooper())
    /** Bumped on every Back, so an answer to an earlier Back is ignored. */
    private var backSeq = 0

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val s = Prefs.load(this)
        interactive = s.interactive
        isInteractive = interactive
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
            if (interactive) it.requestFocus()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!interactive) return super.dispatchKeyEvent(event)
        val p = page
        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (p == null) { finish(); return true }
                if (!p.hasFocus()) p.requestFocus()
                // The page sees OK as Enter, the same key a keyboard sends.
                val e = if (event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)
                    KeyEvent(event.downTime, event.eventTime, event.action, KeyEvent.KEYCODE_ENTER,
                        event.repeatCount, event.metaState, event.deviceId, event.scanCode, event.flags, event.source)
                else event
                p.dispatchKeyEvent(e)
                true
            }
            KeyEvent.KEYCODE_BACK -> {
                // Act once, on the press; the release has nothing left to do.
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) back(p)
                true
            }
            else -> {
                // Volume, mute, media, input and app buttons: end the
                // screensaver, as they do with interactive off.
                finish()
                true
            }
        }
    }

    /** Lets the page handle Back; ends the screensaver if it didn't, or doesn't answer in time. */
    private fun back(p: PageView?) {
        if (p == null) { finish(); return }
        val seq = ++backSeq
        val giveUp = Runnable { if (seq == backSeq) finish() }
        handler.postDelayed(giveUp, BACK_TIMEOUT_MS)
        p.evaluateJavascript(BACK_JS) { result ->
            if (seq != backSeq) return@evaluateJavascript
            handler.removeCallbacks(giveUp)
            backSeq++
            if (result != "true") finish()
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
        const val BACK_TIMEOUT_MS = 500L
        const val BACK_JS = "(function(){try{return window.dashboardSaver?.onBack?.()===true}catch(e){return false}})()"
    }
}

package com.lotamaster.dreampage

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent

/**
 * The saved page, full screen and interactive, so you can sign in (the
 * screensaver can't take input). Back goes back a page, then closes.
 */
class PreviewActivity : Activity() {

    private lateinit var page: PageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        page = PageView(this, Prefs.url(this))
        setContentView(page)
        page.requestFocus()
        page.start()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && page.canGoBack()) {
            page.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        page.stop()
        super.onDestroy()
    }
}

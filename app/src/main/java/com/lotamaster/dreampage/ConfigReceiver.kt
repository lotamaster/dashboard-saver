package com.lotamaster.dreampage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Sets the URL from adb, for setups kept as code:
 *
 *   adb shell am broadcast -n com.lotamaster.dreampage/.ConfigReceiver \
 *     -a com.lotamaster.dreampage.action.CONFIGURE --es url https://example.com/
 *
 * The manifest requires the DUMP permission to send this, which the adb
 * shell has and ordinary apps can't get.
 */
class ConfigReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val url = intent.getStringExtra("url")?.trim()
        if (url != null && Prefs.isValid(url)) {
            Prefs.setUrl(context, url)
            resultCode = 1
            resultData = "url set"
        } else {
            resultCode = 2
            resultData = "missing or invalid --es url"
        }
        Log.i("Dreampage", resultData ?: "")
    }
}

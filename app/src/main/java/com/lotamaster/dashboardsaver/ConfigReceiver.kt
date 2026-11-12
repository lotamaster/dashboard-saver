package com.lotamaster.dashboardsaver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Configures DashboardSaver from adb, for setups kept as code. Extras that
 * aren't passed keep their current value:
 *
 *   adb shell am broadcast -n <package>/com.lotamaster.dashboardsaver.ConfigReceiver \
 *     -a <package>.action.CONFIGURE \
 *     --es url https://example.com/ --es allowed_hosts "auth.example.com,*.cdn.net" \
 *     --ei reload_minutes 60 --ei retry_seconds 30 --ei zoom_percent 100 \
 *     --ez dim false --ez shift_pixels true --ez interactive true --ez clear_data false
 *
 * The manifest requires the DUMP permission to send this, which the adb
 * shell has and ordinary apps can't get.
 */
class ConfigReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val errors = mutableListOf<String>()
        var s = Prefs.load(context)

        intent.getStringExtra(Prefs.URL)?.trim()?.let {
            if (Prefs.isValidUrl(it)) s = s.copy(url = it) else errors += "url must be a full http(s) URL"
        }
        intent.getStringExtra(Prefs.ALLOWED_HOSTS)?.let { s = s.copy(allowedHosts = Prefs.parseHosts(it)) }
        intInRange(intent, Prefs.RELOAD_MINUTES, Prefs.RELOAD_RANGE, errors)?.let { s = s.copy(reloadMinutes = it) }
        intInRange(intent, Prefs.RETRY_SECONDS, Prefs.RETRY_RANGE, errors)?.let { s = s.copy(retrySeconds = it) }
        intInRange(intent, Prefs.ZOOM_PERCENT, Prefs.ZOOM_RANGE, errors)?.let { s = s.copy(zoomPercent = it) }
        if (intent.hasExtra(Prefs.DIM)) s = s.copy(dim = intent.getBooleanExtra(Prefs.DIM, false))
        if (intent.hasExtra(Prefs.SHIFT_PIXELS)) s = s.copy(shiftPixels = intent.getBooleanExtra(Prefs.SHIFT_PIXELS, false))
        if (intent.hasExtra(Prefs.INTERACTIVE)) s = s.copy(interactive = intent.getBooleanExtra(Prefs.INTERACTIVE, false))

        if (errors.isEmpty()) {
            val before = Prefs.load(context)
            Prefs.save(context, s)
            val clear = intent.getBooleanExtra(CLEAR_DATA, false)
            if (clear) Prefs.clearSiteData()
            // Names only: a URL can carry a token, and logcat is not the place for it.
            val changed = changedKeys(before, s) + if (clear) listOf(CLEAR_DATA) else emptyList()
            resultCode = 1
            resultData = if (changed.isEmpty()) "saved: nothing changed" else "saved: " + changed.joinToString(", ")
        } else {
            resultCode = 2
            resultData = "nothing saved: " + errors.joinToString("; ")
        }
        Log.i(TAG, resultData ?: "")
    }

    private fun changedKeys(a: Settings, b: Settings): List<String> = buildList {
        if (a.url != b.url) add(Prefs.URL)
        if (a.allowedHosts != b.allowedHosts) add(Prefs.ALLOWED_HOSTS)
        if (a.reloadMinutes != b.reloadMinutes) add(Prefs.RELOAD_MINUTES)
        if (a.retrySeconds != b.retrySeconds) add(Prefs.RETRY_SECONDS)
        if (a.zoomPercent != b.zoomPercent) add(Prefs.ZOOM_PERCENT)
        if (a.dim != b.dim) add(Prefs.DIM)
        if (a.shiftPixels != b.shiftPixels) add(Prefs.SHIFT_PIXELS)
        if (a.interactive != b.interactive) add(Prefs.INTERACTIVE)
    }

    /** Null if the extra is absent or invalid (invalid also adds an error). */
    private fun intInRange(intent: Intent, key: String, range: IntRange, errors: MutableList<String>): Int? {
        if (!intent.hasExtra(key)) return null
        // --ei gives an Int; accept --es "60" too.
        val asInt = intent.getIntExtra(key, Int.MIN_VALUE)
        val value = if (asInt != Int.MIN_VALUE) asInt else intent.getStringExtra(key)?.trim()?.toIntOrNull()
        if (value == null || value !in range) {
            errors += "$key must be a number from ${range.first} to ${range.last}"
            return null
        }
        return value
    }

    private companion object {
        const val TAG = "DashboardSaver"
        const val CLEAR_DATA = "clear_data"
    }
}

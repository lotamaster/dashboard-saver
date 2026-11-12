package com.lotamaster.dashboardsaver

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebStorage

/** Everything DashboardSaver can be told, stored in one SharedPreferences file. */
data class Settings(
    val url: String,
    /** Extra hosts the page may navigate to (its own host is always allowed). "*.example.com" matches subdomains. */
    val allowedHosts: List<String>,
    /** Reload the page every N minutes while the screensaver runs; 0 = never. */
    val reloadMinutes: Int,
    /** Wait this long before retrying a page that failed to load. */
    val retrySeconds: Int,
    /** Page zoom; 100 = the page's own layout. */
    val zoomPercent: Int,
    /** Let the screen dim while the screensaver runs. */
    val dim: Boolean,
    /** Move the page a few pixels every couple of minutes, against burn-in. */
    val shiftPixels: Boolean,
)

object Prefs {
    private const val FILE = "dashboardsaver"

    const val URL = "url"
    const val ALLOWED_HOSTS = "allowed_hosts"
    const val RELOAD_MINUTES = "reload_minutes"
    const val RETRY_SECONDS = "retry_seconds"
    const val ZOOM_PERCENT = "zoom_percent"
    const val DIM = "dim"
    const val SHIFT_PIXELS = "shift_pixels"

    val RELOAD_RANGE = 0..1440
    val RETRY_RANGE = 5..3600
    val ZOOM_RANGE = 25..400

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(context: Context): Settings {
        val p = prefs(context)
        return Settings(
            url = p.getString(URL, null) ?: BuildConfig.DEFAULT_URL,
            allowedHosts = parseHosts(p.getString(ALLOWED_HOSTS, "") ?: ""),
            reloadMinutes = p.getInt(RELOAD_MINUTES, 0),
            retrySeconds = p.getInt(RETRY_SECONDS, 30),
            zoomPercent = p.getInt(ZOOM_PERCENT, 100),
            dim = p.getBoolean(DIM, false),
            shiftPixels = p.getBoolean(SHIFT_PIXELS, false),
        )
    }

    fun save(context: Context, s: Settings) {
        prefs(context).edit()
            .putString(URL, s.url)
            .putString(ALLOWED_HOSTS, s.allowedHosts.joinToString(","))
            .putInt(RELOAD_MINUTES, s.reloadMinutes)
            .putInt(RETRY_SECONDS, s.retrySeconds)
            .putInt(ZOOM_PERCENT, s.zoomPercent)
            .putBoolean(DIM, s.dim)
            .putBoolean(SHIFT_PIXELS, s.shiftPixels)
            .apply()
    }

    /** Only full http(s) URLs with a host are accepted. */
    fun isValidUrl(url: String): Boolean {
        val uri = Uri.parse(url.trim())
        return (uri.scheme == "https" || uri.scheme == "http") && !uri.host.isNullOrEmpty()
    }

    /** "a.com, *.b.com" → ["a.com", "*.b.com"], lowercased, blanks dropped. */
    fun parseHosts(raw: String): List<String> =
        raw.split(',', ' ', '\n').map { it.trim().lowercase() }.filter { it.isNotEmpty() }.distinct()

    fun hostAllowed(host: String?, s: Settings): Boolean {
        if (host == null) return false
        val h = host.lowercase()
        if (h == Uri.parse(s.url).host?.lowercase()) return true
        return s.allowedHosts.any { entry ->
            if (entry.startsWith("*.")) h.endsWith(entry.substring(1)) || h == entry.substring(2)
            else h == entry
        }
    }

    /** Signs out of everything: cookies and the pages' local storage. */
    fun clearSiteData() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
    }
}

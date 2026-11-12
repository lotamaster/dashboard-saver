package com.lotamaster.dreampage

import android.content.Context
import android.net.Uri

/** The one setting Dreampage has: which page to show. */
object Prefs {
    private const val FILE = "dreampage"
    private const val KEY_URL = "url"

    fun url(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_URL, "") ?: ""

    fun setUrl(context: Context, url: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY_URL, url).apply()
    }

    /** Only full http(s) URLs with a host are accepted. */
    fun isValid(url: String): Boolean {
        val uri = Uri.parse(url.trim())
        return (uri.scheme == "https" || uri.scheme == "http") && !uri.host.isNullOrEmpty()
    }
}

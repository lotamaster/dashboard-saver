package com.lotamaster.dashboardsaver

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings.ACTION_DREAM_SETTINGS
import android.provider.Settings.Secure
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/** Edit the settings, open the page to sign in, and see whether DashboardSaver is the active screensaver. */
class SettingsActivity : Activity() {

    private lateinit var url: EditText
    private lateinit var allowedHosts: EditText
    private lateinit var reloadMinutes: EditText
    private lateinit var retrySeconds: EditText
    private lateinit var zoomPercent: EditText
    private lateinit var dim: CheckBox
    private lateinit var shiftPixels: CheckBox
    private lateinit var interactive: CheckBox
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        url = view(R.id.url)
        allowedHosts = view(R.id.allowed_hosts)
        reloadMinutes = view(R.id.reload_minutes)
        retrySeconds = view(R.id.retry_seconds)
        zoomPercent = view(R.id.zoom_percent)
        dim = view(R.id.dim)
        shiftPixels = view(R.id.shift_pixels)
        interactive = view(R.id.interactive)
        status = view(R.id.status)

        // One row of buttons on a TV or tablet; stacked on a phone, where they
        // wouldn't fit side by side.
        if (resources.configuration.screenWidthDp < 600) {
            view<LinearLayout>(R.id.buttons).orientation = LinearLayout.VERTICAL
            for (id in listOf(R.id.open, R.id.system, R.id.clear)) {
                (view<Button>(id).layoutParams as LinearLayout.LayoutParams).marginStart = 0
            }
        }
        view<Button>(R.id.save).setOnClickListener { save() }
        view<Button>(R.id.open).setOnClickListener {
            if (save()) startActivity(Intent(this, PreviewActivity::class.java))
        }
        view<Button>(R.id.system).setOnClickListener { openSystemSettings() }
        view<Button>(R.id.clear).setOnClickListener { confirmClear() }

        val dream = ComponentName(this, DashboardSaverDream::class.java).flattenToShortString()
        view<TextView>(R.id.help).text = getString(R.string.adb_help, dream)
    }

    override fun onResume() {
        super.onResume()
        // Re-read every time: settings may have changed over adb while this
        // screen was in the background, and saving stale fields would undo that.
        show(Prefs.load(this))
        status.setText(currentStatus())
    }

    private fun <T : View> view(id: Int): T =
        checkNotNull(findViewById<T>(id)) { "missing view $id" }

    // Plain ASCII digits on purpose: they are parsed back with toIntOrNull().
    @SuppressLint("SetTextI18n")
    private fun show(s: Settings) {
        url.setText(s.url)
        allowedHosts.setText(s.allowedHosts.joinToString(", "))
        reloadMinutes.setText(s.reloadMinutes.toString())
        retrySeconds.setText(s.retrySeconds.toString())
        zoomPercent.setText(s.zoomPercent.toString())
        dim.isChecked = s.dim
        shiftPixels.isChecked = s.shiftPixels
        interactive.isChecked = s.interactive
    }

    /** Validates every field; saves only if all are good. */
    private fun save(): Boolean {
        val u = url.text.toString().trim()
        if (!Prefs.isValidUrl(u)) {
            url.error = getString(R.string.bad_url)
            url.requestFocus()
            return false
        }
        val reload = number(reloadMinutes, Prefs.RELOAD_RANGE) ?: return false
        val retry = number(retrySeconds, Prefs.RETRY_RANGE) ?: return false
        val zoom = number(zoomPercent, Prefs.ZOOM_RANGE) ?: return false

        Prefs.save(
            this,
            Settings(
                url = u,
                allowedHosts = Prefs.parseHosts(allowedHosts.text.toString()),
                reloadMinutes = reload,
                retrySeconds = retry,
                zoomPercent = zoom,
                dim = dim.isChecked,
                shiftPixels = shiftPixels.isChecked,
                interactive = interactive.isChecked,
            ),
        )
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
        return true
    }

    private fun number(field: EditText, range: IntRange): Int? {
        val n = field.text.toString().trim().toIntOrNull()
        if (n == null || n !in range) {
            field.error = getString(R.string.bad_number, range.first, range.last)
            field.requestFocus()
            return null
        }
        return n
    }

    private fun confirmClear() {
        AlertDialog.Builder(this)
            .setMessage(R.string.clear_confirm)
            .setPositiveButton(R.string.clear) { _, _ ->
                Prefs.clearSiteData()
                Toast.makeText(this, R.string.cleared, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun openSystemSettings() {
        try {
            startActivity(Intent(ACTION_DREAM_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, R.string.no_dream_settings, Toast.LENGTH_LONG).show()
        }
    }

    /** Reads the hidden "screensaver_components" setting; some devices refuse. */
    private fun currentStatus(): Int = try {
        val raw = Secure.getString(contentResolver, "screensaver_components")
        val ours = ComponentName(this, DashboardSaverDream::class.java)
        val active = raw?.split(',')?.mapNotNull { ComponentName.unflattenFromString(it) }
        if (active == null) R.string.status_unknown
        else if (ours in active) R.string.status_active
        else R.string.status_inactive
    } catch (e: SecurityException) {
        R.string.status_unknown
    }
}

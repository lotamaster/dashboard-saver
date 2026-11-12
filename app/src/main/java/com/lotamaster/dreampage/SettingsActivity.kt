package com.lotamaster.dreampage

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

/** Pick the page, open it to sign in, and see whether Dreampage is the active screensaver. */
class SettingsActivity : Activity() {

    private lateinit var url: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        url = view(R.id.url)
        status = view(R.id.status)

        url.setText(Prefs.url(this))
        url.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE) save() else false
        }
        view<Button>(R.id.save).setOnClickListener { save() }
        view<Button>(R.id.open).setOnClickListener {
            if (save()) startActivity(Intent(this, PreviewActivity::class.java))
        }
        view<Button>(R.id.system).setOnClickListener { openSystemSettings() }
    }

    override fun onResume() {
        super.onResume()
        status.setText(currentStatus())
    }

    private fun <T : android.view.View> view(id: Int): T =
        checkNotNull(findViewById<T>(id)) { "missing view $id" }

    private fun save(): Boolean {
        val value = url.text.toString().trim()
        if (!Prefs.isValid(value)) {
            url.error = getString(R.string.bad_url)
            return false
        }
        Prefs.setUrl(this, value)
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
        return true
    }

    private fun openSystemSettings() {
        try {
            startActivity(Intent(Settings.ACTION_DREAM_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, R.string.no_dream_settings, Toast.LENGTH_LONG).show()
        }
    }

    /** Reads the hidden "screensaver_components" setting; some devices refuse. */
    private fun currentStatus(): Int = try {
        val raw = Settings.Secure.getString(contentResolver, "screensaver_components")
        val ours = ComponentName(this, DreampageDream::class.java)
        val active = raw?.split(',')?.mapNotNull { ComponentName.unflattenFromString(it) }
        if (active == null) R.string.status_unknown
        else if (ours in active) R.string.status_active
        else R.string.status_inactive
    } catch (e: SecurityException) {
        R.string.status_unknown
    }
}

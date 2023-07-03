package us.huseli.umpc.mpd.engine

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import us.huseli.umpc.Constants.PREF_HOSTNAME
import us.huseli.umpc.Constants.PREF_OUTPUTS_ENABLED
import us.huseli.umpc.Constants.PREF_PASSWORD
import us.huseli.umpc.Constants.PREF_PORT
import us.huseli.umpc.Constants.PREF_STREAMING_URL
import us.huseli.umpc.data.MPDCredentials

class SettingsEngine(context: Context) : SharedPreferences.OnSharedPreferenceChangeListener {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val enabledOutputs = MutableStateFlow<Set<Int>>(emptySet()).apply {
        preferences.getStringSet(PREF_OUTPUTS_ENABLED, emptySet())?.let { outputs ->
            value = outputs.map { it.toInt() }.toSet()
        }
    }

    val hostname = MutableStateFlow(preferences.getString(PREF_HOSTNAME, "") ?: "")
    val password = MutableStateFlow(preferences.getString(PREF_PASSWORD, "") ?: "")
    val port = MutableStateFlow(preferences.getInt(PREF_PORT, 6600))
    val streamingUrl = MutableStateFlow(preferences.getString(PREF_STREAMING_URL, null))
    val credentials = MutableStateFlow(MPDCredentials(hostname.value, port.value, password.value))

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    fun setOutputEnabled(id: Int, isEnabled: Boolean) {
        enabledOutputs.value = enabledOutputs.value.toMutableSet().apply {
            if (isEnabled) add(id) else remove(id)
        }
    }

    fun save() {
        preferences
            .edit()
            .putString(PREF_HOSTNAME, hostname.value)
            .putString(PREF_PASSWORD, password.value)
            .putInt(PREF_PORT, port.value)
            .putString(PREF_STREAMING_URL, streamingUrl.value)
            .putStringSet(PREF_OUTPUTS_ENABLED, enabledOutputs.value.map { it.toString() }.toSet())
            .apply()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PREF_HOSTNAME -> preferences.getString(key, null)?.let { hostname.value = it }
            PREF_PASSWORD -> preferences.getString(key, null)?.let { password.value = it }
            PREF_PORT -> port.value = preferences.getInt(key, 6600)
            PREF_OUTPUTS_ENABLED -> preferences.getStringSet(key, emptySet())?.let { outputs ->
                enabledOutputs.value = outputs.map { it.toInt() }.toSet()
            }
            PREF_STREAMING_URL -> streamingUrl.value = preferences.getString(key, null)
        }
        credentials.value = MPDCredentials(hostname.value, port.value, password.value)
    }
}

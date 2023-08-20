package us.huseli.umpc.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.Constants.PREF_HOSTNAME
import us.huseli.umpc.Constants.PREF_OUTPUTS_ENABLED
import us.huseli.umpc.Constants.PREF_PASSWORD
import us.huseli.umpc.Constants.PREF_PORT
import us.huseli.umpc.Constants.PREF_STREAMING_URL
import us.huseli.umpc.data.MPDServerCredentials
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) : SharedPreferences.OnSharedPreferenceChangeListener {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val _hostname = MutableStateFlow(preferences.getString(PREF_HOSTNAME, "") ?: "")
    private val _password = MutableStateFlow(preferences.getString(PREF_PASSWORD, "") ?: "")
    private val _port = MutableStateFlow(preferences.getInt(PREF_PORT, 6600))
    private val _streamingUrl = MutableStateFlow(preferences.getString(PREF_STREAMING_URL, null))
    private val _credentials =
        MutableStateFlow(MPDServerCredentials(_hostname.value, _port.value, _password.value))
    private val _enabledOutputs = MutableStateFlow<Set<Int>>(emptySet()).apply {
        preferences.getStringSet(PREF_OUTPUTS_ENABLED, emptySet())?.let { outputs ->
            value = outputs.map { it.toInt() }.toSet()
        }
    }

    val hostname = _hostname.asStateFlow()
    val password = _password.asStateFlow()
    val port = _port.asStateFlow()
    val streamingUrl = _streamingUrl.asStateFlow()
    val credentials = _credentials.asStateFlow()

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    fun save() {
        preferences
            .edit()
            .putString(PREF_HOSTNAME, _hostname.value)
            .putString(PREF_PASSWORD, _password.value)
            .putInt(PREF_PORT, _port.value)
            .putString(PREF_STREAMING_URL, _streamingUrl.value)
            .putStringSet(PREF_OUTPUTS_ENABLED, _enabledOutputs.value.map { it.toString() }.toSet())
            .apply()
        _credentials.value = MPDServerCredentials(_hostname.value, _port.value, _password.value)
    }

    fun setHostname(value: String) {
        _hostname.value = value
    }

    fun setPassword(value: String) {
        _password.value = value
    }

    fun setPort(value: Int) {
        _port.value = value
    }

    fun setStreamingUrl(value: String) {
        _streamingUrl.value = value
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PREF_HOSTNAME -> preferences.getString(key, null)?.let { _hostname.value = it }
            PREF_PASSWORD -> preferences.getString(key, null)?.let { _password.value = it }
            PREF_PORT -> _port.value = preferences.getInt(key, 6600)
            PREF_OUTPUTS_ENABLED -> preferences.getStringSet(key, emptySet())?.let { outputs ->
                _enabledOutputs.value = outputs.map { it.toInt() }.toSet()
            }
            PREF_STREAMING_URL -> _streamingUrl.value = preferences.getString(key, null)
        }
    }
}

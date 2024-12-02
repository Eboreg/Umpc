package us.huseli.umpc.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import us.huseli.umpc.Constants.PREF_FETCH_SPOTIFY_ALBUMART
import us.huseli.umpc.Constants.PREF_OUTPUTS_ENABLED
import us.huseli.umpc.Constants.PREF_SERVERS
import us.huseli.umpc.Constants.PREF_SERVER_IDX
import us.huseli.umpc.InstantAdapter
import us.huseli.umpc.data.MPDServerCredentials
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) : SharedPreferences.OnSharedPreferenceChangeListener {
    private val gson: Gson = GsonBuilder().registerTypeAdapter(Instant::class.java, InstantAdapter()).create()
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val _currentServerIdx = MutableStateFlow(preferences.getInt(PREF_SERVER_IDX, -1).takeIf { it > -1 })
    private val _servers = MutableStateFlow<List<MPDServerCredentials>>(emptyList())
    private val _enabledOutputs = MutableStateFlow<Set<Int>>(emptySet()).apply {
        preferences.getStringSet(PREF_OUTPUTS_ENABLED, emptySet())?.let { outputs ->
            value = outputs.map { it.toInt() }.toSet()
        }
    }
    private val _fetchSpotifyAlbumArt = MutableStateFlow(preferences.getBoolean(PREF_FETCH_SPOTIFY_ALBUMART, true))

    val currentServerIdx = _currentServerIdx.asStateFlow()
    val servers = _servers.asStateFlow()
    val currentServer = combine(_servers, _currentServerIdx) { servers, serverIdx ->
        serverIdx?.let { servers.getOrNull(it) }
    }
    val fetchSpotifyAlbumArt = _fetchSpotifyAlbumArt.asStateFlow()
    val streamingUrl = currentServer.map { it?.streamingUrl }

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
        loadServers()
    }

    fun addServer(server: MPDServerCredentials) {
        _servers.value += server
        _currentServerIdx.value = _servers.value.lastIndex
        save(PREF_SERVERS, PREF_SERVER_IDX)
    }

    fun deleteServer(index: Int) {
        _servers.value -= _servers.value[index]
        _currentServerIdx.value?.let {
            if (_servers.value.isEmpty()) _currentServerIdx.value = null
            else if (it >= _servers.value.size) _currentServerIdx.value = _servers.value.lastIndex
        }
        save(PREF_SERVERS, PREF_SERVER_IDX)
    }

    fun save() = save(PREF_OUTPUTS_ENABLED, PREF_SERVER_IDX)

    fun setFetchSpotifyAlbumArt(value: Boolean) {
        _fetchSpotifyAlbumArt.value = value
    }

    fun setServerIdx(value: Int) {
        _currentServerIdx.value = value
    }

    fun updateServer(index: Int, server: MPDServerCredentials) {
        _servers.value = _servers.value.toMutableList().apply { set(index, server) }
        save(PREF_SERVERS)
    }

    private fun loadServers() {
        val listType = object : TypeToken<List<MPDServerCredentials>>() {}

        gson.fromJson(preferences.getString(PREF_SERVERS, "[]"), listType)?.let { servers ->
            _servers.value = servers
        }
    }

    private fun save(vararg keys: String) {
        var editor = preferences.edit()
        if (keys.contains(PREF_OUTPUTS_ENABLED))
            editor = editor.putStringSet(PREF_OUTPUTS_ENABLED, _enabledOutputs.value.map { it.toString() }.toSet())
        if (keys.contains(PREF_SERVER_IDX)) editor = editor.putInt(PREF_SERVER_IDX, _currentServerIdx.value ?: -1)
        if (keys.contains(PREF_SERVERS)) editor = editor.putString(PREF_SERVERS, gson.toJson(_servers.value))
        editor.apply()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PREF_OUTPUTS_ENABLED -> preferences.getStringSet(key, emptySet())?.let { outputs ->
                _enabledOutputs.value = outputs.map { it.toInt() }.toSet()
            }

            PREF_SERVERS -> loadServers()
            PREF_SERVER_IDX -> preferences.getInt(key, -1).takeIf { it > -1 }?.let {
                setServerIdx(it)
            }

            PREF_FETCH_SPOTIFY_ALBUMART -> _fetchSpotifyAlbumArt.value = preferences.getBoolean(key, true)
        }
    }
}

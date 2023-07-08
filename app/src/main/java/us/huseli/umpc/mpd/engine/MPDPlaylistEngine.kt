package us.huseli.umpc.mpd.engine

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.Constants.PREF_DYNAMIC_PLAYLISTS
import us.huseli.umpc.InstantAdapter
import us.huseli.umpc.data.DynamicPlaylist
import us.huseli.umpc.data.DynamicPlaylistFilter
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDPlaylist
import us.huseli.umpc.data.MPDPlaylistWithSongs
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.mpd.MPDRepository
import us.huseli.umpc.mpd.OnMPDChangeListener
import us.huseli.umpc.mpd.response.MPDResponse
import java.time.Instant

class MPDPlaylistEngine(private val repo: MPDRepository, context: Context) :
    OnMPDChangeListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val storedPlaylistsWithSongs = MutableStateFlow<List<MPDPlaylistWithSongs>>(emptyList())
    private val _storedPlaylists = MutableStateFlow<List<MPDPlaylist>>(emptyList())
    private val _dynamicPlaylists = MutableStateFlow<List<DynamicPlaylist>>(emptyList())
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Instant::class.java, InstantAdapter())
        .create()

    val storedPlaylists = _storedPlaylists.asStateFlow()
    val dynamicPlaylists = _dynamicPlaylists.asStateFlow()

    init {
        repo.registerOnMPDChangeListener(this)
        preferences.registerOnSharedPreferenceChangeListener(this)
        loadDynamicPlaylists()
    }

    fun addAlbumToStoredPlaylist(album: MPDAlbum, playlistName: String, onFinish: (MPDResponse) -> Unit) =
        repo.client.enqueue("searchaddpl", listOf(playlistName, album.searchFilter.toString()), onFinish)

    fun createDynamicPlaylist(name: String, filter: DynamicPlaylistFilter, shuffle: Boolean) {
        val playlist = DynamicPlaylist(name, filter, shuffle)

        _dynamicPlaylists.value = _dynamicPlaylists.value.toMutableList().apply { add(playlist) }
        saveDynamicPlaylists()
    }

    fun deleteDynamicPlaylist(playlist: DynamicPlaylist) {
        _dynamicPlaylists.value = _dynamicPlaylists.value.toMutableList().apply {
            remove(playlist)
        }
        saveDynamicPlaylists()
    }

    fun deleteStoredPlaylist(playlistName: String, onFinish: (MPDResponse) -> Unit) =
        repo.client.enqueue("rm", playlistName, onFinish)

    fun enqueueStoredPlaylistAndPlay(playlistName: String) {
        /** Clears the queue, loads playlist into it, and plays from position 0. */
        repo.client.enqueue("clear") {
            repo.client.enqueue("load", playlistName) {
                repo.client.enqueue("play 0")
            }
        }
    }

    fun fetchStoredPlaylistSongs(playlistName: String, onFinish: (List<MPDSong>) -> Unit) {
        repo.client.enqueue("listplaylistinfo", playlistName) { response ->
            onFinish(response.extractSongs())
        }
    }

    fun loadStoredPlaylists() {
        fetchStoredPlaylists { _storedPlaylists.value = it }
    }

    fun renameStoredPlaylist(name: String, newName: String, onFinish: (Boolean) -> Unit) {
        repo.client.enqueue("rename", listOf(name, newName)) { response ->
            onFinish(response.isSuccess)
        }
    }

    fun updateDynamicPlaylist(
        playlist: DynamicPlaylist,
        name: String,
        filter: DynamicPlaylistFilter,
        shuffle: Boolean,
    ) {
        _dynamicPlaylists.value = _dynamicPlaylists.value.toMutableList().apply {
            remove(playlist)
            add(DynamicPlaylist(name, filter, shuffle))
        }
        saveDynamicPlaylists()
    }

    private fun fetchStoredPlaylists(onFinish: (List<MPDPlaylist>) -> Unit) {
        repo.client.enqueue("listplaylists") { response ->
            onFinish(response.extractPlaylists())
        }
    }

    private fun loadDynamicPlaylists() {
        val listType = object : TypeToken<List<DynamicPlaylist>>() {}

        gson.fromJson(preferences.getString(PREF_DYNAMIC_PLAYLISTS, "[]"), listType)?.let {
            _dynamicPlaylists.value = it
        }
    }

    private fun loadStoredPlaylistsWithSongs() {
        fetchStoredPlaylists { playlists ->
            playlists.forEach { playlist ->
                fetchStoredPlaylistSongs(playlist.name) { songs ->
                    val pws = MPDPlaylistWithSongs(playlist, songs)

                    storedPlaylistsWithSongs.value = storedPlaylistsWithSongs.value
                        .toMutableList()
                        .apply {
                            if (!contains(pws)) {
                                removeIf { it.playlist == playlist }
                                add(pws)
                            }
                        }.sortedByDescending { it.playlist.lastModified }
                }
            }
        }
    }

    private fun saveDynamicPlaylists() {
        val json = gson.toJson(_dynamicPlaylists.value)

        preferences
            .edit()
            .putString(PREF_DYNAMIC_PLAYLISTS, json)
            .apply()
    }

    override fun onMPDChanged(subsystems: List<String>) {
        if (subsystems.contains("stored_playlist")) {
            loadStoredPlaylists()
            loadStoredPlaylistsWithSongs()
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == PREF_DYNAMIC_PLAYLISTS) loadDynamicPlaylists()
    }
}

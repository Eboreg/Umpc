package us.huseli.umpc.mpd.engine

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.Constants.PREF_ACTIVE_DYNAMIC_PLAYLIST
import us.huseli.umpc.Constants.PREF_DYNAMIC_PLAYLISTS
import us.huseli.umpc.DynamicPlaylistState
import us.huseli.umpc.InstantAdapter
import us.huseli.umpc.data.DynamicPlaylist
import us.huseli.umpc.data.DynamicPlaylistFilter
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDPlaylist
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.mpd.MPDRepository
import us.huseli.umpc.mpd.OnMPDChangeListener
import us.huseli.umpc.mpd.response.MPDMapResponse
import java.time.Instant

class MPDPlaylistEngine(
    private val repo: MPDRepository,
    private val context: Context,
    private val ioScope: CoroutineScope,
) : OnMPDChangeListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private val dynamicPlaylistType = object : TypeToken<DynamicPlaylist>() {}
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private var dynamicPlaylistState: DynamicPlaylistState? = null
    private val gson: Gson = GsonBuilder().registerTypeAdapter(Instant::class.java, InstantAdapter()).create()
    private val _storedPlaylists = MutableStateFlow<List<MPDPlaylist>>(emptyList())
    private val _activeDynamicPlaylist = MutableStateFlow<DynamicPlaylist?>(null)
    private val _dynamicPlaylists = MutableStateFlow<List<DynamicPlaylist>>(emptyList())
    private val _loadingDynamicPlaylist = MutableStateFlow(false)

    val storedPlaylists = _storedPlaylists.asStateFlow()
    val dynamicPlaylists = _dynamicPlaylists.asStateFlow()
    val activeDynamicPlaylist = _activeDynamicPlaylist.asStateFlow()
    val loadingDynamicPlaylist = _loadingDynamicPlaylist.asStateFlow()

    init {
        repo.registerOnMPDChangeListener(this)
        preferences.registerOnSharedPreferenceChangeListener(this)
        loadDynamicPlaylists()
        loadActiveDynamicPlaylist(playOnLoad = false, replaceCurrentQueue = false)
    }

    fun activateDynamicPlaylist(playlist: DynamicPlaylist) {
        val json = gson.toJson(playlist)

        preferences
            .edit()
            .putString(PREF_ACTIVE_DYNAMIC_PLAYLIST, json)
            .apply()
    }

    fun addAlbumToStoredPlaylist(album: MPDAlbum, playlistName: String, onFinish: (MPDMapResponse) -> Unit) =
        repo.client.enqueue("searchaddpl", listOf(playlistName, album.searchFilter.toString()), onFinish)

    fun addSongToStoredPlaylist(song: MPDSong, playlistName: String, onFinish: ((MPDMapResponse) -> Unit)? = null) =
        repo.client.enqueue("playlistadd", listOf(playlistName, song.filename), onFinish)

    fun createDynamicPlaylist(filter: DynamicPlaylistFilter, shuffle: Boolean) {
        val playlist = DynamicPlaylist(filter, shuffle)
        _dynamicPlaylists.value = _dynamicPlaylists.value.toMutableList().apply { add(playlist) }
        saveDynamicPlaylists()
    }

    fun deactivateDynamicPlaylist() {
        preferences
            .edit()
            .remove(PREF_ACTIVE_DYNAMIC_PLAYLIST)
            .apply()
    }

    fun deleteDynamicPlaylist(playlist: DynamicPlaylist) {
        _dynamicPlaylists.value = _dynamicPlaylists.value.toMutableList().apply {
            remove(playlist)
        }
        saveDynamicPlaylists()
    }

    fun deleteStoredPlaylist(playlistName: String, onFinish: (MPDMapResponse) -> Unit) =
        repo.client.enqueue("rm", playlistName, onFinish)

    fun enqueueStoredPlaylist(playlistName: String, onFinish: (MPDMapResponse) -> Unit) {
        /** Just puts stored playlist last in queue, nothing else. */
        repo.client.enqueue("load", playlistName, onFinish)
    }

    fun fetchStoredPlaylistSongs(playlistName: String, onFinish: (List<MPDSong>) -> Unit) {
        repo.client.enqueueMultiMap("listplaylistinfo", playlistName) { response ->
            onFinish(response.extractSongs())
        }
    }

    fun loadStoredPlaylists() {
        fetchStoredPlaylists { _storedPlaylists.value = it }
    }

    fun playStoredPlaylist(playlistName: String) {
        /** Clears the queue, loads playlist into it, and plays from position 0. */
        repo.client.enqueue("clear") {
            repo.client.enqueue("load", playlistName) {
                repo.client.enqueue("play 0")
            }
        }
    }

    fun renameStoredPlaylist(name: String, newName: String, onFinish: (Boolean) -> Unit) {
        repo.client.enqueue("rename", listOf(name, newName)) { response ->
            onFinish(response.isSuccess)
        }
    }

    fun updateDynamicPlaylist(
        playlist: DynamicPlaylist,
        filter: DynamicPlaylistFilter,
        shuffle: Boolean,
    ) {
        _dynamicPlaylists.value = _dynamicPlaylists.value.toMutableList().apply {
            remove(playlist)
            add(DynamicPlaylist(filter, shuffle))
        }
        saveDynamicPlaylists()
    }

    private fun fetchStoredPlaylists(onFinish: (List<MPDPlaylist>) -> Unit) {
        repo.client.enqueueMultiMap("listplaylists") { response ->
            onFinish(response.extractPlaylists())
        }
    }

    private fun loadActiveDynamicPlaylist(playOnLoad: Boolean, replaceCurrentQueue: Boolean) {
        val playlist = gson.fromJson(preferences.getString(PREF_ACTIVE_DYNAMIC_PLAYLIST, null), dynamicPlaylistType)

        _activeDynamicPlaylist.value = playlist
        dynamicPlaylistState =
            if (playlist != null) {
                _loadingDynamicPlaylist.value = true
                DynamicPlaylistState(
                    context = context,
                    playlist = playlist,
                    repo = repo,
                    ioScope = ioScope,
                    replaceCurrentQueue = replaceCurrentQueue,
                    playOnLoad = playOnLoad
                ) { _loadingDynamicPlaylist.value = false }
            } else null
    }

    private fun loadDynamicPlaylists() {
        val listType = object : TypeToken<List<DynamicPlaylist>>() {}

        gson.fromJson(preferences.getString(PREF_DYNAMIC_PLAYLISTS, "[]"), listType)?.let {
            _dynamicPlaylists.value = it
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
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PREF_DYNAMIC_PLAYLISTS -> loadDynamicPlaylists()
            PREF_ACTIVE_DYNAMIC_PLAYLIST -> loadActiveDynamicPlaylist(playOnLoad = true, replaceCurrentQueue = true)
        }
    }
}

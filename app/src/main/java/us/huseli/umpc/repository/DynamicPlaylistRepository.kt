package us.huseli.umpc.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.umpc.Constants.PREF_ACTIVE_DYNAMIC_PLAYLIST
import us.huseli.umpc.Constants.PREF_DYNAMIC_PLAYLISTS
import us.huseli.umpc.DynamicPlaylistState
import us.huseli.umpc.InstantAdapter
import us.huseli.umpc.data.DynamicPlaylist
import us.huseli.umpc.data.DynamicPlaylistFilter
import us.huseli.umpc.mpd.BaseMPDFilter
import us.huseli.umpc.mpd.OnMPDChangeListener
import us.huseli.umpc.mpd.response.MPDBatchMapResponse
import us.huseli.umpc.mpd.response.MPDMapResponse
import us.huseli.umpc.mpd.response.MPDMultiMapResponse
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicPlaylistRepository @Inject constructor(
    private val ioScope: CoroutineScope,
    private val mpdRepo: MPDRepository,
    @ApplicationContext private val context: Context,
) : SharedPreferences.OnSharedPreferenceChangeListener {
    private val _activeDynamicPlaylist = MutableStateFlow<DynamicPlaylist?>(null)
    private val _dynamicPlaylists = MutableStateFlow<List<DynamicPlaylist>>(emptyList())
    private val _loadingDynamicPlaylist = MutableStateFlow(false)

    private var dynamicPlaylistState: DynamicPlaylistState? = null
    private val dynamicPlaylistType = object : TypeToken<DynamicPlaylist>() {}
    private val gson: Gson = GsonBuilder().registerTypeAdapter(Instant::class.java, InstantAdapter()).create()
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    val activeDynamicPlaylist = _activeDynamicPlaylist.asStateFlow()
    val currentSongPosition = mpdRepo.currentSongPosition
    val dynamicPlaylists = _dynamicPlaylists.asStateFlow()
    val loadingDynamicPlaylist = _loadingDynamicPlaylist.asStateFlow()

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    fun addDynamicPlaylist(playlist: DynamicPlaylist) {
        _dynamicPlaylists.value = _dynamicPlaylists.value.toMutableList().apply { add(playlist) }
    }

    fun clearQueue(onFinish: ((MPDMapResponse) -> Unit)? = null) = mpdRepo.clearQueue(onFinish)

    fun deactivateDynamicPlaylist() {
        preferences
            .edit()
            .remove(PREF_ACTIVE_DYNAMIC_PLAYLIST)
            .apply()
    }

    fun deleteDynamicPlaylist(playlist: DynamicPlaylist) {
        _dynamicPlaylists.value = _dynamicPlaylists.value.toMutableList().apply { remove(playlist) }
    }

    fun enqueueSongsLast(filenames: Collection<String>, onFinish: ((MPDBatchMapResponse) -> Unit)? = null) =
        mpdRepo.enqueueSongsLast(filenames, onFinish)

    fun loadActiveDynamicPlaylist(playOnLoad: Boolean, replaceCurrentQueue: Boolean) {
        val playlist =
            gson.fromJson(preferences.getString(PREF_ACTIVE_DYNAMIC_PLAYLIST, null), dynamicPlaylistType)
        _activeDynamicPlaylist.value = playlist

        ioScope.launch {
            dynamicPlaylistState?.close()
            dynamicPlaylistState =
                if (playlist != null) {
                    _loadingDynamicPlaylist.value = true
                    DynamicPlaylistState(
                        context = context,
                        playlist = playlist,
                        repo = this@DynamicPlaylistRepository,
                        ioScope = ioScope,
                        replaceCurrentQueue = replaceCurrentQueue,
                        playOnLoad = playOnLoad,
                        onLoaded = { _loadingDynamicPlaylist.value = false },
                    )
                } else null
        }
    }

    fun loadDynamicPlaylists() {
        val listType = object : TypeToken<List<DynamicPlaylist>>() {}

        gson.fromJson(preferences.getString(PREF_DYNAMIC_PLAYLISTS, "[]"), listType)?.let { playlists ->
            _dynamicPlaylists.value = playlists.filter { it.server == mpdRepo.server.value }
        }
    }

    fun playSongByPosition(pos: Int, onFinish: ((MPDMapResponse) -> Unit)? = null) =
        mpdRepo.playSongByPosition(pos, onFinish)

    fun registerOnMPDChangeListener(listener: OnMPDChangeListener) = mpdRepo.registerOnMPDChangeListener(listener)

    fun saveDynamicPlaylists() {
        val json = gson.toJson(_dynamicPlaylists.value)

        preferences
            .edit()
            .putString(PREF_DYNAMIC_PLAYLISTS, json)
            .apply()
    }

    fun search(filter: BaseMPDFilter, onFinish: ((MPDMultiMapResponse) -> Unit)? = null) =
        mpdRepo.search(filter, onFinish)

    fun updateDynamicPlaylist(
        playlist: DynamicPlaylist,
        filter: DynamicPlaylistFilter? = null,
        shuffle: Boolean? = null,
        songCount: Int? = null,
    ) {
        deleteDynamicPlaylist(playlist)
        addDynamicPlaylist(
            playlist.copy(
                filter = filter ?: playlist.filter,
                shuffle = shuffle ?: playlist.shuffle,
                songCount = songCount ?: playlist.songCount,
            )
        )
        saveDynamicPlaylists()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PREF_DYNAMIC_PLAYLISTS -> loadDynamicPlaylists()
            PREF_ACTIVE_DYNAMIC_PLAYLIST -> loadActiveDynamicPlaylist(playOnLoad = true, replaceCurrentQueue = true)
        }
    }
}

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
import us.huseli.umpc.LoggerInterface
import us.huseli.umpc.data.DynamicPlaylist
import us.huseli.umpc.data.DynamicPlaylistFilter
import us.huseli.umpc.mpd.MPDFilter
import us.huseli.umpc.mpd.OnMPDChangeListener
import us.huseli.umpc.mpd.response.MPDBatchTextResponse
import us.huseli.umpc.mpd.response.MPDTextResponse
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicPlaylistRepository @Inject constructor(
    private val ioScope: CoroutineScope,
    val mpdRepo: MPDRepository,
    @ApplicationContext private val context: Context,
) : SharedPreferences.OnSharedPreferenceChangeListener, LoggerInterface {
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
    val queue = mpdRepo.queue

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
        ioScope.launch {
            mpdRepo.connectedServer.collect {
                loadActiveDynamicPlaylist(playOnLoad = false, replaceCurrentQueue = false)
            }
        }
    }

    fun activateDynamicPlaylist(playlist: DynamicPlaylist) {
        val json = gson.toJson(playlist)

        log("DYNAMICPLAYLISTREPOSITORY: activateDynamicPlaylist, playlist=$playlist, json=$json")

        preferences
            .edit()
            .putString(PREF_ACTIVE_DYNAMIC_PLAYLIST, json)
            .apply()
    }

    fun addDynamicPlaylist(
        filters: List<DynamicPlaylistFilter>,
        shuffle: Boolean,
        operator: DynamicPlaylist.Operator,
        songCount: Int? = null,
    ) {
        mpdRepo.connectedServer.value?.let { server ->
            _dynamicPlaylists.value += DynamicPlaylist(filters, operator, server, shuffle, songCount)
        }
    }

    fun clearQueue() = mpdRepo.clearQueue()

    fun deactivateDynamicPlaylist() {
        preferences
            .edit()
            .remove(PREF_ACTIVE_DYNAMIC_PLAYLIST)
            .apply()
    }

    fun deleteDynamicPlaylist(playlist: DynamicPlaylist) {
        _dynamicPlaylists.value -= playlist
    }

    inline fun enqueueSongs(filenames: Collection<String>, crossinline onFinish: (MPDBatchTextResponse) -> Unit) =
        mpdRepo.enqueueSongs(filenames, onFinish)

    fun loadDynamicPlaylists() {
        val listType = object : TypeToken<List<DynamicPlaylist>>() {}

        gson.fromJson(preferences.getString(PREF_DYNAMIC_PLAYLISTS, "[]"), listType)?.let { playlists ->
            mpdRepo.connectedServer.value?.let { server ->
                _dynamicPlaylists.value = playlists.filter { it.server == server }
            }
        }
    }

    fun playSongByPosition(pos: Int) = mpdRepo.playSongByPosition(pos)

    fun registerOnMPDChangeListener(listener: OnMPDChangeListener) = mpdRepo.registerOnMPDChangeListener(listener)

    fun saveDynamicPlaylists() {
        val json = gson.toJson(_dynamicPlaylists.value)

        preferences
            .edit()
            .putString(PREF_DYNAMIC_PLAYLISTS, json)
            .apply()
    }

    fun search(filter: MPDFilter, onFinish: (MPDTextResponse) -> Unit) = mpdRepo.search(filter, onFinish)

    fun searchAnd(filters: Iterable<MPDFilter>, onFinish: (MPDTextResponse) -> Unit) =
        mpdRepo.searchAnd(filters, onFinish)

    inline fun searchOr(filters: Iterable<MPDFilter>, crossinline onFinish: (MPDBatchTextResponse) -> Unit) =
        mpdRepo.searchOr(filters, onFinish)

    fun updateDynamicPlaylist(
        playlist: DynamicPlaylist,
        filters: List<DynamicPlaylistFilter>? = null,
        shuffle: Boolean? = null,
        operator: DynamicPlaylist.Operator? = null,
        songCount: Int? = null,
    ) {
        deleteDynamicPlaylist(playlist)
        _dynamicPlaylists.value += playlist.copy(
            filters = filters ?: playlist.filters,
            shuffle = shuffle ?: playlist.shuffle,
            operator = operator ?: playlist.operator,
            songCount = songCount ?: playlist.songCount,
        )
        saveDynamicPlaylists()
    }

    private fun loadActiveDynamicPlaylist(playOnLoad: Boolean, replaceCurrentQueue: Boolean) {
        val playlist = gson.fromJson(
            preferences.getString(PREF_ACTIVE_DYNAMIC_PLAYLIST, null),
            dynamicPlaylistType
        )

        if (playlist == null) {
            _activeDynamicPlaylist.value = null
            ioScope.launch { dynamicPlaylistState?.close() }
        } else mpdRepo.connectedServer.value?.let { server ->
            if (playlist.server == server) {
                _activeDynamicPlaylist.value = playlist
                ioScope.launch {
                    dynamicPlaylistState?.close()
                    _loadingDynamicPlaylist.value = true
                    dynamicPlaylistState = DynamicPlaylistState(
                        context = context,
                        playlist = playlist,
                        repo = this@DynamicPlaylistRepository,
                        ioScope = ioScope,
                        replaceCurrentQueue = replaceCurrentQueue,
                        playOnLoad = playOnLoad,
                        onLoaded = { _loadingDynamicPlaylist.value = false },
                    )
                }
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PREF_DYNAMIC_PLAYLISTS -> loadDynamicPlaylists()
            PREF_ACTIVE_DYNAMIC_PLAYLIST -> {
                loadActiveDynamicPlaylist(playOnLoad = true, replaceCurrentQueue = true)
            }
        }
    }
}

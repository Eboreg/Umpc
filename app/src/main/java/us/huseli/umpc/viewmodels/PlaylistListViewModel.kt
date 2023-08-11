package us.huseli.umpc.viewmodels

import android.content.Context
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.Constants
import us.huseli.umpc.InstantAdapter
import us.huseli.umpc.PlaylistType
import us.huseli.umpc.data.DynamicPlaylist
import us.huseli.umpc.data.DynamicPlaylistFilter
import us.huseli.umpc.mpd.OnMPDChangeListener
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.viewmodels.abstr.BaseViewModel
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class PlaylistListViewModel @Inject constructor(
    repo: MPDRepository,
    @ApplicationContext context: Context,
) : BaseViewModel(repo), OnMPDChangeListener {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val gson: Gson = GsonBuilder().registerTypeAdapter(Instant::class.java, InstantAdapter()).create()
    private val _displayType = MutableStateFlow(PlaylistType.STORED)

    val displayType = _displayType.asStateFlow()
    val dynamicPlaylists = repo.dynamicPlaylists

    init {
        repo.loadDynamicPlaylists()
        repo.loadStoredPlaylists()
        repo.registerOnMPDChangeListener(this)
    }

    fun activateDynamicPlaylist(playlist: DynamicPlaylist) {
        val json = gson.toJson(playlist)

        preferences
            .edit()
            .putString(Constants.PREF_ACTIVE_DYNAMIC_PLAYLIST, json)
            .apply()
    }

    fun createDynamicPlaylist(filter: DynamicPlaylistFilter, shuffle: Boolean) {
        repo.addDynamicPlaylist(DynamicPlaylist(filter, repo.server.value!!, shuffle))
        repo.saveDynamicPlaylists()
    }

    fun deleteDynamicPlaylist(playlist: DynamicPlaylist) {
        repo.deleteDynamicPlaylist(playlist)
        repo.saveDynamicPlaylists()
    }

    fun getStoredPlaylistSongCount(playlistName: String, onFinish: (Int) -> Unit) =
        repo.client.enqueueMultiMap("listplaylistinfo", playlistName) { response ->
            onFinish(response.extractSongsWithPosition().size)
        }

    fun setDisplayType(value: PlaylistType) {
        _displayType.value = value
    }

    fun updateDynamicPlaylist(
        playlist: DynamicPlaylist,
        filter: DynamicPlaylistFilter,
        shuffle: Boolean,
    ) = repo.updateDynamicPlaylist(playlist, filter, shuffle)

    override fun onMPDChanged(subsystems: List<String>) {
        if (subsystems.contains("stored_playlist")) repo.loadStoredPlaylists()
    }
}

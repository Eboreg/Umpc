package us.huseli.umpc.viewmodels

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import us.huseli.umpc.Constants
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.mpd.OnMPDChangeListener
import us.huseli.umpc.mpd.response.MPDTextResponse
import us.huseli.umpc.repository.AlbumArtRepository
import us.huseli.umpc.repository.DynamicPlaylistRepository
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.repository.MessageRepository
import us.huseli.umpc.viewmodels.abstr.SongSelectViewModel
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    repo: MPDRepository,
    messageRepo: MessageRepository,
    albumArtRepo: AlbumArtRepository,
    dynamicPlaylistRepo: DynamicPlaylistRepository,
    @ApplicationContext context: Context,
) : SongSelectViewModel(repo, messageRepo, albumArtRepo), OnMPDChangeListener {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val removedSongs = mutableListOf<MPDSong>()

    val activeDynamicPlaylist = dynamicPlaylistRepo.activeDynamicPlaylist
    val currentSongPosition = repo.currentSongPosition
    var listState = LazyListState()
        private set
    val queue = repo.queue

    init {
        dynamicPlaylistRepo.loadActiveDynamicPlaylist(playOnLoad = false, replaceCurrentQueue = false)
        repo.registerOnMPDChangeListener(this)

        viewModelScope.launch {
            repo.connectedServer.collect {
                if (it != null) listState = LazyListState()
            }
        }
    }

    fun addQueueToPlaylist(playlistName: String, onFinish: (MPDTextResponse) -> Unit) =
        repo.addQueueToPlaylist(playlistName, onFinish)

    fun clearQueue(onFinish: (MPDTextResponse) -> Unit) = repo.clearQueue { response ->
        viewModelScope.launch { listState.scrollToItem(0) }
        onFinish(response)
    }

    fun deactivateDynamicPlaylist() {
        preferences
            .edit()
            .remove(Constants.PREF_ACTIVE_DYNAMIC_PLAYLIST)
            .apply()
    }

    fun moveSong(fromIdx: Int, toIdx: Int) = repo.moveSongInQueue(fromIdx, toIdx)

    fun removeSelectedSongs() {
        removedSongs.clear()
        removedSongs.addAll(selectedSongs.value)
        repo.removeSongsFromQueue(selectedSongs.value) { response ->
            if (response.isSuccess) deselectAllSongs()
        }
    }

    fun removeSong(song: MPDSong) {
        removedSongs.clear()
        removedSongs.add(song)
        repo.removeSongFromQueue(song)
    }

    fun undoRemoveSongs() =
        repo.enqueueSongs(removedSongs) { response ->
            if (response.isSuccess) removedSongs.clear()
        }

    override fun onMPDChanged(subsystems: List<String>) {
        if (subsystems.contains("playlist")) repo.loadQueue()
    }
}

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
import us.huseli.umpc.mpd.command.MPDMapCommand
import us.huseli.umpc.mpd.response.MPDMapResponse
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.viewmodels.abstr.SongSelectViewModel
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    repo: MPDRepository,
    @ApplicationContext context: Context,
) : SongSelectViewModel(repo), OnMPDChangeListener {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val removedSongs = mutableListOf<MPDSong>()

    val activeDynamicPlaylist = repo.activeDynamicPlaylist
    val currentSongPosition = repo.currentSongPosition
    val queue = repo.queue
    val listState = LazyListState()

    init {
        repo.loadQueue()
        repo.loadActiveDynamicPlaylist(playOnLoad = false, replaceCurrentQueue = false)
        repo.registerOnMPDChangeListener(this)
    }

    fun clearQueue(onFinish: (MPDMapResponse) -> Unit) = repo.clearQueue { response ->
        viewModelScope.launch { listState.scrollToItem(0) }
        onFinish(response)
    }

    fun deactivateDynamicPlaylist() {
        preferences
            .edit()
            .remove(Constants.PREF_ACTIVE_DYNAMIC_PLAYLIST)
            .apply()
    }

    fun moveSong(fromIdx: Int, toIdx: Int) = repo.client.enqueue("move $fromIdx $toIdx")

    fun removeSelectedSongs() {
        removedSongs.clear()
        removedSongs.addAll(selectedSongs.value)
        repo.client.enqueueBatch(
            commands = selectedSongs.value.filter { it.id != null }.map { MPDMapCommand("deleteid ${it.id}") },
            onFinish = { deselectAllSongs() },
        )
    }

    fun removeSong(song: MPDSong) {
        removedSongs.clear()
        removedSongs.add(song)
        song.id?.let { repo.client.enqueue("deleteid $it") }
    }

    fun undoRemoveSongs() =
        repo.client.enqueueBatch(
            commands = removedSongs
                .filter { it.position != null }
                .map { MPDMapCommand("add", listOf(it.filename, it.position.toString())) },
            onFinish = { removedSongs.clear() },
        )

    override fun onMPDChanged(subsystems: List<String>) {
        if (subsystems.contains("playlist")) repo.loadQueue()
    }
}

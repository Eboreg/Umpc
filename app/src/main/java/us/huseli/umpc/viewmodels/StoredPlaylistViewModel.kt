package us.huseli.umpc.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import us.huseli.umpc.Constants.NAV_ARG_PLAYLIST
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.mpd.OnMPDChangeListener
import us.huseli.umpc.mpd.command.MPDMapCommand
import us.huseli.umpc.mpd.response.MPDMapResponse
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.viewmodels.abstr.SongSelectViewModel
import javax.inject.Inject
import kotlin.math.min

@HiltViewModel
class StoredPlaylistViewModel @Inject constructor(
    repo: MPDRepository,
    savedStateHandle: SavedStateHandle,
) : SongSelectViewModel(repo), OnMPDChangeListener {
    private val playlistName: String = savedStateHandle.get<String>(NAV_ARG_PLAYLIST)!!
    private val _removedSongs = mutableListOf<MPDSong>()
    private val _songs = MutableStateFlow<List<MPDSong>>(emptyList())

    val listState = LazyListState()
    val songs = _songs.asStateFlow()
    val playlist = repo.storedPlaylists.map { playlists -> playlists.find { it.name == playlistName } }

    init {
        loadSongs()
        repo.registerOnMPDChangeListener(this)
    }

    fun delete(onFinish: (MPDMapResponse) -> Unit) =
        repo.client.enqueue("rm", playlistName, onFinish)

    fun enqueue(onFinish: (MPDMapResponse) -> Unit) =
        repo.client.enqueue("load", playlistName, onFinish)

    fun moveSong(fromIdx: Int, toIdx: Int) =
        repo.client.enqueue("playlistmove", listOf(playlistName, fromIdx.toString(), toIdx.toString()))

    fun rename(newName: String, onFinish: (Boolean) -> Unit) =
        repo.client.enqueue("rename", listOf(playlistName, newName)) { response ->
            onFinish(response.isSuccess)
        }

    private fun loadSongs() =
        repo.client.enqueueMultiMap("listplaylistinfo", playlistName) { response ->
            _songs.value = response.extractSongsWithPosition()
        }

    fun play() {
        val firstSongPosition = min(
            repo.currentSongPosition.value?.plus(1) ?: repo.queue.value.size,
            repo.queue.value.size
        )

        repo.client.enqueue("load", listOf(playlistName, "0:", firstSongPosition.toString())) { response ->
            if (response.isSuccess) playSongByPosition(firstSongPosition)
        }
    }

    fun removeSelectedSongs() {
        _removedSongs.clear()
        _removedSongs.addAll(selectedSongs.value)
        repo.client.enqueueBatch(
            selectedSongs.value
                .filter { it.position != null }
                .sortedByDescending { it.position }
                .map { MPDMapCommand("playlistdelete", listOf(playlistName, it.position.toString())) },
            onFinish = { deselectAllSongs() },
        )
    }

    fun removeSong(song: MPDSong) {
        _removedSongs.clear()
        _removedSongs.add(song)
        song.position?.let { repo.client.enqueue("playlistdelete", listOf(playlistName, it.toString())) }
    }

    fun undoRemoveSongs() =
        repo.client.enqueueBatch(
            commands = _removedSongs.sortedBy { it.position }.map {
                MPDMapCommand("playlistadd", listOf(playlistName, it.filename, it.position.toString()))
            },
            onFinish = { _removedSongs.clear() },
        )

    override fun onMPDChanged(subsystems: List<String>) {
        if (subsystems.contains("stored_playlist")) loadSongs()
    }
}

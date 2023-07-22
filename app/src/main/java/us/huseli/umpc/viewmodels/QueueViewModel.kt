package us.huseli.umpc.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import dagger.hilt.android.lifecycle.HiltViewModel
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.mpd.MPDRepository
import us.huseli.umpc.mpd.response.MPDMapResponse
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(repo: MPDRepository) : SongSelectViewModel(repo) {
    private val _removedSongs = mutableListOf<MPDSong>()

    val activeDynamicPlaylist = repo.engines.playlist.activeDynamicPlaylist
    val currentSongPosition = repo.currentSongPosition
    val queue = repo.queue
    val listState = LazyListState()

    fun clearQueue(onFinish: (MPDMapResponse) -> Unit) = repo.engines.control.clearQueue(onFinish)

    fun deactivateDynamicPlaylist() = repo.engines.playlist.deactivateDynamicPlaylist()

    fun moveSong(fromIdx: Int, toIdx: Int) = repo.engines.control.moveSongInQueue(fromIdx, toIdx)

    fun removeSelectedSongs() {
        _removedSongs.clear()
        _removedSongs.addAll(selectedSongs.value)
        repo.engines.control.removeSongsFromQueue(selectedSongs.value) { deselectAllSongs() }
    }

    fun removeSong(song: MPDSong) {
        _removedSongs.clear()
        _removedSongs.add(song)
        repo.engines.control.removeSongFromQueue(song)
    }

    fun undoRemoveSongs() = repo.engines.control.enqueueSongs(_removedSongs) { _removedSongs.clear() }
}

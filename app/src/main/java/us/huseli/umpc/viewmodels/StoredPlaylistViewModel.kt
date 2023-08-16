package us.huseli.umpc.viewmodels

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import us.huseli.umpc.Constants.NAV_ARG_PLAYLIST
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.mpd.OnMPDChangeListener
import us.huseli.umpc.mpd.response.MPDMapResponse
import us.huseli.umpc.repository.AlbumArtRepository
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.repository.MessageRepository
import us.huseli.umpc.viewmodels.abstr.SongSelectViewModel
import javax.inject.Inject
import kotlin.math.min

@HiltViewModel
class StoredPlaylistViewModel @Inject constructor(
    repo: MPDRepository,
    messageRepo: MessageRepository,
    albumArtRepo: AlbumArtRepository,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext context: Context,
) : SongSelectViewModel(repo, messageRepo, albumArtRepo, context), OnMPDChangeListener {
    private val playlistName: String = savedStateHandle.get<String>(NAV_ARG_PLAYLIST)!!
    private val _removedSongs = mutableListOf<MPDSong>()
    private val _songs = MutableStateFlow<List<MPDSong>>(emptyList())

    val listState = LazyListState()
    val songs = _songs.asStateFlow()
    val playlist = repo.playlists.map { playlists -> playlists.find { it.name == playlistName } }

    init {
        loadSongs()
        repo.registerOnMPDChangeListener(this)
    }

    fun delete(onFinish: (MPDMapResponse) -> Unit) = repo.deletePlaylist(playlistName, onFinish)

    fun enqueue(onFinish: (MPDMapResponse) -> Unit) = repo.enqueuePlaylistNext(playlistName, onFinish)

    fun moveSong(fromIdx: Int, toIdx: Int) = repo.moveSongInPlaylist(playlistName, fromIdx, toIdx)

    fun rename(newName: String, onFinish: (Boolean) -> Unit) =
        repo.renamePlaylist(playlistName, newName) { onFinish(it.isSuccess) }

    private fun loadSongs() = repo.getPlaylistSongs(playlistName) { _songs.value = it }

    fun play() {
        val firstSongPosition = min(
            repo.currentSongPosition.value?.plus(1) ?: repo.queue.value.size,
            repo.queue.value.size
        )
        repo.enqueuePlaylistNext(playlistName) { response ->
            if (response.isSuccess) repo.playSongByPosition(firstSongPosition)
        }
    }

    fun removeSelectedSongs() {
        _removedSongs.clear()
        _removedSongs.addAll(selectedSongs.value)
        repo.removeSongsFromPlaylist(playlistName, selectedSongs.value.mapNotNull { it.position }) { response ->
            if (response.isSuccess) deselectAllSongs()
        }
    }

    fun removeSong(song: MPDSong) {
        _removedSongs.clear()
        _removedSongs.add(song)
        repo.removeSongFromPlaylist(playlistName, song)
    }

    fun undoRemoveSongs() =
        repo.addSongsToPlaylistPositioned(_removedSongs, playlistName) { response ->
            if (response.isSuccess) _removedSongs.clear()
        }

    override fun onMPDChanged(subsystems: List<String>) {
        if (subsystems.contains("stored_playlist")) loadSongs()
    }
}

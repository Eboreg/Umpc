package us.huseli.umpc.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.umpc.Constants.NAV_ARG_PLAYLIST
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.mpd.OnMPDChangeListener
import us.huseli.umpc.mpd.response.MPDTextResponse
import us.huseli.umpc.repository.AlbumArtRepository
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.repository.MessageRepository
import us.huseli.umpc.viewmodels.abstr.SongSelectViewModel
import javax.inject.Inject

@HiltViewModel
class StoredPlaylistViewModel @Inject constructor(
    repo: MPDRepository,
    messageRepo: MessageRepository,
    albumArtRepo: AlbumArtRepository,
    savedStateHandle: SavedStateHandle,
) : SongSelectViewModel(repo, messageRepo, albumArtRepo), OnMPDChangeListener {
    private val _removedSongs = mutableListOf<MPDSong>()

    val playlistName: String = savedStateHandle.get<String>(NAV_ARG_PLAYLIST)!!
    val listState = LazyListState()
    val playlist = repo.playlists.filterNotNull().distinctUntilChanged().map { playlists ->
        playlists.find { it.name == playlistName }?.also { if (it.songs == null) repo.loadPlaylistSongs(it) }
    }

    init {
        repo.registerOnMPDChangeListener(this)
    }

    inline fun delete(crossinline onFinish: (MPDTextResponse) -> Unit) =
        repo.deletePlaylist(playlistName) { onFinish(it) }

    inline fun enqueue(crossinline onFinish: (MPDTextResponse) -> Unit) =
        repo.enqueuePlaylist(playlistName) { onFinish(it) }

    fun moveSong(fromIdx: Int, toIdx: Int) = repo.moveSongInPlaylist(playlistName, fromIdx, toIdx)

    inline fun rename(newName: String, crossinline onFinish: (Boolean) -> Unit) =
        repo.renamePlaylist(playlistName, newName) { onFinish(it.isSuccess) }

    fun play() = repo.playPlaylist(playlistName)

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

    private fun loadSongs() = viewModelScope.launch {
        playlist.first()?.let { repo.loadPlaylistSongs(it) }
    }

    override fun onMPDChanged(subsystems: List<String>) {
        if (subsystems.contains("stored_playlist")) loadSongs()
    }
}

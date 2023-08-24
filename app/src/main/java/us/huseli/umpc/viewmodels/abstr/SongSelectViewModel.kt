package us.huseli.umpc.viewmodels.abstr

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.mpd.response.MPDBatchTextResponse
import us.huseli.umpc.repository.AlbumArtRepository
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.repository.MessageRepository

abstract class SongSelectViewModel(
    repo: MPDRepository,
    messageRepo: MessageRepository,
    albumArtRepo: AlbumArtRepository,
) : AlbumArtViewModel(repo, messageRepo, albumArtRepo) {
    private val _selectedSongs = MutableStateFlow<List<MPDSong>>(emptyList())
    val selectedSongs = _selectedSongs.asStateFlow()

    inline fun addSelectedSongsToPlaylist(playlistName: String, crossinline onFinish: (MPDBatchTextResponse) -> Unit) =
        repo.addSongsToPlaylist(selectedSongs.value, playlistName, onFinish)

    fun deselectAllSongs() {
        _selectedSongs.value = emptyList()
    }

    inline fun enqueueSelectedSongs(crossinline onFinish: (MPDBatchTextResponse) -> Unit) =
        repo.enqueueSongsLast(selectedSongs.value.map { it.filename }, onFinish)

    fun playSelectedSongs() = repo.enqueueSongsNextAndPlay(_selectedSongs.value)

    fun toggleSongSelected(song: MPDSong) {
        _selectedSongs.value = _selectedSongs.value.toMutableList().apply {
            if (contains(song)) remove(song)
            else add(song)
        }
    }
}

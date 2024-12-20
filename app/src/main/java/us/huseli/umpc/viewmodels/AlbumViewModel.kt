package us.huseli.umpc.viewmodels

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import us.huseli.umpc.Constants.NAV_ARG_ALBUM
import us.huseli.umpc.Constants.NAV_ARG_ARTIST
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.mpd.response.MPDBatchTextResponse
import us.huseli.umpc.repository.AlbumArtRepository
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.repository.MessageRepository
import us.huseli.umpc.viewmodels.abstr.SongSelectViewModel
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(
    repo: MPDRepository,
    messageRepo: MessageRepository,
    albumArtRepo: AlbumArtRepository,
    savedStateHandle: SavedStateHandle,
) : SongSelectViewModel(repo, messageRepo, albumArtRepo) {
    private val albumArg: String = savedStateHandle.get<String>(NAV_ARG_ALBUM)!!
    private val artistArg: String = savedStateHandle.get<String>(NAV_ARG_ARTIST)!!
    private val _albumWithSongs = MutableStateFlow<MPDAlbumWithSongs?>(null)
    private val _albumArt = MutableStateFlow<ImageBitmap?>(null)

    val album = MPDAlbum(artistArg, albumArg)
    val albumWithSongs = _albumWithSongs.asStateFlow()
    val albumArt = _albumArt.asStateFlow()
    val currentSongFilename = repo.currentSong.map { it?.filename }.distinctUntilChanged()

    init {
        repo.getAlbumWithSongs(album) { albumWithSongs ->
            _albumWithSongs.value = albumWithSongs
            albumWithSongs.albumArtKey?.let { albumArtKey ->
                getAlbumArt(albumArtKey) { _albumArt.value = it.fullImage }
            }
        }
    }

    inline fun addToPlaylist(playlistName: String, crossinline onFinish: (MPDBatchTextResponse) -> Unit) =
        repo.addAlbumToPlaylist(album, playlistName) { onFinish(it) }

    inline fun enqueue(crossinline onFinish: (MPDBatchTextResponse) -> Unit) = enqueueAlbum(album, onFinish)

    fun play() = repo.playAlbum(album)
}

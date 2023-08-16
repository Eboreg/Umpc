package us.huseli.umpc.viewmodels

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.Constants.NAV_ARG_ALBUM
import us.huseli.umpc.Constants.NAV_ARG_ARTIST
import us.huseli.umpc.ImageRequestType
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.mpd.response.MPDBatchMapResponse
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
    @ApplicationContext context: Context,
) : SongSelectViewModel(repo, messageRepo, albumArtRepo, context) {
    private val albumArg: String = savedStateHandle.get<String>(NAV_ARG_ALBUM)!!
    private val artistArg: String = savedStateHandle.get<String>(NAV_ARG_ARTIST)!!
    private val _albumWithSongs = MutableStateFlow<MPDAlbumWithSongs?>(null)
    private val _albumArt = MutableStateFlow<ImageBitmap?>(null)

    val album = MPDAlbum(artistArg, albumArg)
    val albumWithSongs = _albumWithSongs.asStateFlow()
    val albumArt = _albumArt.asStateFlow()

    init {
        repo.getAlbumWithSongs(album) { albumWithSongs ->
            _albumWithSongs.value = albumWithSongs
            albumWithSongs.albumArtKey?.let { albumArtKey ->
                getAlbumArt(albumArtKey, ImageRequestType.FULL) { _albumArt.value = it.fullImage }
            }
        }
    }

    fun addToPlaylist(playlistName: String, onFinish: (MPDBatchMapResponse) -> Unit) =
        repo.addAlbumToPlaylist(album, playlistName, onFinish)

    fun enqueueLast(onFinish: (MPDBatchMapResponse) -> Unit) = enqueueAlbumLast(album, onFinish)

    fun play() = playAlbum(album)
}

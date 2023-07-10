package us.huseli.umpc.viewmodels

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.umpc.Constants.NAV_ARG_ALBUM
import us.huseli.umpc.Constants.NAV_ARG_ARTIST
import us.huseli.umpc.ImageRequestType
import us.huseli.umpc.data.AlbumArtKey
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.mpd.MPDRepository
import us.huseli.umpc.mpd.response.MPDMapResponse
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(
    repo: MPDRepository,
    savedStateHandle: SavedStateHandle
) : BaseViewModel(repo) {
    private val albumArg: String = savedStateHandle.get<String>(NAV_ARG_ALBUM)!!
    private val artistArg: String = savedStateHandle.get<String>(NAV_ARG_ARTIST)!!
    private val _albumWithSongs = MutableStateFlow<MPDAlbumWithSongs?>(null)
    private val _albumArt = MutableStateFlow<ImageBitmap?>(null)

    val album = MPDAlbum(artistArg, albumArg)
    val albumWithSongs = _albumWithSongs.asStateFlow()
    val albumArt = _albumArt.asStateFlow()

    init {
        repo.fetchSongListByAlbum(album) {
            _albumWithSongs.value = MPDAlbumWithSongs(album, it)
        }
        viewModelScope.launch {
            repo.engines.image.getAlbumArt(AlbumArtKey(artistArg, albumArg), ImageRequestType.FULL) {
                _albumArt.value = it.fullImage
            }
        }
    }

    fun addToPlaylist(playlistName: String, onFinish: (MPDMapResponse) -> Unit) {
        repo.engines.playlist.addAlbumToStoredPlaylist(album, playlistName, onFinish)
    }
}

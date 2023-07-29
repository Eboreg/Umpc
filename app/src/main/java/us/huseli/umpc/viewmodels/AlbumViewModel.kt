package us.huseli.umpc.viewmodels

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.Constants.NAV_ARG_ALBUM
import us.huseli.umpc.Constants.NAV_ARG_ARTIST
import us.huseli.umpc.ImageRequestType
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.mpd.response.MPDMapResponse
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.viewmodels.abstr.SongSelectViewModel
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(
    repo: MPDRepository,
    savedStateHandle: SavedStateHandle,
) : SongSelectViewModel(repo) {
    private val albumArg: String = savedStateHandle.get<String>(NAV_ARG_ALBUM)!!
    private val artistArg: String = savedStateHandle.get<String>(NAV_ARG_ARTIST)!!
    private val _albumWithSongs = MutableStateFlow<MPDAlbumWithSongs?>(null)
    private val _albumArt = MutableStateFlow<ImageBitmap?>(null)

    val album = MPDAlbum(artistArg, albumArg)
    val albumWithSongs = _albumWithSongs.asStateFlow()
    val albumArt = _albumArt.asStateFlow()

    init {
        repo.client.enqueueMultiMap(album.searchFilter.find()) { response ->
            val songs = response.extractSongs()
            val albumWithSongs = MPDAlbumWithSongs(album, songs)

            repo.addAlbumsWithSongs(listOf(albumWithSongs))
            _albumWithSongs.value = albumWithSongs
            albumWithSongs.albumArtKey?.let { albumArtKey ->
                repo.getAlbumArt(albumArtKey, ImageRequestType.FULL) { _albumArt.value = it.fullImage }
            }
        }
    }

    fun addAlbumToPlaylist(playlistName: String, onFinish: (MPDMapResponse) -> Unit) =
        repo.client.enqueue("searchaddpl", listOf(playlistName, album.searchFilter.toString()), onFinish)
}

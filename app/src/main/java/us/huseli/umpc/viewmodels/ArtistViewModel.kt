package us.huseli.umpc.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.umpc.Constants
import us.huseli.umpc.ImageRequestType
import us.huseli.umpc.data.AlbumArtKey
import us.huseli.umpc.data.MPDAlbumArt
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.data.groupByAlbum
import us.huseli.umpc.mpd.MPDRepository
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(
    repo: MPDRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel(repo) {
    private val _albums = MutableStateFlow<List<MPDAlbumWithSongs>>(emptyList())

    val albums = _albums.asStateFlow()
    val artist: String = savedStateHandle.get<String>(Constants.NAV_ARG_ARTIST)!!

    init {
        repo.fetchSongsByArtist(artist) { songs ->
            songs.groupByAlbum().also { albums ->
                _albums.value = albums
            }
        }
    }

    fun getAlbumArt(keys: Iterable<AlbumArtKey>, callback: (MPDAlbumArt) -> Unit) = viewModelScope.launch {
        keys.forEach { key ->
            repo.engines.image.getAlbumArt(key, ImageRequestType.BOTH, callback)
        }
    }
}

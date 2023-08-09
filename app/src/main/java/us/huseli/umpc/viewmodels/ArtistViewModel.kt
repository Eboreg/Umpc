package us.huseli.umpc.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import us.huseli.umpc.Constants.NAV_ARG_ARTIST
import us.huseli.umpc.ImageRequestType
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.data.groupByAlbum
import us.huseli.umpc.mpd.mpdFind
import us.huseli.umpc.mpd.mpdList
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.viewmodels.abstr.AlbumSelectViewModel
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(
    repo: MPDRepository,
    savedStateHandle: SavedStateHandle,
) : AlbumSelectViewModel(repo) {
    private val _albums = MutableStateFlow<List<MPDAlbum>>(emptyList())
    private val _appearsOnAlbums = MutableStateFlow<List<MPDAlbum>>(emptyList())
    private val _albumsWithSongs = MutableStateFlow<List<MPDAlbumWithSongs>>(emptyList())
    private val _appearsOnAlbumsWithSongs = MutableStateFlow<List<MPDAlbumWithSongs>>(emptyList())
    private val _songCount = MutableStateFlow(0)
    private val _totalDuration = MutableStateFlow(0.0)
    private val _gridAlbumArt = MutableStateFlow<List<ImageBitmap>>(emptyList())

    val artist: String = savedStateHandle.get<String>(NAV_ARG_ARTIST)!!
    val albums = combine(_albums, _albumsWithSongs) { albums, albumsWithSongs ->
        (albumsWithSongs + albums
            .filter { album -> !albumsWithSongs.map { it.album }.contains(album) }
            .map { MPDAlbumWithSongs(it, emptyList()) }).sortedBy { it.album.name.lowercase() }
    }
    val appearsOnAlbums = combine(_appearsOnAlbums, _appearsOnAlbumsWithSongs) { albums, albumsWithSongs ->
        (albumsWithSongs + albums
            .filter { album -> !albumsWithSongs.map { it.album }.contains(album) }
            .map { MPDAlbumWithSongs(it, emptyList()) }).sortedBy { it.album.name.lowercase() }
    }
    val songCount = _songCount.asStateFlow()
    val totalDuration = _totalDuration.asStateFlow()
    val gridAlbumArt = _gridAlbumArt.asStateFlow()
    val listState = LazyListState()

    init {
        repo.client.enqueueMultiMap(mpdList("albumsort") { equals("albumartist", artist) }) { response ->
            _albums.value = response.extractAlbums(artist)
            repo.client.enqueueMultiMap(mpdList("albumsort", "albumartist") { equals("artist", artist) }) { response2 ->
                _appearsOnAlbums.value = response2.extractAlbums().filter { it.artist != artist }

                viewModelScope.launch {
                    fetchAlbumSongs { albumWithSongs ->
                        if (albumWithSongs.album.artist == artist) {
                            _albumsWithSongs.value += albumWithSongs
                            _songCount.value += albumWithSongs.songs.size
                            _totalDuration.value += albumWithSongs.songs.sumOf { it.duration ?: 0.0 }
                        } else {
                            _appearsOnAlbumsWithSongs.value += albumWithSongs
                            albumWithSongs.songs.filter { it.artist == artist }.also { songs ->
                                _songCount.value += songs.size
                                _totalDuration.value += songs.sumOf { it.duration ?: 0.0 }
                            }
                        }
                        if (_gridAlbumArt.value.size < 16) {
                            albumWithSongs.albumArtKey?.let { key ->
                                repo.getAlbumArt(key, ImageRequestType.FULL) { albumArt ->
                                    _gridAlbumArt.value += albumArt.fullImage
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun fetchAlbumSongs(onEach: (MPDAlbumWithSongs) -> Unit) {
        val appearsOnAlbumsMap = _appearsOnAlbums.value.groupBy { it.artist }

        repo.client.enqueueMultiMap(mpdFind { equals("albumartist", artist) }) { response ->
            response.extractSongs().groupByAlbum().forEach(onEach)
        }

        appearsOnAlbumsMap.forEach { (albumArtist, albums) ->
            val albumRegex = albums.map { "(^${it.name}$)" }.toSet().joinToString("|")
            val command = mpdFind { equals("albumartist", albumArtist) and regex("album", albumRegex) }

            repo.client.enqueueMultiMap(command) { response ->
                response.extractSongs().groupByAlbum().forEach(onEach)
            }
        }
    }
}

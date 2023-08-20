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
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.repository.AlbumArtRepository
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.repository.MessageRepository
import us.huseli.umpc.viewmodels.abstr.AlbumSelectViewModel
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(
    repo: MPDRepository,
    messageRepo: MessageRepository,
    albumArtRepo: AlbumArtRepository,
    savedStateHandle: SavedStateHandle,
) : AlbumSelectViewModel(repo, messageRepo, albumArtRepo) {
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
        repo.getAlbumsByAlbumArtist(artist) { albums ->
            // TODO: albums empty bc mopidy doesn't give everything albumartist?
            _albums.value = albums

            repo.getAlbumsByArtist(artist) { appearsOnAlbums ->
                _appearsOnAlbums.value = appearsOnAlbums.filter { it.artist != artist }
                _albums.value += appearsOnAlbums.filter { it.artist == artist }.minus(_albums.value.toSet())

                viewModelScope.launch {
                    repo.getAlbumsWithSongs(_albums.value) { albumsWithSongs ->
                        _albumsWithSongs.value = albumsWithSongs
                        _songCount.value += albumsWithSongs.sumOf { it.songs.size }
                        _totalDuration.value += albumsWithSongs.sumOf { aws -> aws.songs.sumOf { it.duration ?: 0.0 } }
                        getGridAlbumArt(albumsWithSongs)
                    }

                    repo.getAlbumsWithSongs(_appearsOnAlbums.value) { albumsWithSongs ->
                        _appearsOnAlbumsWithSongs.value = albumsWithSongs
                        albumsWithSongs.flatMap { it.songs }.filter { it.artist == artist }.also { songs ->
                            _songCount.value += songs.size
                            _totalDuration.value += songs.sumOf { it.duration ?: 0.0 }
                        }
                        getGridAlbumArt(albumsWithSongs)
                    }
                }
            }
        }
    }

    private fun getGridAlbumArt(albums: Collection<MPDAlbumWithSongs>) {
        albums.mapNotNull { it.albumArtKey }.forEach { key ->
            if (_gridAlbumArt.value.size < 16) getAlbumArt(key) { _gridAlbumArt.value += it.fullImage }
        }
    }
}

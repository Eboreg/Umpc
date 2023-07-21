package us.huseli.umpc.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.umpc.Constants.NAV_ARG_ARTIST
import us.huseli.umpc.ImageRequestType
import us.huseli.umpc.data.MPDAlbumArt
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.mpd.MPDRepository
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(
    repo: MPDRepository,
    savedStateHandle: SavedStateHandle,
) : AlbumSelectViewModel(repo) {
    private val _albumArtistAlbums = MutableStateFlow<List<MPDAlbumWithSongs>>(emptyList())
    private val _nonAlbumArtistAlbums = MutableStateFlow<List<MPDAlbumWithSongs>>(emptyList())
    private val _artistSongs = combine(_albumArtistAlbums, _nonAlbumArtistAlbums) { aaa, naaa ->
        aaa.flatMap { it.songs } + naaa.flatMap { it.songs.filter { s -> s.artist == artist } }
    }
    private val _albumArtMap = MutableStateFlow<Map<String, MPDAlbumArt>>(emptyMap())

    val artist: String = savedStateHandle.get<String>(NAV_ARG_ARTIST)!!
    val albumArtistAlbums = _albumArtistAlbums.asStateFlow()
    val nonAlbumArtistAlbums = _nonAlbumArtistAlbums.asStateFlow()
    val songCount = _artistSongs.map { it.size }
    val totalDuration = _artistSongs.map { songs -> songs.sumOf { it.duration ?: 0.0 } }
    val albumArtMap = _albumArtMap.asStateFlow()

    init {
        repo.fetchAlbumWithSongsListsByArtist(artist) { albumArtistAlbums, nonAlbumArtistAlbums ->
            _albumArtistAlbums.value = albumArtistAlbums
            _nonAlbumArtistAlbums.value = nonAlbumArtistAlbums

            viewModelScope.launch {
                val keys = (albumArtistAlbums + nonAlbumArtistAlbums).mapNotNull { it.albumArtKey }
                var finishedImages = 0

                keys.forEach { key ->
                    repo.engines.image.getAlbumArt(key, ImageRequestType.BOTH) { albumArt ->
                        _albumArtMap.value = _albumArtMap.value.toMutableMap().apply {
                            put(albumArt.key.album, albumArt)
                        }
                    }
                    if (++finishedImages >= 16) return@forEach
                }
            }
        }
    }
}

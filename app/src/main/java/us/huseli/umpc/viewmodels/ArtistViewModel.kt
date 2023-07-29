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
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDAlbumArt
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.data.groupByAlbum
import us.huseli.umpc.data.plus
import us.huseli.umpc.data.sortedByYear
import us.huseli.umpc.mpd.mpdFind
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.viewmodels.abstr.AlbumSelectViewModel
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(
    repo: MPDRepository,
    savedStateHandle: SavedStateHandle,
) : AlbumSelectViewModel(repo) {
    // Albums where the artist is the album artist:
    private val _albumArtistAlbums = MutableStateFlow<List<MPDAlbumWithSongs>>(emptyList())
    // Albums where the artist is _not_ the album artist:
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
        fetchAlbumWithSongsListsByArtist(artist) { albumArtistAlbums, nonAlbumArtistAlbums ->
            _albumArtistAlbums.value = albumArtistAlbums
            _nonAlbumArtistAlbums.value = nonAlbumArtistAlbums
            repo.addAlbumsWithSongs(albumArtistAlbums)
            repo.addAlbumsWithSongs(nonAlbumArtistAlbums)

            viewModelScope.launch {
                val keys = (albumArtistAlbums + nonAlbumArtistAlbums).mapNotNull { it.albumArtKey }
                var finishedImages = 0

                keys.forEach { key ->
                    repo.getAlbumArt(key, ImageRequestType.BOTH) { albumArt ->
                        _albumArtMap.value = _albumArtMap.value.toMutableMap().apply {
                            put(albumArt.key.album, albumArt)
                        }
                    }
                    if (++finishedImages >= 16) return@forEach
                }
            }
        }
    }

    private fun fetchAlbumWithSongsListsByAlbumList(
        albums: List<MPDAlbum>,
        onFinish: (List<MPDAlbumWithSongs>) -> Unit,
    ) {
        val albumsWithSongs = mutableListOf<MPDAlbumWithSongs>()
        var i = 0

        if (albums.isEmpty()) onFinish(albumsWithSongs)

        albums.forEach { album ->
            repo.client.enqueueMultiMap(album.searchFilter.find()) { response ->
                val albumWithSongs = response.extractSongs().groupByAlbum()[0]

                albumsWithSongs.add(albumWithSongs)
                if (++i == albums.size) onFinish(albumsWithSongs)
            }
        }
    }

    private fun fetchAlbumWithSongsListsByArtist(
        artist: String,
        onFinish: (List<MPDAlbumWithSongs>, List<MPDAlbumWithSongs>) -> Unit,
    ) {
        // Albums where the artist is _not_ the album artist:
        var nonAlbumArtistAlbums = listOf<MPDAlbumWithSongs>()
        // Albums where the artist _is_ the album artist:
        var albumArtistAlbums = listOf<MPDAlbumWithSongs>()

        repo.client.enqueueMultiMap(mpdFind { equals("artist", artist) }) { response ->
            // We got songs where this artist is in the "artist" tag:
            response.extractSongs().toMutableSet().also { songs ->
                nonAlbumArtistAlbums = songs.filter { it.artist != it.album.artist }.groupByAlbum()
                albumArtistAlbums = songs.filter { it.artist == it.album.artist }.groupByAlbum()
            }
            // The nonAlbumArtistAlbums are likely not complete (as they
            // contain songs by other artists), so the songs for those will
            // have to be fetched separately:
            fetchAlbumWithSongsListsByAlbumList(nonAlbumArtistAlbums.map { it.album }) { aws ->
                nonAlbumArtistAlbums = aws
                repo.client.enqueueMultiMap(mpdFind { equals("albumartist", artist) }) { response ->
                    albumArtistAlbums = albumArtistAlbums.plus(response.extractSongs().groupByAlbum())
                    onFinish(albumArtistAlbums.sortedByYear(), nonAlbumArtistAlbums.sortedByYear())
                }
            }
        }
    }
}

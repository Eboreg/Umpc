package us.huseli.umpc.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.umpc.ImageRequestType
import us.huseli.umpc.LibraryGrouping
import us.huseli.umpc.LibrarySearchType
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDAlbumArt
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.data.MPDArtistWithAlbums
import us.huseli.umpc.data.groupByArtist
import us.huseli.umpc.leadingChars
import us.huseli.umpc.mpd.MPDRepository
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(repo: MPDRepository) : AlbumSelectViewModel(repo) {
    private val _activeLibrarySearchType = MutableStateFlow(LibrarySearchType.NONE)
    private val _albums = MutableStateFlow<List<MPDAlbum>>(emptyList())
    private val _artists = MutableStateFlow<List<MPDArtistWithAlbums>>(emptyList())
    private val _librarySearchTerm = MutableStateFlow("")
    private var _grouping = MutableStateFlow(LibraryGrouping.ARTIST)

    val albums = _albums.asStateFlow()
    val artists = _artists.asStateFlow()
    val isLibrarySearchActive = _activeLibrarySearchType.map { it != LibrarySearchType.NONE }
    val librarySearchTerm = _librarySearchTerm.asStateFlow()
    val grouping = _grouping.asStateFlow()
    val artistListState = LazyListState()
    val albumListState = LazyListState()
    val artistLeadingChars = _artists.leadingChars { it.name }
    val albumLeadingChars = _albums.leadingChars { it.name }

    init {
        viewModelScope.launch {
            combine(repo.albums, _activeLibrarySearchType, _librarySearchTerm) { albums, searchType, searchTerm ->
                when (searchType) {
                    LibrarySearchType.ALBUM -> albums.filter {
                        it.name.contains(searchTerm, true) || it.artist.contains(searchTerm, true)
                    }
                    LibrarySearchType.ARTIST -> albums.filter { it.artist.contains(searchTerm, true) }
                    LibrarySearchType.NONE -> albums
                }
            }.collect { albums ->
                _albums.value = albums
                _artists.value = albums.groupByArtist()
            }
        }
    }

    fun activateLibrarySearch(grouping: LibraryGrouping) {
        _activeLibrarySearchType.value = when (grouping) {
            LibraryGrouping.ARTIST -> LibrarySearchType.ARTIST
            LibraryGrouping.ALBUM -> LibrarySearchType.ALBUM
        }
    }

    fun deactivateLibrarySearch() {
        _activeLibrarySearchType.value = LibrarySearchType.NONE
    }

    fun getAlbumWithSongsByAlbum(album: MPDAlbum, callback: (MPDAlbumWithSongs) -> Unit) =
        repo.getAlbumWithSongsByAlbum(album, callback)

    fun getThumbnail(albumWithSongs: MPDAlbumWithSongs, callback: (MPDAlbumArt) -> Unit) = viewModelScope.launch {
        albumWithSongs.albumArtKey?.let { albumArtKey ->
            repo.engines.image.getAlbumArt(albumArtKey, ImageRequestType.THUMBNAIL, callback)
        }
    }

    fun searchLibrary(grouping: LibraryGrouping) {
        when (grouping) {
            LibraryGrouping.ARTIST -> _activeLibrarySearchType.value = LibrarySearchType.ARTIST
            LibraryGrouping.ALBUM -> _activeLibrarySearchType.value = LibrarySearchType.ALBUM
        }
    }

    fun setGrouping(value: LibraryGrouping) {
        _grouping.value = value
    }

    fun setLibrarySearchTerm(value: String) {
        _librarySearchTerm.value = value
    }
}

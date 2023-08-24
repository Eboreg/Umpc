package us.huseli.umpc.viewmodels

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.umpc.LibraryGrouping
import us.huseli.umpc.LibrarySearchType
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.data.MPDArtistWithAlbums
import us.huseli.umpc.data.groupByArtist
import us.huseli.umpc.leadingChars
import us.huseli.umpc.mpd.OnMPDChangeListener
import us.huseli.umpc.repository.AlbumArtRepository
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.repository.MessageRepository
import us.huseli.umpc.viewmodels.abstr.AlbumSelectViewModel
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    repo: MPDRepository,
    messageRepo: MessageRepository,
    albumArtRepo: AlbumArtRepository,
) : AlbumSelectViewModel(repo, messageRepo, albumArtRepo), OnMPDChangeListener {
    private val _activeLibrarySearchType = MutableStateFlow(LibrarySearchType.NONE)
    private val _albums = MutableStateFlow<List<MPDAlbum>>(emptyList())
    private val _albumsWithSongs = MutableStateFlow<List<MPDAlbumWithSongs>>(emptyList())
    private val _artists = MutableStateFlow<List<MPDArtistWithAlbums>>(emptyList())
    private var _grouping = MutableStateFlow(LibraryGrouping.ARTIST)
    private val _librarySearchTerm = MutableStateFlow("")
    private val _pendingAlbumsWithSongs = mutableSetOf<MPDAlbum>()

    val albumLeadingChars = _albums.leadingChars { it.name }
    val albums = _albums.asStateFlow()
    val artistLeadingChars = _artists.leadingChars { it.name }
    val artists = _artists.asStateFlow()
    val grouping = _grouping.asStateFlow()
    val isLibrarySearchActive = _activeLibrarySearchType.map { it != LibrarySearchType.NONE }
    val librarySearchTerm = _librarySearchTerm.asStateFlow()

    init {
        repo.registerOnMPDChangeListener(this)

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

    fun getAlbumWithSongsFlow(album: MPDAlbum) =
        _albumsWithSongs.map { flow -> flow.find { it.album == album } ?: MPDAlbumWithSongs(album, emptyList()) }

    fun loadAlbumsWithSongs(albums: List<MPDAlbum>) =
        albums
            .minus(_pendingAlbumsWithSongs)
            .minus(_albumsWithSongs.value.map { it.album }.toSet())
            .takeIf { it.isNotEmpty() }
            ?.let { filteredAlbums ->
                _pendingAlbumsWithSongs.addAll(filteredAlbums)
                repo.getAlbumsWithSongs(filteredAlbums) { albumsWithSongs ->
                    _albumsWithSongs.value += albumsWithSongs.minus(_albumsWithSongs.value.toSet())
                    _pendingAlbumsWithSongs.removeAll(albumsWithSongs.map { it.album }.toSet())
                }
            }

    fun searchLibrary() {
        when (_grouping.value) {
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

    override fun onMPDChanged(subsystems: List<String>) {
        if (subsystems.contains("database")) repo.loadAlbums()
    }
}

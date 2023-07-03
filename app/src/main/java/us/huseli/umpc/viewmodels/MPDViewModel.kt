package us.huseli.umpc.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.umpc.ImageRequestType
import us.huseli.umpc.LibraryGrouping
import us.huseli.umpc.MessageEngine
import us.huseli.umpc.PlayerState
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDAlbumArt
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.data.MPDArtistWithAlbums
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.data.groupByArtist
import us.huseli.umpc.mpd.MPDEngine
import us.huseli.umpc.mpd.MPDStreamPlayer
import javax.inject.Inject

@HiltViewModel
class MPDViewModel @Inject constructor(
    private val engine: MPDEngine,
    private val messageEngine: MessageEngine,
    private val streamPlayer: MPDStreamPlayer,
) : ViewModel() {
    enum class LibrarySearchType { ARTIST, ALBUM, NONE }

    private val _activeLibrarySearchType = MutableStateFlow(LibrarySearchType.NONE)
    private val _albums = MutableStateFlow<List<MPDAlbum>>(emptyList())
    private val _artists = MutableStateFlow<List<MPDArtistWithAlbums>>(emptyList())
    private val _librarySearchTerm = MutableStateFlow("")
    private val _songSearchTerm = MutableStateFlow("")
    private val _songSearchResults = MutableStateFlow<List<MPDSong>>(emptyList())

    val albums = _albums.asStateFlow()
    val artists = _artists.asStateFlow()
    val currentSong = engine.currentSong
    val currentSongAlbumArt = engine.currentSongAlbumArt
    val currentSongDuration = engine.currentSongDuration
    val currentSongElapsed = engine.currentSongElapsed
    val currentSongFilename = currentSong.map { it?.filename }.distinctUntilChanged()
    val currentSongId = engine.currentSongId
    val currentSongIndex = engine.currentSongIndex
    val error = engine.error
    val isLibrarySearchActive = _activeLibrarySearchType.map { it != LibrarySearchType.NONE }
    val isStreaming = streamPlayer.isStreaming
    val librarySearchTerm = _librarySearchTerm.asStateFlow()
    val message = messageEngine.message
    val playerState = engine.playerState
    val queue = engine.queue
    val randomState = engine.randomState
    val repeatState = engine.repeatState
    val songSearchResults = _songSearchResults.asStateFlow()
    val songSearchTerm = _songSearchTerm.asStateFlow()
    val volume = engine.volume

    init {
        viewModelScope.launch {
            combine(engine.albums, _activeLibrarySearchType, _librarySearchTerm) { albums, searchType, searchTerm ->
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

    fun addMessage(message: String) = messageEngine.addMessage(message)
    fun clearError() = engine.clearError()
    fun clearMessage() = messageEngine.clearMessage()
    fun next() = engine.next()
    fun playAlbum(album: MPDAlbum) = engine.enqueueNextAndPlay(album)
    fun previous() = engine.previous()
    fun seek(time: Double) = engine.seek(time)
    fun setVolume(value: Int) = engine.setVolume(value)
    fun stop() = engine.stop()
    fun toggleRandomState() = engine.toggleRandomState()
    fun toggleRepeatState() = engine.toggleRepeatState()

    fun activateLibrarySearch(grouping: LibraryGrouping) {
        _activeLibrarySearchType.value = when (grouping) {
            LibraryGrouping.ARTIST -> LibrarySearchType.ARTIST
            LibraryGrouping.ALBUM -> LibrarySearchType.ALBUM
        }
    }

    fun deactivateLibrarySearch() {
        _activeLibrarySearchType.value = LibrarySearchType.NONE
    }

    fun enqueueAlbum(album: MPDAlbum) = engine.enqueueLast(album) { response ->
        if (response.isSuccess) addMessage("The album was enqueued.")
        else addMessage("Could not enqeue album: ${response.error}")
    }

    fun enqueueSong(song: MPDSong) = engine.enqueueLast(song) { response ->
        if (response.isSuccess) addMessage("The song was enqueued.")
        else addMessage("Could not enqueue song: ${response.error}")
    }

    fun getAlbumArtState(song: MPDSong): State<ImageBitmap?> = mutableStateOf<ImageBitmap?>(null).also { state ->
        viewModelScope.launch {
            engine.getAlbumArt(song.albumArtKey, ImageRequestType.FULL) { state.value = it.fullImage }
        }
    }

    fun getAlbumsWithSongsByAlbumArtist(artist: String, onFinish: (List<MPDAlbumWithSongs>) -> Unit) =
        engine.getAlbumsWithSongsByAlbumArtist(artist, onFinish)

    fun getAlbumWithSongs(album: MPDAlbum): StateFlow<MPDAlbumWithSongs> = engine.getAlbumWithSongs(album)

    fun getThumbnail(album: MPDAlbumWithSongs, callback: (MPDAlbumArt) -> Unit) = viewModelScope.launch {
        engine.getAlbumArt(album.albumArtKey, ImageRequestType.THUMBNAIL, callback)
    }

    fun playOrPause() = engine.playOrPause()

    fun playOrPauseSong(song: MPDSong) {
        if (song.filename == currentSong.value?.filename) engine.playOrPause()
        else if (song.id != null) engine.playSongId(song.id)
        else engine.enqueueNextAndPlay(song)
    }

    fun searchLibrary(grouping: LibraryGrouping) {
        when (grouping) {
            LibraryGrouping.ARTIST -> _activeLibrarySearchType.value = LibrarySearchType.ARTIST
            LibraryGrouping.ALBUM -> _activeLibrarySearchType.value = LibrarySearchType.ALBUM
        }
    }

    fun setLibrarySearchTerm(value: String) {
        _librarySearchTerm.value = value
    }

    fun setSongSearchTerm(value: String) {
        _songSearchTerm.value = value
        if (value.length >= 3) {
            engine.search(value) { _songSearchResults.value = it }
        }
    }

    fun toggleStream(enabled: Boolean) = viewModelScope.launch {
        if (enabled) streamPlayer.start()
        else streamPlayer.stop()
    }
}

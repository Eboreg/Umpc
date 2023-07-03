package us.huseli.umpc.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.umpc.Constants
import us.huseli.umpc.ImageRequestType
import us.huseli.umpc.MessageEngine
import us.huseli.umpc.data.AlbumArtKey
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDAlbumArt
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.data.groupByAlbum
import us.huseli.umpc.mpd.MPDEngine
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val engine: MPDEngine,
    private val messageEngine: MessageEngine,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _albums = MutableStateFlow<List<MPDAlbumWithSongs>>(emptyList())
    private val currentSong: StateFlow<MPDSong?> = engine.currentSong

    val artist: String = savedStateHandle.get<String>(Constants.NAV_ARG_ARTIST)!!
    val albums = _albums.asStateFlow()
    val currentSongFilename = currentSong.map { it?.filename }.distinctUntilChanged()
    val playerState = engine.playerState

    init {
        engine.fetchSongsByArtist(artist) { songs ->
            songs.groupByAlbum().also { albums ->
                _albums.value = albums
            }
        }
    }

    fun enqueueAlbum(album: MPDAlbum) = engine.enqueueLast(album) { response ->
        if (response.isSuccess) messageEngine.addMessage("The album was enqueued.")
        else messageEngine.addMessage("Could not enqeue album: ${response.error}")
    }

    fun enqueueSong(song: MPDSong) = engine.enqueueLast(song) { response ->
        if (response.isSuccess) messageEngine.addMessage("The song was enqueued.")
        else messageEngine.addMessage("Could not enqueue song: ${response.error}")
    }

    fun playAlbum(album: MPDAlbum) = engine.enqueueNextAndPlay(album)

    fun getAlbumArt(keys: Iterable<AlbumArtKey>, callback: (MPDAlbumArt) -> Unit) = viewModelScope.launch {
        keys.forEach { key ->
            engine.getAlbumArt(key, ImageRequestType.BOTH, callback)
        }
    }

    fun playOrPauseSong(song: MPDSong) {
        if (song.filename == currentSong.value?.filename) engine.playOrPause()
        else if (song.id != null) engine.playSongId(song.id)
        else engine.enqueueNextAndPlay(song)
    }
}

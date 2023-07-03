package us.huseli.umpc.viewmodels

import androidx.compose.ui.graphics.ImageBitmap
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
import us.huseli.umpc.Constants.NAV_ARG_ALBUM
import us.huseli.umpc.Constants.NAV_ARG_ARTIST
import us.huseli.umpc.ImageRequestType
import us.huseli.umpc.MessageEngine
import us.huseli.umpc.data.AlbumArtKey
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.mpd.MPDEngine
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val engine: MPDEngine,
    private val messageEngine: MessageEngine,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val artistArg: String = savedStateHandle.get<String>(NAV_ARG_ARTIST)!!
    private val albumArg: String = savedStateHandle.get<String>(NAV_ARG_ALBUM)!!
    private val _album = MutableStateFlow<MPDAlbumWithSongs?>(null)
    private val _albumArt = MutableStateFlow<ImageBitmap?>(null)
    private val currentSong: StateFlow<MPDSong?> = engine.currentSong

    val currentSongFilename = currentSong.map { it?.filename }.distinctUntilChanged()
    val playerState = engine.playerState
    val albumArt = _albumArt.asStateFlow()
    val album = _album.asStateFlow()

    init {
        engine.fetchSongs(artistArg, albumArg) {
            _album.value = MPDAlbumWithSongs(artistArg, albumArg, it)
        }
        viewModelScope.launch {
            engine.getAlbumArt(AlbumArtKey(artistArg, albumArg), ImageRequestType.FULL) {
                _albumArt.value = it.fullImage
            }
        }
    }

    fun enqueueAlbum(album: MPDAlbum?) {
        if (album != null) {
            engine.enqueueLast(album) { response ->
                if (response.isSuccess) messageEngine.addMessage("The album was enqueued.")
                else messageEngine.addMessage("Could not enqeue album: ${response.error}")
            }
        }
    }

    fun enqueueSong(song: MPDSong) = engine.enqueueLast(song) { response ->
        if (response.isSuccess) messageEngine.addMessage("The song was enqueued.")
        else messageEngine.addMessage("Could not enqueue song: ${response.error}")
    }

    fun playAlbum(album: MPDAlbum?) {
        if (album != null) engine.enqueueNextAndPlay(album)
    }

    fun playOrPauseSong(song: MPDSong) {
        if (song.filename == currentSong.value?.filename) engine.playOrPause()
        else if (song.id != null) engine.playSongId(song.id)
        else engine.enqueueNextAndPlay(song)
    }
}

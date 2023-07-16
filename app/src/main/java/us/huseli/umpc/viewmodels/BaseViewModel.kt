package us.huseli.umpc.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.umpc.ImageRequestType
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDAlbumArt
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.mpd.MPDRepository

abstract class BaseViewModel(protected val repo: MPDRepository) : ViewModel() {
    val currentSong = repo.currentSong
    val currentSongFilename = repo.currentSong.map { it?.filename }.distinctUntilChanged()
    val isStreaming = repo.streamPlayer.isStreaming
    val playerState = repo.playerState
    val storedPlaylists = repo.engines.playlist.storedPlaylists
    val volume = repo.volume

    fun addMessage(message: String) = repo.engines.message.addMessage(message)

    fun enqueueAlbum(album: MPDAlbum) {
        repo.engines.control.enqueueAlbumLast(album) { response ->
            if (response.isSuccess) repo.engines.message.addMessage("The album was enqueued.")
            else repo.engines.message.addMessage("Could not enqeue album: ${response.error}")
        }
    }

    fun enqueueSong(song: MPDSong) = repo.engines.control.enqueueSongLast(song) { response ->
        if (response.isSuccess) repo.engines.message.addMessage("The song was enqueued.")
        else repo.engines.message.addMessage("Could not enqueue song: ${response.error}")
    }

    fun getAlbumArt(song: MPDSong, callback: (MPDAlbumArt) -> Unit) = viewModelScope.launch {
        repo.engines.image.getAlbumArt(song.albumArtKey, ImageRequestType.FULL, callback)
    }

    fun playAlbum(album: MPDAlbum?) = album?.let { repo.engines.control.enqueueAlbumNextAndPlay(album) }

    fun playOrPauseSong(song: MPDSong) {
        if (song.filename == repo.currentSong.value?.filename) repo.engines.control.playOrPause()
        else if (song.id != null) repo.engines.control.playSongId(song.id)
        else repo.engines.control.enqueueSongNextAndPlay(song)
    }
}

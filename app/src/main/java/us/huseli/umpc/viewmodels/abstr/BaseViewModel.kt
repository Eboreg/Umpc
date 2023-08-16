package us.huseli.umpc.viewmodels.abstr

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.mpd.response.MPDBatchMapResponse
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.repository.MessageRepository
import us.huseli.umpc.repository.SnackbarMessage
import java.io.File

abstract class BaseViewModel(
    protected val repo: MPDRepository,
    protected val messageRepo: MessageRepository,
    context: Context,
) : ViewModel() {
    val albumArtDirectory = File(context.cacheDir, "albumArt").apply { mkdirs() }
    val thumbnailDirectory = File(albumArtDirectory, "thumbnails").apply { mkdirs() }
    val currentSong = repo.currentSong
    val currentSongFilename = repo.currentSong.map { it?.filename }.distinctUntilChanged()
    val playerState = repo.playerState
    val protocolVersion = repo.protocolVersion
    val storedPlaylists = repo.playlists
    val volume = repo.volume

    init {
        viewModelScope.launch {
            repo.error.collect { error ->
                if (error != null) messageRepo.addError(error)
            }
        }
    }

    fun addError(message: String) = messageRepo.addError(message)

    fun addError(message: SnackbarMessage) = messageRepo.addError(message)

    fun addMessage(message: String) = messageRepo.addMessage(message)

    fun addMessage(message: SnackbarMessage) = messageRepo.addMessage(message)

    fun enqueueAlbumLast(album: MPDAlbum, onFinish: (MPDBatchMapResponse) -> Unit) =
        repo.enqueueAlbumLast(album, onFinish)

    fun enqueueSongLast(song: MPDSong, onFinish: (MPDBatchMapResponse) -> Unit) =
        repo.enqueueSongLast(song, onFinish)

    fun playAlbum(album: MPDAlbum?) = album?.let { repo.enqueueAlbumNextAndPlay(album) }

    fun playOrPause() = repo.playOrPause()

    fun playOrPauseSong(song: MPDSong) {
        if (song == repo.currentSong.value) playOrPause()
        else if (song.id != null) repo.playSong(song)
        else repo.enqueueSongNext(song) { repo.playSong(song) }
    }

    fun playSongByPosition(pos: Int) = repo.playSongByPosition(pos)
}

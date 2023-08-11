package us.huseli.umpc.viewmodels.abstr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.umpc.ImageRequestType
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDAlbumArt
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.data.MPDVersion
import us.huseli.umpc.mpd.mpdFindAdd
import us.huseli.umpc.mpd.mpdFindPre021
import us.huseli.umpc.mpd.response.MPDMapResponse
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.repository.SnackbarMessage
import kotlin.math.min

abstract class BaseViewModel(val repo: MPDRepository) : ViewModel() {
    val currentSong = repo.currentSong
    val currentSongAlbumArt = repo.currentSongAlbumArt
    val currentSongFilename = repo.currentSong.map { it?.filename }.distinctUntilChanged()
    val playerState = repo.playerState
    val protocolVersion = repo.protocolVersion
    val storedPlaylists = repo.storedPlaylists
    val volume = repo.volume

    fun addError(message: String) = repo.messageRepository.addError(message)

    fun addError(message: SnackbarMessage) = repo.messageRepository.addError(message)

    fun addMessage(message: String) = repo.messageRepository.addMessage(message)

    fun addMessage(message: SnackbarMessage) = repo.messageRepository.addMessage(message)

    fun enqueueAlbumLast(album: MPDAlbum, onFinish: (MPDMapResponse) -> Unit) =
        repo.client.enqueue(album.getSearchFilter(repo.protocolVersion.value).findadd(), onFinish = onFinish)

    fun enqueueSongLast(song: MPDSong, onFinish: (MPDMapResponse) -> Unit) =
        repo.enqueueSongLast(song.filename, onFinish)

    fun getThumbnail(album: MPDAlbumWithSongs, onFinish: (MPDAlbumArt) -> Unit) = viewModelScope.launch {
        album.albumArtKey?.let { repo.getAlbumArt(it, ImageRequestType.THUMBNAIL, onFinish) }
    }

    private fun enqueueSongNextAndPlay(song: MPDSong) {
        val args =
            if (repo.currentSongId.value != null) listOf(song.filename, "+0")
            else listOf(song.filename)

        repo.client.enqueue("addid", args) { response ->
            response.responseMap["Id"]?.get(0)?.toInt()?.let { playSongById(it) }
        }
    }

    fun getAlbumArt(song: MPDSong, callback: (MPDAlbumArt) -> Unit) =
        repo.getAlbumArt(song.albumArtKey, ImageRequestType.FULL, callback)

    fun playAlbum(album: MPDAlbum?) = album?.let {
        val addPosition = if (repo.currentSongPosition.value != null) 0 else null
        val firstSongPosition = min(
            repo.currentSongPosition.value?.plus(1) ?: repo.queue.value.size,
            repo.queue.value.size
        )
        val command =
            if (repo.protocolVersion.value < MPDVersion("0.21"))
                mpdFindPre021 { equals("album", album.name) and equals("albumartist", album.artist) }
            else mpdFindAdd(addPosition) { equals("album", album.name) and equals("albumartist", album.artist) }

        repo.client.enqueue(command) { response ->
            if (response.isSuccess) playSongByPosition(firstSongPosition)
        }
    }

    fun playOrPause() = repo.playOrPause()

    fun playOrPauseSong(song: MPDSong) {
        if (song == repo.currentSong.value) playOrPause()
        else if (song.id != null) playSongById(song.id)
        else enqueueSongNextAndPlay(song)
    }

    private fun playSongById(songId: Int) {
        if (repo.currentSongId.value != songId) repo.disableStopAfterCurrent()
        repo.client.enqueue("playid $songId")
    }

    fun playSongByPosition(pos: Int) = repo.playSongByPosition(pos)
}

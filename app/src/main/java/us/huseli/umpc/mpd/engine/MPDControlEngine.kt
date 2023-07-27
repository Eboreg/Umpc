package us.huseli.umpc.mpd.engine

import androidx.annotation.IntRange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.LoggerInterface
import us.huseli.umpc.PlayerState
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.mpd.MPDRepository
import us.huseli.umpc.mpd.command.MPDMapCommand
import us.huseli.umpc.mpd.response.MPDBatchMapResponse
import us.huseli.umpc.mpd.response.MPDMapResponse
import kotlin.math.max
import kotlin.math.min

class MPDControlEngine(private val repo: MPDRepository) : LoggerInterface {
    private val _stopAfterCurrent = MutableStateFlow(false)

    val stopAfterCurrent = _stopAfterCurrent.asStateFlow()

    fun clearQueue(onFinish: ((MPDMapResponse) -> Unit)? = null) = repo.client.enqueue("clear", onFinish = onFinish)

    fun disableStopAfterCurrent() {
        _stopAfterCurrent.value = false
    }

    fun enqueueAlbumLast(album: MPDAlbum, onFinish: (MPDMapResponse) -> Unit) =
        repo.client.enqueue(album.searchFilter.findadd(), onFinish = onFinish)

    fun enqueueAlbums(albums: List<MPDAlbum>, onFinish: ((MPDBatchMapResponse) -> Unit)? = null) =
        repo.client.enqueueBatch(
            commands = albums.map { MPDMapCommand(it.searchFilter.findadd()) },
            onFinish = onFinish,
        )

    fun enqueueAlbumNextAndPlay(album: MPDAlbum) {
        val addPosition = if (repo.currentSongPosition.value != null) 0 else null
        val firstSongPosition = repo.currentSongPosition.value?.plus(1) ?: repo.queue.value.size

        repo.client.enqueue(album.searchFilter.findadd(addPosition)) { response ->
            if (response.isSuccess) playSongByPosition(firstSongPosition)
        }
    }

    fun enqueueSongLast(song: MPDSong, onFinish: ((MPDMapResponse) -> Unit)? = null) =
        enqueueSongLast(song.filename, onFinish)

    fun enqueueSongLast(filename: String, onFinish: ((MPDMapResponse) -> Unit)? = null) =
        repo.client.enqueue("add", filename, onFinish)

    fun enqueueSongNextAndPlay(song: MPDSong) {
        val args =
            if (repo.currentSongId.value != null) listOf(song.filename, "+0")
            else listOf(song.filename)

        repo.client.enqueue("addid", args) { response ->
            response.responseMap["Id"]?.get(0)?.toInt()?.let { playSongById(it) }
        }
    }

    fun enqueueSongs(songs: List<MPDSong>, onFinish: ((MPDBatchMapResponse) -> Unit)? = null) =
        repo.client.enqueueBatch(
            commands = songs
                .filter { it.position != null }
                .map { MPDMapCommand("add", listOf(it.filename, it.position.toString())) },
            onFinish = onFinish,
        )

    fun enqueueSongsLast(songs: List<MPDSong>, onFinish: ((MPDBatchMapResponse) -> Unit)? = null) =
        repo.client.enqueueBatch(songs.map { MPDMapCommand("add", it.filename) }, onFinish = onFinish)

    fun moveSongInQueue(fromIdx: Int, toIdx: Int) = repo.client.enqueue("move $fromIdx $toIdx")

    fun next() = ensurePlayerState {
        disableStopAfterCurrent()
        repo.client.enqueue("next")
    }

    fun pause() = ensurePlayerState { repo.client.enqueue("pause 1") }

    fun play() = ensurePlayerState {
        if (repo.currentSongPosition.value == null) disableStopAfterCurrent()
        playSongByPosition(repo.currentSongPosition.value ?: 0)
    }

    fun playOrPause() {
        when (repo.playerState.value) {
            PlayerState.PLAY -> pause()
            PlayerState.STOP -> play()
            PlayerState.PAUSE -> repo.client.enqueue("pause 0")
            PlayerState.UNKNOWN -> repo.loadStatus {
                if (repo.playerState.value != PlayerState.UNKNOWN) playOrPause()
            }
        }
    }

    fun playSongById(songId: Int) {
        if (repo.currentSongId.value != songId) disableStopAfterCurrent()
        repo.client.enqueue("playid $songId")
    }

    fun playSongByPosition(pos: Int) {
        if (repo.currentSongPosition.value != pos) disableStopAfterCurrent()
        repo.client.enqueue("play $pos")
    }

    fun previous() = ensurePlayerState {
        disableStopAfterCurrent()
        repo.client.enqueue("previous")
    }

    fun removeSongFromQueue(song: MPDSong) =
        song.id?.let { repo.client.enqueue("deleteid $it") }

    fun removeSongsFromQueue(songs: List<MPDSong>, onFinish: ((MPDBatchMapResponse) -> Unit)? = null) =
        repo.client.enqueueBatch(
            songs.filter { it.id != null }.map { MPDMapCommand("deleteid ${it.id}") },
            onFinish = onFinish,
        )

    fun seek(time: Double) = repo.client.enqueue("seekcur $time")

    fun seekRelative(time: Double) {
        val timeString = if (time >= 0) "+$time" else time.toString()
        repo.client.enqueue("seekcur $timeString")
    }

    fun setOutputEnabled(id: Int, isEnabled: Boolean) =
        repo.client.enqueue(if (isEnabled) "enableoutput $id" else "disableoutput $id")

    fun setVolume(@IntRange(0, 100) value: Int) =
        repo.client.enqueue("setvol $value")

    fun stop() = ensurePlayerState {
        disableStopAfterCurrent()
        repo.client.enqueue("stop")
    }

    fun toggleRandomState() =
        repo.client.enqueue("random", if (repo.randomState.value) "0" else "1")

    fun toggleRepeatState() =
        repo.client.enqueue("repeat", if (repo.repeatState.value) "0" else "1")

    fun toggleStopAfterCurrent() {
        _stopAfterCurrent.value = !_stopAfterCurrent.value
    }

    fun volumeDown() {
        if (repo.volume.value > 0) {
            val volume = max(0, repo.volume.value - 5)
            repo.client.enqueue("setvol $volume")
        }
    }

    fun volumeUp() {
        if (repo.volume.value < 100) {
            val volume = min(100, repo.volume.value + 5)
            repo.client.enqueue("setvol $volume")
        }
    }

    private fun ensurePlayerState(callback: () -> Unit) {
        if (repo.playerState.value == PlayerState.UNKNOWN) {
            repo.loadStatus {
                if (repo.playerState.value != PlayerState.UNKNOWN) callback()
            }
        } else callback()
    }
}

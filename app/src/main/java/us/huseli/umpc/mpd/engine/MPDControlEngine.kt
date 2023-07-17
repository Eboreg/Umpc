package us.huseli.umpc.mpd.engine

import androidx.annotation.IntRange
import us.huseli.umpc.PlayerState
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.mpd.MPDRepository
import us.huseli.umpc.mpd.response.MPDMapResponse
import kotlin.math.max
import kotlin.math.min

class MPDControlEngine(private val repo: MPDRepository) {
    fun clearQueue(onFinish: ((MPDMapResponse) -> Unit)? = null) {
        repo.client.enqueue("clear", onFinish = onFinish)
    }

    fun enqueueAlbumLast(album: MPDAlbum, onFinish: (MPDMapResponse) -> Unit) {
        repo.client.enqueue(album.searchFilter.findadd(), onFinish = onFinish)
    }

    fun enqueueAlbumNextAndPlay(album: MPDAlbum) {
        val addPosition = if (repo.currentSongPosition.value != null) 0 else null
        val firstSongPosition = repo.currentSongPosition.value?.plus(1) ?: repo.queue.value.size

        repo.client.enqueue(album.searchFilter.findadd(addPosition)) { response ->
            if (response.isSuccess) playSongPosition(firstSongPosition)
        }
    }

    fun enqueueSongLast(song: MPDSong, onFinish: ((MPDMapResponse) -> Unit)? = null) =
        enqueueSongLast(song.filename, onFinish)

    fun enqueueSongLast(filename: String, onFinish: ((MPDMapResponse) -> Unit)? = null) {
        repo.client.enqueue("add", filename, onFinish)
    }

    fun enqueueSongNextAndPlay(song: MPDSong) {
        val args =
            if (repo.currentSongId.value != null) listOf(song.filename, "+0")
            else listOf(song.filename)

        repo.client.enqueue("addid", args) { response ->
            response.responseMap["Id"]?.get(0).let { repo.client.enqueue("playid $it") }
        }
    }

    fun moveSongInQueue(fromIdx: Int, toIdx: Int) = repo.client.enqueue("move $fromIdx $toIdx")

    fun next() = repo.client.enqueue("next")

    private fun pause() = repo.client.enqueue("pause 1")

    fun play(position: Int? = null) = playSongPosition(position ?: repo.currentSongPosition.value ?: 0)

    fun playOrPause() {
        when (repo.playerState.value) {
            PlayerState.PLAY -> pause()
            PlayerState.STOP -> play()
            PlayerState.PAUSE -> resume()
        }
    }

    fun playSongId(songId: Int) = repo.client.enqueue("playid $songId")

    private fun playSongPosition(pos: Int) = repo.client.enqueue("play $pos")

    fun previous() = repo.client.enqueue("previous")

    private fun resume() = repo.client.enqueue("pause 0")

    fun seek(time: Double) = repo.client.enqueue("seekcur $time")

    fun seekRelative(time: Double) {
        val timeString = if (time >= 0) "+$time" else time.toString()
        repo.client.enqueue("seekcur $timeString")
    }

    fun setOutputEnabled(id: Int, isEnabled: Boolean) =
        repo.client.enqueue(if (isEnabled) "enableoutput $id" else "disableoutput $id")

    fun setVolume(@IntRange(0, 100) value: Int) =
        repo.client.enqueue("setvol $value")

    fun stop() = repo.client.enqueue("stop")

    fun toggleRandomState() =
        repo.client.enqueue("random", if (repo.randomState.value) "0" else "1")

    fun toggleRepeatState() =
        repo.client.enqueue("repeat", if (repo.repeatState.value) "0" else "1")

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
}

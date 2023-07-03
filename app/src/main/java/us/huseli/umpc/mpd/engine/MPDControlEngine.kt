package us.huseli.umpc.mpd.engine

import androidx.annotation.IntRange
import us.huseli.umpc.PlayerState
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDResponse
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.mpd.MPDRepository
import us.huseli.umpc.mpd.findAdd

class MPDControlEngine(private val repo: MPDRepository) {
    fun enqueueAlbumLast(album: MPDAlbum, onFinish: (MPDResponse) -> Unit) {
        val command = findAdd { and(equals("albumartist", album.artist), equals("album", album.name)) }
        repo.client.value?.enqueue(command, onFinish = onFinish)
    }

    fun enqueueAlbumNextAndPlay(album: MPDAlbum) {
        val command = findAdd("+0") {
            and(equals("albumartist", album.artist), equals("album", album.name))
        }
        repo.client.value?.enqueue(command) { response -> if (response.isSuccess) next() }
    }

    fun enqueueSongLast(song: MPDSong, onFinish: (MPDResponse) -> Unit) {
        repo.client.value?.enqueue("add", listOf(song.filename), onFinish)
    }

    fun enqueueSongNextAndPlay(song: MPDSong) {
        repo.client.value?.enqueue("add", listOf(song.filename, "+0")) { response ->
            if (response.isSuccess) next()
        }
    }

    fun next() = repo.client.value?.enqueue("next")

    private fun pause() = repo.client.value?.enqueue("pause 1")

    fun play(songPosition: Int? = null) =
        repo.client.value?.enqueue(
            "play",
            listOf((songPosition ?: repo.currentSongIndex.value ?: 0).toString())
        )

    fun playOrPause() {
        when (repo.playerState.value) {
            PlayerState.PLAY -> pause()
            PlayerState.STOP -> play()
            PlayerState.PAUSE -> resume()
        }
    }

    fun playSongId(songId: Int) = repo.client.value?.enqueue("playid $songId")

    fun previous() = repo.client.value?.enqueue("previous")

    private fun resume() = repo.client.value?.enqueue("pause 0")

    fun seek(time: Double, songPosition: Int? = null) =
        if (songPosition != null) repo.client.value?.enqueue("seek $songPosition $time")
        else repo.client.value?.enqueue("seekcur $time")

    fun setOutputEnabled(id: Int, isEnabled: Boolean) =
        repo.client.value?.enqueue(if (isEnabled) "enableoutput $id" else "disableoutput $id")

    fun setVolume(@IntRange(0, 100) value: Int) =
        repo.client.value?.enqueue("setvol $value")

    fun stop() = repo.client.value?.enqueue("stop")

    fun toggleRandomState() =
        repo.client.value?.enqueue("random ${if (repo.randomState.value) "0" else "1"}")

    fun toggleRepeatState() =
        repo.client.value?.enqueue("repeat ${if (repo.repeatState.value) "0" else "1"}")
}

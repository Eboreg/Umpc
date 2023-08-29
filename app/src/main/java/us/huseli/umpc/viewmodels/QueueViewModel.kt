package us.huseli.umpc.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.mpd.OnMPDChangeListener
import us.huseli.umpc.mpd.response.MPDTextResponse
import us.huseli.umpc.repository.AlbumArtRepository
import us.huseli.umpc.repository.DynamicPlaylistRepository
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.repository.MessageRepository
import us.huseli.umpc.viewmodels.abstr.SongSelectViewModel
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    repo: MPDRepository,
    messageRepo: MessageRepository,
    albumArtRepo: AlbumArtRepository,
    private val dynamicPlaylistRepo: DynamicPlaylistRepository,
) : SongSelectViewModel(repo, messageRepo, albumArtRepo), OnMPDChangeListener {
    private val removedSongs = mutableListOf<MPDSong>()

    val activeDynamicPlaylist = dynamicPlaylistRepo.activeDynamicPlaylist
    val currentSongPosition = repo.currentSongPosition
    val queue = repo.queue

    init {
        repo.registerOnMPDChangeListener(this)
    }

    inline fun addQueueToPlaylist(playlistName: String, crossinline onFinish: (MPDTextResponse) -> Unit) =
        repo.addQueueToPlaylist(playlistName) { onFinish(it) }

    inline fun clearQueue(crossinline onFinish: (MPDTextResponse) -> Unit) = repo.clearQueue { onFinish(it) }

    fun deactivateDynamicPlaylist() = dynamicPlaylistRepo.deactivateDynamicPlaylist()

    fun moveSong(fromIdx: Int, toIdx: Int) = repo.moveSongInQueue(fromIdx, toIdx)

    fun removeSelectedSongs() {
        removedSongs.clear()
        removedSongs.addAll(selectedSongs.value)
        repo.removeSongsFromQueue(selectedSongs.value) { response ->
            if (response.isSuccess) deselectAllSongs()
        }
    }

    fun removeSong(song: MPDSong) {
        removedSongs.clear()
        removedSongs.add(song)
        repo.removeSongFromQueue(song)
    }

    fun undoRemoveSongs() =
        repo.enqueueSongs(removedSongs) { response ->
            if (response.isSuccess) removedSongs.clear()
        }

    override fun onMPDChanged(subsystems: List<String>) {
        if (subsystems.contains("playlist")) repo.loadQueue()
    }
}

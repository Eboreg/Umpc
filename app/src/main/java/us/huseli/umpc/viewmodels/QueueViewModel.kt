package us.huseli.umpc.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import dagger.hilt.android.lifecycle.HiltViewModel
import us.huseli.umpc.mpd.MPDRepository
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(repo: MPDRepository) : BaseViewModel(repo) {
    val currentSongId = repo.currentSongId
    val currentSongPosition = repo.currentSongPosition
    val queue = repo.queue
    val listState = LazyListState()

    fun moveSong(fromIdx: Int, toIdx: Int) = repo.engines.control.moveSongInQueue(fromIdx, toIdx)
}

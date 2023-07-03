package us.huseli.umpc.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import us.huseli.umpc.mpd.MPDRepository
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(repo: MPDRepository) : BaseViewModel(repo) {
    val currentSongId = repo.currentSongId
    val currentSongIndex = repo.currentSongIndex
    val queue = repo.queue
}

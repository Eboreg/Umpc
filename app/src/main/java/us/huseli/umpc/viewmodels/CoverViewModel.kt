package us.huseli.umpc.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import us.huseli.umpc.mpd.MPDRepository
import javax.inject.Inject

@HiltViewModel
class CoverViewModel @Inject constructor(repo: MPDRepository) : BaseViewModel(repo) {
    val queue = repo.queue
    val randomState = repo.randomState
    val repeatState = repo.repeatState
    val volume = repo.volume

    fun previous() = repo.engines.control.previous()
    fun seek(time: Double) = repo.engines.control.seek(time)
    fun setVolume(value: Int) = repo.engines.control.setVolume(value)
    fun stop() = repo.engines.control.stop()
    fun toggleRandomState() = repo.engines.control.toggleRandomState()
    fun toggleRepeatState() = repo.engines.control.toggleRepeatState()
}

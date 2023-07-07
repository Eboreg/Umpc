package us.huseli.umpc.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import us.huseli.umpc.mpd.MPDRepository
import javax.inject.Inject

@HiltViewModel
class MPDViewModel @Inject constructor(repo: MPDRepository) : BaseViewModel(repo) {
    val error = repo.engines.message.error
    val message = repo.engines.message.message

    fun clearError() = repo.engines.message.clearError()
    fun clearMessage() = repo.engines.message.clearMessage()
}

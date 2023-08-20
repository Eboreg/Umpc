package us.huseli.umpc.viewmodels

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.repository.MessageRepository
import us.huseli.umpc.viewmodels.abstr.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class InfoViewModel @Inject constructor(
    repo: MPDRepository,
    messageRepo: MessageRepository,
) : BaseViewModel(repo, messageRepo) {
    val stats = repo.stats

    init {
        repo.loadStats()

        viewModelScope.launch {
            repo.connectedServer.collect { if (it != null) repo.loadStats() }
        }
    }
}

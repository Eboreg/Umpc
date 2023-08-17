package us.huseli.umpc.viewmodels

import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.repository.MessageRepository
import us.huseli.umpc.viewmodels.abstr.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class InfoViewModel @Inject constructor(
    repo: MPDRepository,
    messageRepo: MessageRepository,
    @ApplicationContext context: Context,
) : BaseViewModel(repo, messageRepo, context) {
    val stats = repo.stats

    fun loadStats() = repo.loadStats()
}

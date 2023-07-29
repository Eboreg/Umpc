package us.huseli.umpc.mpd.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import us.huseli.umpc.mpd.OnMPDChangeListener
import us.huseli.umpc.mpd.command.MPDMapCommand
import us.huseli.umpc.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MPDIdleClient @Inject constructor(
    ioScope: CoroutineScope,
    settingsRepository: SettingsRepository,
) : MPDBaseClient(ioScope, settingsRepository) {
    private val onMPDChangeListeners = mutableListOf<OnMPDChangeListener>()

    fun registerOnMPDChangeListener(listener: OnMPDChangeListener) {
        onMPDChangeListeners.add(listener)
    }

    override suspend fun start() {
        val command = MPDMapCommand("idle")
        worker?.cancel()

        worker = workerScope.launch {
            while (isActive) {
                when (state.value) {
                    State.PREPARED -> connect(failSilently = true)
                    State.READY -> {
                        val response = command.execute(socket)
                        if (!response.isSuccess) connect(failSilently = true)
                        else {
                            val subsystems = response.extractChanged()
                            onMPDChangeListeners.forEach { it.onMPDChanged(subsystems) }
                        }
                    }
                    else -> delay(1000)
                }
            }
        }
    }
}

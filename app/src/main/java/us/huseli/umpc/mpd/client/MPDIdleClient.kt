package us.huseli.umpc.mpd.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import us.huseli.umpc.mpd.OnMPDChangeListener
import us.huseli.umpc.mpd.request.MPDRequest
import us.huseli.umpc.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MPDIdleClient @Inject constructor(
    ioScope: CoroutineScope,
    settingsRepository: SettingsRepository,
) : BaseMPDClient(ioScope, settingsRepository) {
    private val onMPDChangeListeners = mutableListOf<OnMPDChangeListener>()
    override val soTimeout = 0

    fun registerOnMPDChangeListener(listener: OnMPDChangeListener) {
        onMPDChangeListeners.add(listener)
    }

    override suspend fun start() {
        worker?.cancel()

        worker = workerScope.launch {
            val request = MPDRequest("idle")

            while (isActive) {
                when (_state.value) {
                    State.PREPARED -> connect()
                    State.READY -> catchError(request) {
                        request.execute(socket).also { response ->
                            if (!response.isSuccess) connect()
                            else response.extractValuesOrNull("changed")?.also { subsystems ->
                                onMPDChangeListeners.forEach { it.onMPDChanged(subsystems) }
                            }
                        }
                    }
                    else -> delay(1000)
                }
            }
        }
    }
}

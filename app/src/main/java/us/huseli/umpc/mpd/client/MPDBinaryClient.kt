package us.huseli.umpc.mpd.client

import kotlinx.coroutines.CoroutineScope
import us.huseli.umpc.Constants.BINARY_LIMIT
import us.huseli.umpc.data.MPDServerCapability
import us.huseli.umpc.mpd.request.MPDBinaryRequest
import us.huseli.umpc.mpd.request.MPDRequest
import us.huseli.umpc.mpd.response.MPDBinaryResponse
import us.huseli.umpc.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MPDBinaryClient @Inject constructor(
    ioScope: CoroutineScope,
    settingsRepository: SettingsRepository,
) : BaseMPDClient(ioScope, settingsRepository) {
    override val retryInterval = 10000L

    override suspend fun connect() {
        super.connect()
        if (
            _state.value == State.READY &&
            connectedServer.value?.hasCapability(MPDServerCapability.BINARYLIMIT) == true
        ) enqueue(MPDRequest("binarylimit $BINARY_LIMIT"))
    }

    inline fun enqueue(command: String, arg: String, crossinline onFinish: (MPDBinaryResponse) -> Unit) =
        MPDBinaryRequest(command, listOf(arg)) { onFinish(it) }.also { enqueue(it) }
}

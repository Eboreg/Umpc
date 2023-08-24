package us.huseli.umpc.mpd.client

import kotlinx.coroutines.CoroutineScope
import us.huseli.umpc.formatMPDCommand
import us.huseli.umpc.mpd.request.MPDBatchRequest
import us.huseli.umpc.mpd.request.MPDRequest
import us.huseli.umpc.mpd.response.MPDBatchTextResponse
import us.huseli.umpc.mpd.response.MPDTextResponse
import us.huseli.umpc.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MPDClient @Inject constructor(
    ioScope: CoroutineScope,
    settingsRepository: SettingsRepository,
) : BaseMPDClient(ioScope, settingsRepository) {
    fun enqueue(
        command: String,
        args: Collection<Any> = emptyList(),
        onFinish: ((MPDTextResponse) -> Unit)? = null,
    ) = enqueue(MPDRequest(formatMPDCommand(command, args), onFinish))

    fun enqueue(command: String, onFinish: ((MPDTextResponse) -> Unit)? = null) =
        enqueue(command, emptyList(), onFinish = onFinish)

    fun enqueue(command: String, arg: Any, onFinish: ((MPDTextResponse) -> Unit)? = null) =
        enqueue(command, listOf(arg), onFinish)

    inline fun enqueueBatch(commands: Iterable<String>, crossinline onFinish: (MPDBatchTextResponse) -> Unit) =
        enqueue(MPDBatchRequest(commands) { onFinish(it) })
}

package us.huseli.umpc.mpd.client

import kotlinx.coroutines.CoroutineScope
import us.huseli.umpc.formatMPDCommand
import us.huseli.umpc.mpd.command.MPDBatchCommand
import us.huseli.umpc.mpd.command.MPDCommand
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
        args: Collection<*> = emptyList<Any>(),
        onFinish: ((MPDTextResponse) -> Unit)? = null,
    ) = MPDCommand(formatMPDCommand(command, args), onFinish).also { enqueue(it) }

    fun enqueue(command: String, onFinish: ((MPDTextResponse) -> Unit)? = null) =
        enqueue(command, emptyList<Any>(), onFinish)

    fun enqueue(command: String, arg: Any, onFinish: ((MPDTextResponse) -> Unit)? = null) =
        enqueue(command, listOf(arg), onFinish)

    fun enqueueBatch(commands: List<String>, onFinish: ((MPDBatchTextResponse) -> Unit)? = null) =
        MPDBatchCommand(commands, onFinish).also { enqueue(it) }
}

package us.huseli.umpc.mpd.client

import kotlinx.coroutines.CoroutineScope
import us.huseli.umpc.Constants.BINARY_LIMIT
import us.huseli.umpc.mpd.command.MPDBinaryCommand
import us.huseli.umpc.mpd.command.MPDMapCommand
import us.huseli.umpc.mpd.response.MPDBinaryResponse

class MPDBinaryClient(ioScope: CoroutineScope) : MPDClient(ioScope) {
    override suspend fun connect(failSilently: Boolean): Boolean {
        return if (super.connect(failSilently)) {
            socket.soTimeout = 500
            enqueue(MPDMapCommand("binarylimit $BINARY_LIMIT"))
            true
        } else false
    }

    private fun enqueueBinary(
        command: String,
        args: Collection<String>,
        onFinish: ((MPDBinaryResponse) -> Unit)?
    ) = enqueue(MPDBinaryCommand(command, args, onFinish))

    fun enqueueBinary(command: String, arg: String, onFinish: ((MPDBinaryResponse) -> Unit)?) =
        enqueueBinary(command, listOf(arg), onFinish)
}

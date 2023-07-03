package us.huseli.umpc.mpd.client

import kotlinx.coroutines.CoroutineScope
import us.huseli.umpc.Constants
import us.huseli.umpc.data.MPDCredentials
import us.huseli.umpc.data.MPDResponse
import us.huseli.umpc.mpd.command.MPDBinaryCommand
import us.huseli.umpc.mpd.command.MPDCommand

class MPDBinaryClient(ioScope: CoroutineScope, credentials: MPDCredentials) : MPDBaseClient(ioScope, credentials) {
    override suspend fun initialize() {
        connect()
        enqueue(MPDCommand("binarylimit ${Constants.BINARY_LIMIT}"))
        startWorker()
    }

    override suspend fun connect() {
        super.connect()
        socket.value.soTimeout = 500
    }

    override fun enqueue(
        command: String,
        args: Collection<String>,
        onFinish: ((MPDResponse) -> Unit)?
    ) = enqueue(MPDBinaryCommand(command, args, onFinish))
}

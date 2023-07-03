package us.huseli.umpc.mpd

import kotlinx.coroutines.CoroutineScope
import us.huseli.umpc.Constants
import us.huseli.umpc.data.MPDCredentials
import us.huseli.umpc.data.MPDResponse

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

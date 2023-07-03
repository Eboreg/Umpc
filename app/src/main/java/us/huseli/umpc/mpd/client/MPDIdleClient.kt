package us.huseli.umpc.mpd.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import us.huseli.umpc.data.MPDCredentials
import us.huseli.umpc.data.MPDResponse
import us.huseli.umpc.mpd.command.MPDCommand

class MPDIdleClient(ioScope: CoroutineScope, credentials: MPDCredentials) : MPDBaseClient(ioScope, credentials) {
    fun start(args: Collection<String> = emptyList(), onFinish: (MPDResponse) -> Unit) {
        val command = MPDCommand("idle", args)

        worker = ioScope.launch {
            while (isActive) {
                val response = command.execute(socket.value)
                if (response.status == MPDResponse.Status.EMPTY) connect()
                else onFinish(response)
            }
        }
    }

    override suspend fun initialize() {
        connect()
    }
}

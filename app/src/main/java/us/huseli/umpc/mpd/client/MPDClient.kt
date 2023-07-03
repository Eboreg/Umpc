package us.huseli.umpc.mpd.client

import kotlinx.coroutines.CoroutineScope
import us.huseli.umpc.data.MPDCredentials

class MPDClient(ioScope: CoroutineScope, credentials: MPDCredentials) : MPDBaseClient(ioScope, credentials) {
    override suspend fun initialize() {
        connect()
        startWorker()
    }
}

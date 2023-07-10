package us.huseli.umpc.mpd.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import us.huseli.umpc.mpd.command.MPDMapCommand
import us.huseli.umpc.mpd.response.MPDMapResponse

class MPDIdleClient(ioScope: CoroutineScope) : MPDClient(ioScope) {
    fun start(onFinish: (MPDMapResponse) -> Unit) {
        val command = MPDMapCommand("idle")

        workerScope.launch {
            while (isActive) {
                when (state.value) {
                    State.PREPARED -> connect(failSilently = true)
                    State.READY -> {
                        val response = command.execute(socket)
                        if (!response.isSuccess) connect(failSilently = true)
                        else onFinish(response)
                    }
                    else -> delay(1000)
                }
            }
        }
    }
}

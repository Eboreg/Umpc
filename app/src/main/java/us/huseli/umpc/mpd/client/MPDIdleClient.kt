package us.huseli.umpc.mpd.client

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import us.huseli.umpc.mpd.command.MPDCommand
import us.huseli.umpc.mpd.response.MPDResponse

class MPDIdleClient : MPDClient() {
    fun start(onFinish: (MPDResponse) -> Unit) {
        val command = MPDCommand("idle")

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

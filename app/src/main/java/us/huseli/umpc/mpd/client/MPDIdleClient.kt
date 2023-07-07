package us.huseli.umpc.mpd.client

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import us.huseli.umpc.data.MPDResponse
import us.huseli.umpc.mpd.command.MPDCommand
import us.huseli.umpc.mpd.command.MPDCommandException

class MPDIdleClient : MPDClient() {
    fun start(onFinish: (MPDResponse) -> Unit) {
        val command = MPDCommand("idle")

        workerScope.launch {
            while (isActive) {
                when (state.value) {
                    State.PREPARED -> connect(failSilently = true)
                    State.READY -> {
                        try {
                            onFinish(command.execute(socket))
                        } catch (e: MPDCommandException) {
                            connect(failSilently = true)
                        }
                    }
                    else -> delay(1000)
                }
            }
        }
    }
}

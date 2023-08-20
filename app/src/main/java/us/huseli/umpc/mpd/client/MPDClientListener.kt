package us.huseli.umpc.mpd.client

import us.huseli.umpc.mpd.command.BaseMPDCommand

interface MPDClientListener {
    fun onMPDClientError(client: BaseMPDClient, exception: Throwable, command: BaseMPDCommand<*>? = null)
}

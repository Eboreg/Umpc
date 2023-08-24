package us.huseli.umpc.mpd.client

import us.huseli.umpc.mpd.request.BaseMPDRequest

interface MPDClientListener {
    fun onMPDClientError(client: BaseMPDClient, exception: Throwable, request: BaseMPDRequest<*>? = null)
}

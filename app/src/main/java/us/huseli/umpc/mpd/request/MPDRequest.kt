package us.huseli.umpc.mpd.request

import us.huseli.umpc.mpd.response.MPDTextResponse

open class MPDRequest(
    command: String,
    onFinish: ((MPDTextResponse) -> Unit)? = null,
) : BaseMPDTextRequest<MPDTextResponse>(command, onFinish) {
    override fun getEmptyResponse() = MPDTextResponse()

    override fun equals(other: Any?) = other is MPDRequest && other.command == command

    override fun hashCode(): Int = command.hashCode()

    override fun toString() = "${javaClass.simpleName}[$command]"
}

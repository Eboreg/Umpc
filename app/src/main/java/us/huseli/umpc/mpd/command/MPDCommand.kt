package us.huseli.umpc.mpd.command

import us.huseli.umpc.mpd.response.MPDTextResponse

open class MPDCommand(
    command: String,
    onFinish: ((MPDTextResponse) -> Unit)? = null,
) : BaseMPDTextCommand<MPDTextResponse>(command, onFinish) {
    override fun getEmptyResponse() = MPDTextResponse()

    override fun equals(other: Any?) = other is MPDCommand && other.command == command

    override fun hashCode(): Int = command.hashCode()

    override fun toString() = "${javaClass.simpleName}[$command]"
}

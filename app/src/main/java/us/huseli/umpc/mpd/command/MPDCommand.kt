package us.huseli.umpc.mpd.command

import us.huseli.umpc.mpd.response.MPDTextResponse
import java.net.Socket

open class MPDCommand(
    val command: String,
    onFinish: ((MPDTextResponse) -> Unit)? = null,
) : BaseMPDCommand<MPDTextResponse>(onFinish) {
    override suspend fun getResponse(socket: Socket): MPDTextResponse =
        getTextResponse(socket, command)

    override fun equals(other: Any?) =
        other is MPDCommand && other.command == command

    override fun hashCode(): Int = command.hashCode()

    override fun toString() = "${javaClass.simpleName}[$command]"
}

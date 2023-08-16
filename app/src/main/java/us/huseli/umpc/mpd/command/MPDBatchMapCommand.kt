package us.huseli.umpc.mpd.command

import us.huseli.umpc.mpd.response.MPDBatchMapResponse
import java.net.Socket

class MPDBatchMapCommand(
    val commands: Collection<String>,
    onFinish: ((MPDBatchMapResponse) -> Unit)? = null
) : MPDBaseCommand<MPDBatchMapResponse>(onFinish) {
    override suspend fun getResponse(socket: Socket): MPDBatchMapResponse {
        val command = (listOf("command_list_ok_begin") + commands + listOf("command_list_end")).joinToString("\n")
        return withSocket(socket) { fillTextResponse(command, MPDBatchMapResponse()) }
    }

    override fun equals(other: Any?) =
        other is MPDBatchMapCommand && other.commands == commands

    override fun hashCode(): Int = commands.hashCode()

    override fun toString() = "${javaClass.simpleName}[$commands]"
}

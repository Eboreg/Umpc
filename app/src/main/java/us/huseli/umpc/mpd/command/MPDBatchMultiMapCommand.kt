package us.huseli.umpc.mpd.command

import us.huseli.umpc.mpd.response.MPDBatchMultiMapResponse
import java.net.Socket

class MPDBatchMultiMapCommand(
    val commands: Collection<String>,
    onFinish: ((MPDBatchMultiMapResponse) -> Unit)? = null
) : MPDBaseCommand<MPDBatchMultiMapResponse>(onFinish) {
    override suspend fun getResponse(socket: Socket): MPDBatchMultiMapResponse {
        val command = (listOf("command_list_ok_begin") + commands + listOf("command_list_end")).joinToString("\n")
        return withSocket(socket) { fillTextResponse(command, MPDBatchMultiMapResponse()) }
    }

    override fun equals(other: Any?) = other is MPDBatchMultiMapCommand && other.commands == commands

    override fun hashCode(): Int = commands.hashCode()
}

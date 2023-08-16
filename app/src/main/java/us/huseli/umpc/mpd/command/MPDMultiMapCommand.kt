package us.huseli.umpc.mpd.command

import us.huseli.umpc.formatMPDCommand
import us.huseli.umpc.mpd.response.MPDMultiMapResponse
import java.net.Socket

class MPDMultiMapCommand(
    val command: String,
    val args: Collection<*> = emptyList<Any>(),
    onFinish: ((MPDMultiMapResponse) -> Unit)? = null,
) : MPDBaseCommand<MPDMultiMapResponse>(onFinish) {
    override suspend fun getResponse(socket: Socket): MPDMultiMapResponse =
        withSocket(socket) { fillTextResponse(formatMPDCommand(command, args), MPDMultiMapResponse()) }

    override fun equals(other: Any?) =
        other is MPDMultiMapCommand && other.command == command && other.args == args

    override fun hashCode(): Int = 31 * command.hashCode() + args.hashCode()

    override fun toString() = "${javaClass.simpleName}[${formatMPDCommand(command, args)}]"
}

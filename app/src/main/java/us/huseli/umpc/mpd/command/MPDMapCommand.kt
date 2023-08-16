package us.huseli.umpc.mpd.command

import us.huseli.umpc.formatMPDCommand
import us.huseli.umpc.mpd.response.MPDMapResponse
import java.net.Socket

class MPDMapCommand(
    val command: String,
    val args: Collection<*> = emptyList<Any>(),
    onFinish: ((MPDMapResponse) -> Unit)? = null,
) : MPDBaseCommand<MPDMapResponse>(onFinish) {
    constructor(command: String, arg: String, onFinish: ((MPDMapResponse) -> Unit)? = null) :
        this(command, listOf(arg), onFinish)

    override suspend fun getResponse(socket: Socket): MPDMapResponse =
        withSocket(socket) { fillTextResponse(formatMPDCommand(command, args), MPDMapResponse()) }

    override fun equals(other: Any?) =
        other is MPDMapCommand && other.command == command && other.args == args

    override fun hashCode(): Int = 31 * command.hashCode() + args.hashCode()

    override fun toString() = "${javaClass.simpleName}[${formatMPDCommand(command, args)}]"
}

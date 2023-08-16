package us.huseli.umpc.mpd.command

import us.huseli.umpc.formatMPDCommand
import us.huseli.umpc.mpd.response.MPDListResponse
import java.net.Socket

class MPDListCommand(
    val command: String,
    val key: String,
    val args: Collection<Any> = emptyList(),
    onFinish: ((MPDListResponse) -> Unit)? = null,
) : MPDBaseCommand<MPDListResponse>(onFinish) {
    override suspend fun getResponse(socket: Socket) =
        withSocket(socket) { fillTextResponse(formatMPDCommand(command, args), MPDListResponse(key)) }

    override fun equals(other: Any?) =
        other is MPDListCommand && other.command == command && other.key == key && other.args == args

    override fun hashCode(): Int = 31 * (31 * command.hashCode() + key.hashCode()) + args.hashCode()
}

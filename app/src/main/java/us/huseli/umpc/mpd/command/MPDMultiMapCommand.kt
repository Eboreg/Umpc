package us.huseli.umpc.mpd.command

import us.huseli.umpc.mpd.response.MPDBaseResponse
import us.huseli.umpc.mpd.response.MPDMultiMapResponse
import java.net.Socket

class MPDMultiMapCommand(
    val command: String,
    val args: Collection<String> = emptyList(),
    onFinish: ((MPDMultiMapResponse) -> Unit)? = null,
) : MPDBaseCommand<MPDMultiMapResponse>(onFinish) {
    override suspend fun getResponse(socket: Socket): MPDMultiMapResponse {
        return try {
            withSocket(socket) { fillTextResponse(getCommand(command, args), MPDMultiMapResponse()) }
        } catch (e: Exception) {
            MPDMultiMapResponse().finish(
                status = MPDBaseResponse.Status.ERROR_NET,
                exception = e,
            )
        }
    }

    override fun equals(other: Any?) =
        other is MPDMultiMapCommand && other.command == command && other.args == args

    override fun hashCode(): Int = 31 * command.hashCode() + args.hashCode()

    override fun toString() = "${javaClass.simpleName}[${getCommand(command, args)}]"
}

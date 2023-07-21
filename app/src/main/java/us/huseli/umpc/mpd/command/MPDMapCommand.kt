package us.huseli.umpc.mpd.command

import us.huseli.umpc.mpd.response.MPDBaseResponse
import us.huseli.umpc.mpd.response.MPDMapResponse
import java.net.Socket

class MPDMapCommand(
    val command: String,
    val args: Collection<String> = emptyList(),
    onFinish: ((MPDMapResponse) -> Unit)? = null,
) : MPDBaseCommand<MPDMapResponse>(onFinish) {
    constructor(command: String, arg: String, onFinish: ((MPDMapResponse) -> Unit)? = null) :
        this(command, listOf(arg), onFinish)

    override suspend fun getResponse(socket: Socket): MPDMapResponse {
        return try {
            withSocket(socket) { fillTextResponse(getCommand(command, args), MPDMapResponse()) }
        } catch (e: Exception) {
            MPDMapResponse().finish(
                status = MPDBaseResponse.Status.ERROR_NET,
                exception = e,
            )
        }
    }

    override fun equals(other: Any?) =
        other is MPDMapCommand && other.command == command && other.args == args

    override fun hashCode(): Int = 31 * command.hashCode() + args.hashCode()

    override fun toString() = "${javaClass.simpleName}[${getCommand(command, args)}]"
}

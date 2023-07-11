package us.huseli.umpc.mpd.command

import us.huseli.umpc.mpd.response.MPDMapResponse
import java.net.Socket

class MPDMapCommand(
    command: String,
    args: Collection<String> = emptyList(),
    onFinish: ((MPDMapResponse) -> Unit)? = null,
) : MPDBaseCommand<MPDMapResponse>(command, args, onFinish) {
    constructor(command: String, arg: String, onFinish: ((MPDMapResponse) -> Unit)? = null) :
        this(command, listOf(arg), onFinish)

    override suspend fun getResponse(socket: Socket): MPDMapResponse {
        return withSocket(socket) {
            fillTextResponse(MPDMapResponse())
        }
    }
}

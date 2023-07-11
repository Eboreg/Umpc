package us.huseli.umpc.mpd.command

import us.huseli.umpc.mpd.response.MPDMultiMapResponse
import java.net.Socket

class MPDMultiMapCommand(
    command: String,
    args: Collection<String> = emptyList(),
    onFinish: ((MPDMultiMapResponse) -> Unit)? = null,
) : MPDBaseCommand<MPDMultiMapResponse>(command, args, onFinish) {
    override suspend fun getResponse(socket: Socket): MPDMultiMapResponse {
        return withSocket(socket) {
            fillTextResponse(MPDMultiMapResponse())
        }
    }
}

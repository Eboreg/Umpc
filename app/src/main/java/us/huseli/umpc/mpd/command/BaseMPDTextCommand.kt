package us.huseli.umpc.mpd.command

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.huseli.umpc.data.toMPDError
import us.huseli.umpc.mpd.response.BaseMPDResponse
import java.io.PrintWriter
import java.net.Socket
import java.net.SocketException

abstract class BaseMPDTextCommand<T : BaseMPDResponse>(
    command: String,
    onFinish: ((T) -> Unit)? = null,
) : BaseMPDCommand<T>(command, onFinish) {
    abstract fun getEmptyResponse(): T

    override suspend fun getResponse(socket: Socket): T {
        var line: String?
        val response = getEmptyResponse()

        val inputStream = withContext(Dispatchers.IO) { socket.getInputStream() }
        val writer = withContext(Dispatchers.IO) { PrintWriter(socket.getOutputStream(), true) }

        writer.println(command)

        while (true) {
            line = try {
                readLine(inputStream)
            } catch (e: SocketException) {
                if (socket.isClosed) null
                else throw e
            }

            if (line != null) {
                if (line == "OK") {
                    return response.finish(status = BaseMPDResponse.Status.OK)
                } else if (line.startsWith("ACK ")) {
                    return response.finish(
                        status = BaseMPDResponse.Status.ERROR_MPD,
                        mpdError = line.toMPDError(),
                    )
                } else response.putLine(line)
            } else {
                return response.finish(status = BaseMPDResponse.Status.EMPTY_RESPONSE)
            }
        }
    }
}

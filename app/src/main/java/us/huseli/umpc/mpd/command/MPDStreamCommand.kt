package us.huseli.umpc.mpd.command

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.huseli.umpc.Constants
import us.huseli.umpc.LoggerInterface
import us.huseli.umpc.mpd.response.MPDBaseResponse
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.PrintWriter
import java.net.Socket
import kotlin.math.max

data class MPDResponseLine(val string: String?) {
    enum class Type { PAIR, OK, ACK, NULL }

    val type = when {
        string == null -> Type.NULL
        string == "OK" -> Type.OK
        string.startsWith("ACK ") -> Type.ACK
        else -> Type.PAIR
    }
    val pair: Pair<String, String>?
        get() = MPDBaseResponse.parseResponseLine(string)
}

class MPDStreamCommand : LoggerInterface {
    private val readBuffer = ByteArray(Constants.READ_BUFFER_SIZE)
    private val lineBuffer = ByteArrayOutputStream()
    private val lineChannel = Channel<MPDResponseLine>()
    private val mapChannel = Channel<Map<String, List<String>>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var readBufferReadPos = 0
    private var readBufferWritePos = 0

    suspend fun streamMaps(command: String, socket: Socket) {
        var startKey: String? = null
        var currentMap = mutableMapOf<String, List<String>>()

        scope.launch { execute(command, socket) }

        for (line in lineChannel) {
            line.pair?.let { (key, value) ->
                if (startKey == null) startKey = key
                else if (key == startKey) {
                    mapChannel.send(currentMap)
                    currentMap = mutableMapOf()
                }
                currentMap[key] = currentMap[key]?.let { it + value } ?: listOf(value)
            }
        }
        if (startKey != null) mapChannel.send(currentMap)
    }

    suspend fun execute(command: String, socket: Socket) {
        withContext(Dispatchers.IO) {
            val writer = PrintWriter(socket.getOutputStream(), true)
            val inputStream = socket.getInputStream()

            log("START $this")
            writer.println(command)
            do {
                val line = MPDResponseLine(readLine(inputStream))
                lineChannel.send(line)
            } while (line.type == MPDResponseLine.Type.OK)
            lineChannel.close()
        }
    }

    protected suspend fun readLine(inputStream: InputStream): String? {
        var localReadPos = readBufferReadPos
        lineBuffer.reset()

        while (true) {
            if (localReadPos == readBufferWritePos) {
                lineBuffer.write(readBuffer, readBufferReadPos, localReadPos - readBufferReadPos)
                fillReadBuffer(inputStream)
                if (readBufferWritePos < 1) break
                localReadPos = 0
                continue
            }
            if (readBuffer[localReadPos] == '\n'.code.toByte()) {
                lineBuffer.write(readBuffer, readBufferReadPos, localReadPos - readBufferReadPos)
                readBufferReadPos = localReadPos + 1
                break
            }
            localReadPos++
        }
        return lineBuffer.toString().takeIf { it.isNotEmpty() }
    }

    private suspend fun fillReadBuffer(inputStream: InputStream) {
        withContext(Dispatchers.IO) {
            readBufferWritePos = try {
                max(inputStream.read(readBuffer, 0, Constants.READ_BUFFER_SIZE), 0)
            } catch (e: Exception) {
                log("fillReadBuffer: $e, cause=${e.cause}", Log.ERROR)
                0
            }
            readBufferReadPos = 0
        }
    }

    private fun dataReady(): Int = readBufferWritePos - readBufferReadPos
}

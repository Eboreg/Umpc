package us.huseli.umpc.mpd.command

import android.util.Log
import androidx.annotation.WorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.huseli.umpc.data.MPDResponse
import us.huseli.umpc.mpd.MPDSearch
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.PrintWriter
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import kotlin.math.max
import kotlin.math.min

@WorkerThread
abstract class MPDBaseCommand(
    val command: String,
    val args: Collection<String> = emptyList(),
    val onFinish: ((MPDResponse) -> Unit)? = null,
) {
    private val readBuffer = ByteArray(READ_BUFFER_SIZE)
    private val lineBuffer = ByteArrayOutputStream()
    private var readBufferReadPos = 0
    private var readBufferWritePos = 0

    private lateinit var inputStream: InputStream
    private lateinit var writer: PrintWriter

    private fun dataReady(): Int = readBufferWritePos - readBufferReadPos

    private suspend fun fillReadBuffer() {
        withContext(Dispatchers.IO) {
            @Suppress("LiftReturnOrAssignment")
            try {
                readBufferWritePos = max(inputStream.read(readBuffer, 0, READ_BUFFER_SIZE), 0)
            } catch (e: SocketTimeoutException) {
                Log.e(javaClass.simpleName, "fillReadBuffer: $e, cause=${e.cause}")
                readBufferWritePos = 0
            } catch (e: SocketException) {
                Log.e(javaClass.simpleName, "fillReadBuffer: $e, cause=${e.cause}")
                readBufferWritePos = 0
            }
            readBufferReadPos = 0
        }
    }

    @Suppress("SameParameterValue")
    private suspend fun skipBytes(size: Int) {
        var dataRead = 0
        var readyData: Int
        var dataToRead: Int

        while (dataRead < size) {
            readyData = dataReady()
            dataToRead = min(readyData, size - dataRead)
            dataRead += dataToRead
            readBufferReadPos += dataToRead
            if (dataReady() == 0 && dataRead != size) fillReadBuffer()
        }
    }

    protected suspend fun readLine(): String? {
        var localReadPos = readBufferReadPos
        lineBuffer.reset()

        while (true) {
            if (localReadPos == readBufferWritePos) {
                lineBuffer.write(readBuffer, readBufferReadPos, localReadPos - readBufferReadPos)
                fillReadBuffer()
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

    protected suspend fun readBinary(size: Int): ByteArray {
        val data = ByteArray(size)
        var dataRead = 0
        var dataToRead: Int
        var readyData: Int

        while (dataRead < size) {
            readyData = dataReady()
            dataToRead = min(readyData, size - dataRead)
            System.arraycopy(readBuffer, readBufferReadPos, data, dataRead, dataToRead)
            dataRead += dataToRead
            readBufferReadPos += dataToRead
            if (dataReady() == 0 && dataRead != size) fillReadBuffer()
        }

        skipBytes(1)
        return data
    }

    @Suppress("RedundantSuspendModifier")
    protected suspend fun writeLine(line: String) {
        writer.println(line)
        writer.flush()
    }

    suspend fun execute(socket: Socket): MPDResponse {
        return withContext(Dispatchers.IO) {
            inputStream = socket.getInputStream()
            writer = PrintWriter(socket.getOutputStream())
            getResponse()
        }
    }

    abstract suspend fun getResponse(): MPDResponse

    override fun toString() = "${javaClass.simpleName}[${getCommand(command, args)}]"

    override fun equals(other: Any?) = other is MPDBaseCommand && other.command == command && other.args == args

    override fun hashCode(): Int = 31 * command.hashCode() + args.hashCode()

    companion object {
        const val READ_BUFFER_SIZE = 8192
        val RESPONSE_REGEX = Regex("^([^:]*): (.*)$")

        fun parseResponseLine(line: String): Pair<String, String>? =
            RESPONSE_REGEX.find(line)?.groupValues?.let { if (it.size == 3) it[1] to it[2] else null }

        fun getCommand(command: String, args: Collection<String> = emptyList()) =
            if (args.isEmpty()) command
            else "$command ${args.joinToString(" ") { "\"${MPDSearch.escape(it)}\"" }}"
    }
}

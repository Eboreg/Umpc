package us.huseli.umpc.mpd.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.huseli.umpc.data.MPDCredentials
import us.huseli.umpc.data.MPDResponse
import us.huseli.umpc.mpd.command.MPDBaseCommand
import us.huseli.umpc.mpd.command.MPDCommand
import java.io.BufferedReader
import java.io.Closeable
import java.io.IOException
import java.io.InputStreamReader
import java.net.Socket
import java.net.UnknownHostException

abstract class MPDBaseClient(
    protected val ioScope: CoroutineScope,
    private val credentials: MPDCredentials,
) : Closeable {
    protected val socket = MutableStateFlow(Socket())
    protected var worker: Job? = null
    private val commandQueue = MutableStateFlow<List<MPDBaseCommand>>(emptyList())
    private val wantedTagTypes = listOf(
        "Artist",
        "ArtistSort",
        "Album",
        "AlbumSort",
        "AlbumArtist",
        "AlbumArtistSort",
        "Title",
        "Track",
        "Date",
        "Disc",
    )

    private lateinit var mpdVersion: String

    abstract suspend fun initialize()

    protected open suspend fun connect() {
        try {
            withContext(Dispatchers.IO) {
                val socket = Socket(credentials.hostname, credentials.port)
                val inputStream = socket.getInputStream()
                val reader = BufferedReader(InputStreamReader(inputStream))
                val responseLine = reader.readLine()

                if (responseLine == null || !responseLine.startsWith("OK MPD")) {
                    throw Exception("Expected OK MPD response, got: $responseLine")
                }

                mpdVersion = responseLine.substring(7)
                this@MPDBaseClient.socket.value.close()
                this@MPDBaseClient.socket.value = socket

                if (credentials.password != null) {
                    val response = MPDCommand("password", listOf(credentials.password)).execute(socket)
                    if (!response.isSuccess) {
                        close()
                        throw Exception("Error on password command: ${response.error}")
                    }
                }

                MPDCommand("tagtypes").execute(socket).extractTagTypes().also {
                    MPDCommand("tagtypes disable", it.minus(wantedTagTypes)).execute(socket)
                }
            }
        } catch (e: UnknownHostException) {
            throw e
        } catch (e: IOException) {
            throw e
        }
    }

    fun enqueue(command: MPDBaseCommand) {
        if (!commandQueue.value.contains(command)) {
            commandQueue.value = commandQueue.value.toMutableList().apply { add(command) }
        }
    }

    open fun enqueue(
        command: String,
        args: Collection<String> = emptyList(),
        onFinish: ((MPDResponse) -> Unit)? = null,
    ) = enqueue(MPDCommand(command, args, onFinish))

    protected fun startWorker() {
        worker = ioScope.launch {
            while (isActive) {
                val command: MPDBaseCommand?
                commandQueue.value = commandQueue.value.toMutableList().apply { command = removeFirstOrNull() }
                if (command != null) runCommand(command) else delay(100)
            }
        }
    }

    private suspend fun runCommand(command: MPDBaseCommand) {
        val response = command.execute(socket.value)
        if (response.status == MPDResponse.Status.EMPTY) {
            connect()
            enqueue(command)
        } else {
            command.onFinish?.invoke(response)
        }
    }

    override fun close() {
        worker?.cancel()
        socket.value.close()
    }
}

package us.huseli.umpc.mpd.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.huseli.umpc.data.MPDCredentials
import us.huseli.umpc.mpd.command.MPDBaseCommand
import us.huseli.umpc.mpd.command.MPDCommand
import us.huseli.umpc.mpd.response.MPDResponse
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket

class MPDClientException(
    client: MPDClient,
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause) {
    val clientClass: String = client.javaClass.simpleName
}

open class MPDClient {
    enum class State { STARTED, PREPARED, READY, RUNNING, ERROR }

    private val commandQueue = mutableListOf<MPDBaseCommand>()
    private var credentials: MPDCredentials? = null
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

    protected val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    protected val state = MutableStateFlow(State.STARTED)
    protected var socket = Socket()

    fun enqueue(command: String, onFinish: ((MPDResponse) -> Unit)? = null) =
        enqueue(command, listOf(), onFinish)

    fun enqueue(command: String, arg: String, onFinish: ((MPDResponse) -> Unit)? = null) =
        enqueue(command, listOf(arg), onFinish)

    open fun enqueue(
        command: String,
        args: Collection<String> = emptyList(),
        onFinish: ((MPDResponse) -> Unit)? = null,
    ) = enqueue(MPDCommand(command, args, onFinish))

    fun setCredentials(credentials: MPDCredentials) {
        if (credentials != this.credentials) {
            this.credentials = credentials
            state.value = State.PREPARED
        }
    }

    suspend fun start() {
        workerScope.launch {
            while (isActive) {
                when (state.value) {
                    State.PREPARED -> connect(failSilently = true)
                    State.READY -> {
                        if (commandQueue.isNotEmpty()) {
                            state.value = State.RUNNING
                            runCommand(commandQueue.removeFirst())
                        }
                    }
                    else -> delay(100)
                }
            }
        }
    }

    protected open suspend fun connect(failSilently: Boolean = false): Boolean {
        state.value = State.PREPARED
        return withContext(Dispatchers.IO) {
            try {
                socket.close()
                credentials?.run { socket = Socket(hostname, port) }

                val inputStream = socket.getInputStream()
                val reader = BufferedReader(InputStreamReader(inputStream))
                val responseLine = reader.readLine()

                if (responseLine == null || !responseLine.startsWith("OK MPD")) {
                    throw MPDClientException(this@MPDClient, "Expected OK MPD response, got: $responseLine")
                }

                credentials?.password?.also { password ->
                    val response = MPDCommand("password", password).execute(socket)
                    if (!response.isSuccess) {
                        throw MPDClientException(this@MPDClient, "Error on password command: ${response.error}")
                    }
                }

                MPDCommand("tagtypes").execute(socket).extractTagTypes().also {
                    MPDCommand("tagtypes disable", it.minus(wantedTagTypes)).execute(socket)
                }

                state.value = State.READY
                true
            } catch (e: Exception) {
                state.value = State.ERROR
                if (!failSilently) {
                    if (e is MPDClientException) throw e
                    else throw MPDClientException(this@MPDClient, e.toString(), e)
                }
                false
            }
        }
    }

    protected fun enqueue(command: MPDBaseCommand) {
        if (!commandQueue.contains(command)) {
            commandQueue.add(command)
        }
    }

    private suspend fun runCommand(command: MPDBaseCommand) {
        try {
            state.value = State.RUNNING
            val response = command.execute(socket)
            state.value = State.READY
            command.onFinish?.invoke(response)
        } catch (_: NullPointerException) {
            state.value = State.READY
        }
    }
}

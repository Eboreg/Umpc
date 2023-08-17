package us.huseli.umpc.mpd.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import us.huseli.umpc.LoggerInterface
import us.huseli.umpc.data.MPDCredentials
import us.huseli.umpc.data.MPDServer
import us.huseli.umpc.data.MPDVersion
import us.huseli.umpc.formatMPDCommand
import us.huseli.umpc.mpd.command.BaseMPDCommand
import us.huseli.umpc.mpd.command.MPDCommand
import us.huseli.umpc.mpd.response.BaseMPDResponse
import us.huseli.umpc.repository.SettingsRepository
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket

abstract class MPDBaseClient(
    private val ioScope: CoroutineScope,
    private val settingsRepository: SettingsRepository,
) : LoggerInterface {
    enum class State { STARTED, PREPARED, CONNECTING, READY, RUNNING, ERROR }

    private val _protocolVersion = MutableStateFlow(MPDVersion())
    private val _server = MutableStateFlow<MPDServer?>(null)
    private val commandQueue = MutableStateFlow<List<BaseMPDCommand<*>>>(emptyList())
    private val commandMutex = Mutex()
    private val commandQueueMutex = Mutex()
    protected var credentials: MPDCredentials? = null
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

    protected var worker: Job? = null
    protected val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    protected val state = MutableStateFlow(State.STARTED)
    protected var socket = Socket()

    val protocolVersion = _protocolVersion.asStateFlow()
    val server = _server.asStateFlow()

    init {
        ioScope.launch {
            settingsRepository.credentials.collect { credentials ->
                if (credentials != this@MPDBaseClient.credentials) {
                    this@MPDBaseClient.credentials = credentials
                    state.value = State.PREPARED
                }
                start()
            }
        }
    }

    protected fun <RT : BaseMPDResponse> enqueue(command: BaseMPDCommand<RT>, force: Boolean = false) {
        ioScope.launch {
            commandQueueMutex.withLock {
                if (force || !commandQueue.value.contains(command)) {
                    log("ENQUEUE $command, commandQueue=${commandQueue.value}")
                    commandQueue.value += command
                }
            }
        }
    }

    open suspend fun start() {
        worker?.cancel()
        worker = workerScope.launch {
            while (isActive) {
                when (state.value) {
                    State.PREPARED -> connect(failSilently = true)
                    State.READY -> {
                        commandQueue.value.firstOrNull()?.let { command ->
                            commandQueue.value -= command
                            runCommand(command)
                        }
                    }
                    else -> {}
                }
                delay(100)
            }
        }
    }

    protected open suspend fun connect(failSilently: Boolean = false): Boolean {
        state.value = State.CONNECTING
        return withContext(Dispatchers.IO) {
            try {
                socket.close()
                credentials?.run { socket = Socket(hostname, port) }

                val inputStream = socket.getInputStream()
                val reader = BufferedReader(InputStreamReader(inputStream))
                val responseLine = reader.readLine()

                if (responseLine == null || !responseLine.startsWith("OK MPD")) {
                    throw MPDClientException(this@MPDBaseClient, "Expected OK MPD response, got: $responseLine")
                }

                _protocolVersion.value = MPDVersion(responseLine.substringAfter("OK MPD "))
                _server.value = credentials?.let { MPDServer(it.hostname, it.port) }

                credentials?.password?.also { password ->
                    val response = MPDCommand(formatMPDCommand("password", password)).execute(socket)
                    if (!response.isSuccess) {
                        throw MPDClientException(this@MPDBaseClient, "Error on password command: ${response.error}")
                    }
                }

                MPDCommand("tagtypes").execute(socket).extractValues("tagtype").also {
                    val command = formatMPDCommand("tagtypes disable", it.minus(wantedTagTypes.toSet()))
                    MPDCommand(command).execute(socket)
                }

                state.value = State.READY
                true
            } catch (e: Exception) {
                state.value = State.ERROR
                if (!failSilently) {
                    if (e is MPDClientException) throw e
                    else throw MPDClientException(this@MPDBaseClient, e.toString(), e)
                }
                false
            }
        }
    }

    private suspend fun <RT : BaseMPDResponse> runCommand(command: BaseMPDCommand<RT>) {
        try {
            state.value = State.RUNNING
            val response = commandMutex.withLock { command.execute(socket) }
            if (response.status == BaseMPDResponse.Status.EMPTY_RESPONSE) {
                // connect(failSilently = true)
                state.value = State.PREPARED
                enqueue(command, force = true)
            } else {
                state.value = State.READY
                command.onFinish?.invoke(response)
            }
        } catch (_: NullPointerException) {
            state.value = State.READY
        }
    }
}

package us.huseli.umpc.repository

import androidx.annotation.IntRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import us.huseli.umpc.LoggerInterface
import us.huseli.umpc.PlayerState
import us.huseli.umpc.containsAny
import us.huseli.umpc.data.AlbumArtKey
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.data.MPDAudioFormat
import us.huseli.umpc.data.MPDError
import us.huseli.umpc.data.MPDOutput
import us.huseli.umpc.data.MPDPlaylist
import us.huseli.umpc.data.MPDServerCapability
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.data.MPDStats
import us.huseli.umpc.data.MPDStatus
import us.huseli.umpc.data.groupByAlbum
import us.huseli.umpc.data.sorted
import us.huseli.umpc.data.toMPDStats
import us.huseli.umpc.formatMPDCommand
import us.huseli.umpc.mpd.BaseMPDFilter
import us.huseli.umpc.mpd.BaseMPDFilterContext
import us.huseli.umpc.mpd.MPDFilterContext
import us.huseli.umpc.mpd.MPDFilterContextPre021
import us.huseli.umpc.mpd.OnMPDChangeListener
import us.huseli.umpc.mpd.client.BaseMPDClient
import us.huseli.umpc.mpd.client.MPDBinaryClient
import us.huseli.umpc.mpd.client.MPDClient
import us.huseli.umpc.mpd.client.MPDClientListener
import us.huseli.umpc.mpd.client.MPDIdleClient
import us.huseli.umpc.mpd.command.BaseMPDCommand
import us.huseli.umpc.mpd.command.MPDBatchCommand
import us.huseli.umpc.mpd.mpdFindAdd
import us.huseli.umpc.mpd.mpdFindAddRelative
import us.huseli.umpc.mpd.response.BaseMPDResponse
import us.huseli.umpc.mpd.response.MPDBatchTextResponse
import us.huseli.umpc.mpd.response.MPDBinaryResponse
import us.huseli.umpc.mpd.response.MPDTextResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MPDRepository @Inject constructor(
    private val client: MPDClient,
    private val binaryClient: MPDBinaryClient,
    private val idleClient: MPDIdleClient,
    private val ioScope: CoroutineScope,
) : OnMPDChangeListener, MPDClientListener, LoggerInterface {
    private val _albums = MutableStateFlow<List<MPDAlbum>>(emptyList())
    private val _currentAudioFormat = MutableStateFlow<MPDAudioFormat?>(null)
    private val _currentBitrate = MutableStateFlow<Int?>(null)
    private val _currentSong = MutableStateFlow<MPDSong?>(null)
    private val _currentSongDuration = MutableStateFlow<Double?>(null)
    private val _currentSongElapsed = MutableStateFlow<Double?>(null)
    private val _currentSongId = MutableStateFlow<Int?>(null)
    private val _currentSongPosition = MutableStateFlow<Int?>(null)
    private val _error = MutableStateFlow<String?>(null)
    private val _isConnected = MutableStateFlow(false)
    private val _isIOError = MutableStateFlow(false)
    private val _outputs = MutableStateFlow<List<MPDOutput>>(emptyList())
    private val _playerState = MutableStateFlow(PlayerState.UNKNOWN)
    private val _playlists = MutableStateFlow<List<MPDPlaylist>?>(null)
    private val _queue = MutableStateFlow<List<MPDSong>>(emptyList())
    private val _randomState = MutableStateFlow(false)
    private val _repeatState = MutableStateFlow(false)
    private val _stats = MutableStateFlow<MPDStats?>(null)
    private val _stopAfterCurrent = MutableStateFlow(false)
    private val _volume = MutableStateFlow(100)

    private val fetchedAlbumArtKeys = mutableListOf<AlbumArtKey>()
    private var dbUpdateId: Int? = 0
    private val onUpdateFinishCallbacks = mutableMapOf<Int, () -> Unit>()
    private val pendingAlbumsWithSongsCommands = mutableMapOf<String, MPDBatchCommand>()
    private var statusJob: Job? = null

    private fun mpdFilter(block: BaseMPDFilterContext.() -> BaseMPDFilter): BaseMPDFilter =
        if (serverHasCapability(MPDServerCapability.NEW_FILTER_SYNTAX)) with(MPDFilterContext) { block() }
        else with(MPDFilterContextPre021) { block() }

    val albums = _albums.asStateFlow()
    val connectedServer = client.connectedServer
    val currentAudioFormat = _currentAudioFormat.asStateFlow()
    val currentBitrate = _currentBitrate.asStateFlow()
    val currentSong = _currentSong.asStateFlow()
    val currentSongDuration = _currentSongDuration.asStateFlow()
    val currentSongElapsed = _currentSongElapsed.asStateFlow()
    val currentSongId = _currentSongId.asStateFlow()
    val currentSongPosition = _currentSongPosition.asStateFlow()
    val error = _error.asStateFlow()
    val isConnected = _isConnected.asStateFlow()
    val isIOError = _isIOError.asStateFlow()
    val outputs = _outputs.asStateFlow()
    val playerState = _playerState.asStateFlow()
    val playlists = _playlists.asStateFlow()
    val queue = _queue.asStateFlow()
    val randomState = _randomState.asStateFlow()
    val repeatState = _repeatState.asStateFlow()
    val stats = _stats.asStateFlow()
    val stopAfterCurrent = _stopAfterCurrent.asStateFlow()
    val volume = _volume.asStateFlow()

    init {
        idleClient.registerOnMPDChangeListener(this)
        client.registerListener(this)
        idleClient.registerListener(this)
        binaryClient.registerListener(this)

        ioScope.launch {
            connectedServer.collect {
                if (it == null) _queue.value = emptyList()
                else {
                    loadStatus()
                    loadCurrentSong()
                    loadQueue()
                    loadPlaylists()
                }
            }
        }

        /*
        loadQueue(
            onDataStoreUpdated = {
                ioScope.launch {
                    // If IO error, do not show cached queue.
                    context.queueDataStore.data.combine(isIOError) { queue, isIOError ->
                        if (!isIOError) queue.toNative() else emptyList()
                    }.collect { _queue.value = it }
                }
            },
            onFinish = { _queue.value = it }
        )
         */

        ioScope.launch {
            /**
             * If any client object reports IO error, reset flows and set
             * isIOError = true until that changes.
             */
            combine(idleClient.state, client.state, binaryClient.state) { s1, s2, s3 ->
                listOf(s1, s2, s3).any { it == BaseMPDClient.State.IO_ERROR }
            }.distinctUntilChanged().collect {
                _isIOError.value = it
                if (it) reset()
            }
        }

        ioScope.launch {
            /**
             * isConnected signals that we are verifiably connected to the
             * server. It is not an exact negation of isIOError, since the
             * following states cause them both to be false:
             *
             * 1. Client objects are initialising and not ready yet
             * 2. Server connection works but there was an auth error
             *
             * Not sure which is the optimal way to handle all cases, will have
             * to think it through.
             */
            idleClient.state
                .map { it == BaseMPDClient.State.READY || it == BaseMPDClient.State.RUNNING }
                .distinctUntilChanged()
                .collect { _isConnected.value = it }
        }

        ioScope.launch {
            // While playing, query for status every 10 seconds.
            playerState.collect { playerState ->
                if (playerState == PlayerState.PLAY) {
                    if (statusJob == null || statusJob?.isCancelled == true) {
                        statusJob = ioScope.launch {
                            while (isActive) {
                                delay(10_000)
                                loadStatus()
                            }
                        }
                    }
                } else {
                    statusJob?.cancel()
                    statusJob = null
                }
            }
        }

        ioScope.launch {
            // Fetch (or empty) current song info as soon as it changes.
            currentSongPosition.collect {
                if (stopAfterCurrent.value) {
                    stop()
                    disableStopAfterCurrent()
                } else loadCurrentSong()
            }
        }
    }

    fun addAlbumToPlaylist(album: MPDAlbum, playlistName: String, onFinish: ((MPDBatchTextResponse) -> Unit)? = null) =
        addAlbumsToPlaylist(listOf(album), playlistName, onFinish)

    fun addAlbumsToPlaylist(
        albums: Collection<MPDAlbum>,
        playlistName: String,
        onFinish: ((MPDBatchTextResponse) -> Unit)? = null,
    ) =
        client.enqueueBatch(
            commands = albums
                .map { it.getSearchFilter(connectedServer.value?.protocolVersion) }
                .map { formatMPDCommand("searchaddpl", playlistName, it) },
            onFinish = onFinish,
        )

    fun addQueueToPlaylist(playlistName: String, onFinish: ((MPDTextResponse) -> Unit)? = null) =
        client.enqueue("save", playlistName) { response ->
            if (!response.isSuccess && serverHasCapability(MPDServerCapability.SAVE_APPEND_REPLACE))
                client.enqueue("save", listOf(playlistName, "append"), onFinish)
            else onFinish?.invoke(response)
        }

    fun addSongToPlaylist(song: MPDSong, playlistName: String, onFinish: ((MPDBatchTextResponse) -> Unit)? = null) =
        addSongsToPlaylist(listOf(song), playlistName, onFinish)

    fun addSongsToPlaylist(
        songs: Collection<MPDSong>,
        playlistName: String,
        onFinish: ((MPDBatchTextResponse) -> Unit)? = null,
    ) = client.enqueueBatch(
        commands = songs.map { formatMPDCommand("playlistadd", playlistName, it.filename) },
        onFinish = onFinish,
    )

    fun addSongsToPlaylistPositioned(
        songs: Collection<MPDSong>,
        playlistName: String,
        onFinish: ((MPDBatchTextResponse) -> Unit)? = null,
    ) = client.enqueueBatch(
        commands = songs.sortedBy { it.position }.map {
            if (it.position != null) formatMPDCommand("playlistadd", playlistName, it.filename, it.position)
            else formatMPDCommand("playlistadd", playlistName, it.filename)
        },
        onFinish = onFinish,
    )

    fun clearError(onFinish: ((MPDTextResponse) -> Unit)? = null) = client.enqueue("clearerror", onFinish)

    fun clearQueue(onFinish: ((MPDTextResponse) -> Unit)? = null) = client.enqueue("clear", onFinish)

    fun deletePlaylist(playlistName: String, onFinish: ((MPDTextResponse) -> Unit)? = null) =
        client.enqueue("rm", playlistName, onFinish)

    fun enqueueAlbumLast(album: MPDAlbum, onFinish: ((MPDBatchTextResponse) -> Unit)? = null) =
        enqueueAlbumsLast(listOf(album), onFinish)

    fun enqueueAlbumNextAndPlay(album: MPDAlbum, onFinish: ((BaseMPDResponse) -> Unit)? = null) =
        enqueueAlbumsNextAndPlay(listOf(album), onFinish)

    fun enqueueAlbumsLast(albums: Collection<MPDAlbum>, onFinish: ((MPDBatchTextResponse) -> Unit)? = null) =
        client.enqueueBatch(
            albums.map { it.getSearchFilter(connectedServer.value?.protocolVersion).findadd() },
            onFinish,
        )

    fun enqueueAlbumsNextAndPlay(albums: Collection<MPDAlbum>, onFinish: ((BaseMPDResponse) -> Unit)? = null) {
        val position =
            if (serverHasCapability(MPDServerCapability.SEARCHADD_POSITION))
                currentSongPosition.value?.plus(1) ?: queue.value.size
            else queue.value.size

        client.enqueueBatch(
            albums.reversed().map { album ->
                if (
                    serverHasCapability(MPDServerCapability.SEARCHADD_RELATIVE_POSITION) &&
                    currentSongPosition.value != null
                ) mpdFindAddRelative(0) {
                    equals("album", album.name) and equals("albumartist", album.artist)
                }
                else if (serverHasCapability(MPDServerCapability.SEARCHADD_POSITION))
                    mpdFindAdd(position) { equals("album", album.name) and equals("albumartist", album.artist) }
                else
                    mpdFilter { equals("album", album.name) and equals("albumartist", album.artist) }.findadd()
                /*
                else if (serverHasCapability(MPDServerCapability.NEW_FILTER_SYNTAX))
                    mpdFindAdd { equals("album", album.name) and equals("albumartist", album.artist) }
                else mpdFindAddPre021 { equals("album", album.name) and equals("albumartist", album.artist) }
                 */
            }
        ) { response ->
            if (response.isSuccess) playSongByPosition(position, onFinish)
            else onFinish?.invoke(response)
        }
    }

    fun enqueuePlaylistLast(playlistName: String, onFinish: ((MPDTextResponse) -> Unit)? = null) =
        client.enqueue("load", playlistName, onFinish)

    fun enqueuePlaylistNextAndPlay(playlistName: String, onFinish: ((MPDTextResponse) -> Unit)? = null) {
        val position: Int
        val command: String

        if (serverHasCapability(MPDServerCapability.LOAD_POSITION)) {
            position = currentSongPosition.value?.plus(1) ?: queue.value.size
            val addPosition = if (currentSongPosition.value != null) "+0" else position
            command = formatMPDCommand("load", playlistName, "0:", addPosition)
        } else {
            position = queue.value.size
            command = formatMPDCommand("load", playlistName)
        }

        client.enqueue(command) { response ->
            if (response.isSuccess) playSongByPosition(position, onFinish)
            else onFinish?.invoke(response)
        }
    }

    fun enqueueSongLast(song: MPDSong, onFinish: ((MPDBatchTextResponse) -> Unit)? = null) =
        enqueueSongsLast(listOf(song.filename), onFinish)

    fun enqueueSongNextAndPlay(song: MPDSong, onFinish: ((BaseMPDResponse) -> Unit)? = null) =
        enqueueSongsNextAndPlay(listOf(song), onFinish)

    fun enqueueSongs(songs: Collection<MPDSong>, onFinish: ((MPDBatchTextResponse) -> Unit)? = null) {
        client.enqueueBatch(
            songs.map {
                if (it.position != null) formatMPDCommand("addid", it.filename, it.position)
                else formatMPDCommand("addid", it.filename)
            },
            onFinish,
        )
    }

    fun enqueueSongsLast(filenames: Collection<String>, onFinish: ((MPDBatchTextResponse) -> Unit)? = null) =
        client.enqueueBatch(filenames.map { formatMPDCommand("add", it) }, onFinish)

    fun enqueueSongsNextAndPlay(songs: Collection<MPDSong>, onFinish: ((BaseMPDResponse) -> Unit)? = null) {
        val position = currentSongPosition.value?.plus(1) ?: queue.value.size
        val addPosition =
            if (
                serverHasCapability(MPDServerCapability.ADDID_RELATIVE_POSITION) &&
                currentSongPosition.value != null
            ) "+0"
            else position
        val command = songs.reversed().map { formatMPDCommand("addid", it.filename, addPosition) }

        client.enqueueBatch(command) { response ->
            if (response.isSuccess) playSongByPosition(position, onFinish)
            else onFinish?.invoke(response)
        }
    }

    fun getAlbumArt(key: AlbumArtKey, onFinish: ((MPDBinaryResponse) -> Unit)? = null) {
        if (!fetchedAlbumArtKeys.contains(key)) {
            binaryClient.enqueue("albumart", key.filename) { response ->
                fetchedAlbumArtKeys.add(key)
                onFinish?.invoke(response)
            }
        }
    }

    fun getAlbumWithSongs(album: MPDAlbum, onFinish: (MPDAlbumWithSongs) -> Unit) =
        getAlbumsWithSongs(listOf(album)) { onFinish(it.first()) }

    fun getAlbumsByAlbumArtist(artistName: String, onFinish: (List<MPDAlbum>) -> Unit) =
        client.enqueue(mpdFilter { equals("albumartist", artistName) }.list("album")) { response ->
            onFinish(response.extractAlbums(artistName))
        }

    fun getAlbumsByArtist(artistName: String, onFinish: (List<MPDAlbum>) -> Unit) {
        client.enqueue(mpdFilter { equals("artist", artistName) }.list("album", listOf("albumartist"))) { response ->
            if (response.mpdError?.type == MPDError.Type.ARG) {
                client.enqueue(mpdFilter { equals("artist", artistName) }.find()) { response2 ->
                    onFinish(response2.extractSongs().map { it.album }.toSet().toList())
                }
            } else onFinish(response.extractAlbums())
        }
    }

    fun getAlbumsWithSongs(albums: Collection<MPDAlbum>, onFinish: (List<MPDAlbumWithSongs>) -> Unit) {
        getAlbumsWithSongs("albumartist", albums) { albumsArtistAlbums ->
            val albumsWithSongs = albumsArtistAlbums.filter { it.songs.isNotEmpty() }
            val emptyAlbums = albums.minus(albumsWithSongs.map { it.album }.toSet())

            if (emptyAlbums.isNotEmpty()) {  // The irony, eh?
                getAlbumsWithSongs("artist", emptyAlbums) { artistAlbums ->
                    onFinish(albumsWithSongs + artistAlbums.filter { emptyAlbums.contains(it.album) })
                }
            } else onFinish(albumsArtistAlbums)
        }
    }

    fun loadAlbums(onFinish: ((List<MPDAlbum>) -> Unit)? = null) {
        /**
         * Mopidy-MPD doesn't support the "group" parameter, but we have no way
         * to tell if it's a Mopidy server. So we just have to try and fail!
         */
        client.enqueue("list album group albumartist") { response ->
            if (response.mpdError?.type == MPDError.Type.ARG) {
                client.enqueue("list albumartist") { response2 ->
                    val albumArtists = response2.extractValues("albumartist")

                    client.enqueueBatch(
                        albumArtists.map { mpdFilter { equals("albumartist", it) }.list("album") }
                    ) { response3 ->
                        val nestedMapList = response3.extractNestedMaps()

                        // Each albumartist should now have a list of maps,
                        // where each map is an album.
                        _albums.value = albumArtists
                            .flatMapIndexed { index, artist ->
                                // Convert a list of artist's album titles:
                                nestedMapList[index].mapNotNull { it["album"] }.map { MPDAlbum(artist, it) }
                            }
                            .sorted()
                            .also { onFinish?.invoke(it) }
                    }
                }
            } else _albums.value = response.extractAlbums().sorted().also { onFinish?.invoke(it) }
        }
    }

    fun loadOutputs(onFinish: ((List<MPDOutput>) -> Unit)? = null) =
        client.enqueue("outputs") { response ->
            response.extractOutputs().let { outputs ->
                _outputs.value = outputs
                onFinish?.invoke(outputs)
            }
        }

    fun loadPlaylistsWithSongs(forceReload: Boolean = false, onFinish: ((List<MPDPlaylist>) -> Unit)? = null) {
        loadPlaylists(forceReload = forceReload) { playlists ->
            val playlistsWithoutSongs =
                if (!forceReload) playlists.filter { it.songs == null }
                else playlists

            if (playlistsWithoutSongs.isNotEmpty()) {
                client.enqueueBatch(
                    playlistsWithoutSongs.map { formatMPDCommand("listplaylistinfo", it.name) }
                ) { response ->
                    if (response.isSuccess) {
                        val updatedPlaylists = playlists.toMutableList().apply {
                            playlistsWithoutSongs
                                .zip(response.extractNestedPositionedSongs())
                                .forEach { (playlist, songs) ->
                                    removeIf { it.name == playlist.name }
                                    add(playlist.copy(songs = songs))
                                }
                        }

                        updatedPlaylists.sortedBy { it.name.lowercase() }.also {
                            _playlists.value = it
                            onFinish?.invoke(it)
                        }
                    } else onFinish?.invoke(playlists)
                }
            } else onFinish?.invoke(playlists)
        }
    }

    fun loadPlaylistSongs(playlist: MPDPlaylist, onFinish: ((List<MPDSong>) -> Unit)? = null) {
        client.enqueue("listplaylistinfo", playlist.name) { response ->
            if (response.isSuccess) {
                val songs = response.extractPositionedSongs()

                _playlists.value = _playlists.value?.toMutableList()?.apply {
                    indexOfFirst { it.name == playlist.name }.takeIf { it > -1 }?.also { index ->
                        add(index, removeAt(index).copy(songs = songs))
                    }
                }
                onFinish?.invoke(songs)
            }
        }
    }

    fun loadQueue(onFinish: ((List<MPDSong>) -> Unit)? = null) {
        client.enqueue("playlistinfo") { response ->
            response.extractSongs().also {
                _queue.value = it
                onFinish?.invoke(it)
            }
        }
    }

    fun loadStats(onFinish: ((MPDStats) -> Unit)? = null) = client.enqueue("stats") { response ->
        if (response.isSuccess) _stats.value = response.extractMap().toMPDStats().also { onFinish?.invoke(it) }
    }

    fun moveSongInQueue(fromIdx: Int, toIdx: Int, onFinish: ((MPDTextResponse) -> Unit)? = null) =
        client.enqueue("move $fromIdx $toIdx", onFinish)

    fun moveSongInPlaylist(
        playlistName: String,
        fromIdx: Int,
        toIdx: Int,
        onFinish: ((MPDTextResponse) -> Unit)? = null,
    ) = client.enqueue(formatMPDCommand("playlistmove", playlistName, fromIdx, toIdx), onFinish)

    fun next(onFinish: ((MPDTextResponse) -> Unit)? = null) = ensurePlayerState {
        disableStopAfterCurrent()
        client.enqueue("next", onFinish)
    }

    fun pause(onFinish: ((MPDTextResponse) -> Unit)? = null) =
        ensurePlayerState { client.enqueue("pause 1", onFinish) }

    fun play(onFinish: ((MPDTextResponse) -> Unit)? = null) = ensurePlayerState {
        currentSongId.value?.let {
            playSongById(it, onFinish)
        } ?: kotlin.run {
            disableStopAfterCurrent()
            playSongByPosition(0, onFinish)
        }
    }

    fun playOrPause(onFinish: ((MPDTextResponse) -> Unit)? = null) {
        when (playerState.value) {
            PlayerState.PLAY -> pause(onFinish)
            PlayerState.STOP -> play(onFinish)
            PlayerState.PAUSE -> client.enqueue("pause 0", onFinish)
            PlayerState.UNKNOWN -> loadStatus {
                if (playerState.value != PlayerState.UNKNOWN) playOrPause(onFinish)
            }
        }
    }

    fun playSongById(id: Int, onFinish: ((MPDTextResponse) -> Unit)? = null) {
        if (currentSongId.value != id) disableStopAfterCurrent()
        client.enqueue("playid $id", onFinish)
    }

    fun playSongByPosition(pos: Int, onFinish: ((MPDTextResponse) -> Unit)? = null) {
        if (currentSongPosition.value != pos) disableStopAfterCurrent()
        client.enqueue("play $pos", onFinish)
    }

    fun previousOrRestart(onFinish: ((MPDTextResponse) -> Unit)? = null) {
        if (currentSongElapsed.value?.takeIf { it > 2 } != null) seek(0.0, onFinish)
        else if (currentSongPosition.value?.takeIf { it > 0 } != null) {
            disableStopAfterCurrent()
            client.enqueue("previous", onFinish)
        }
    }

    fun registerOnMPDChangeListener(listener: OnMPDChangeListener) =
        idleClient.registerOnMPDChangeListener(listener)

    fun removeSongFromPlaylist(
        playlistName: String,
        song: MPDSong,
        onFinish: ((MPDBatchTextResponse) -> Unit)? = null,
    ) = song.position?.let { removeSongsFromPlaylist(playlistName, listOf(it), onFinish) }

    fun removeSongsFromPlaylist(
        playlistName: String,
        positions: Collection<Int>,
        onFinish: ((MPDBatchTextResponse) -> Unit)? = null,
    ) =
        client.enqueueBatch(
            positions.sortedDescending().map { formatMPDCommand("playlistdelete", playlistName, it) },
            onFinish,
        )

    fun removeSongFromQueue(song: MPDSong, onFinish: ((MPDBatchTextResponse) -> Unit)? = null) =
        removeSongsFromQueue(listOf(song), onFinish)

    fun removeSongsFromQueue(songs: Collection<MPDSong>, onFinish: ((MPDBatchTextResponse) -> Unit)? = null) =
        client.enqueueBatch(songs.mapNotNull { it.id }.map { "deleteid $it" }, onFinish)

    fun renamePlaylist(playlistName: String, newName: String, onFinish: ((MPDTextResponse) -> Unit)? = null) =
        client.enqueue("rename", listOf(playlistName, newName), onFinish)

    fun search(term: String, onFinish: ((MPDTextResponse) -> Unit)? = null) {
        /**
         * MPD cannot combine search terms with logical OR for some reason, so
         * we cannot select a list of tags to search, but must use "any". As
         * this may give a lot of search results we don't want, additional
         * filtering must be applied.
         *
         * TODO: Maybe disable search all together in <0.21, or implement
         * it in some completely different way
         */
        search(mpdFilter { contains("any", term) }, onFinish)
    }

    fun search(filter: BaseMPDFilter, onFinish: ((MPDTextResponse) -> Unit)? = null) =
        client.enqueue(filter.search(), onFinish)

    fun seek(time: Double, onFinish: ((MPDTextResponse) -> Unit)? = null) =
        client.enqueue("seekcur $time", onFinish)

    fun seekRelative(time: Double, onFinish: ((MPDTextResponse) -> Unit)? = null) {
        val timeString = if (time >= 0) "+$time" else time.toString()
        client.enqueue("seekcur $timeString", onFinish)
    }

    fun setOutputEnabled(id: Int, isEnabled: Boolean, onFinish: ((MPDTextResponse) -> Unit)? = null) =
        client.enqueue(if (isEnabled) "enableoutput $id" else "disableoutput $id", onFinish)

    fun setVolume(@IntRange(0, 100) value: Int, onFinish: ((MPDTextResponse) -> Unit)? = null) =
        client.enqueue("setvol $value", onFinish)

    fun stop(onFinish: ((MPDTextResponse) -> Unit)? = null) = ensurePlayerState {
        disableStopAfterCurrent()
        client.enqueue("stop", onFinish)
    }

    fun toggleRandomState(onFinish: ((MPDTextResponse) -> Unit)? = null) =
        client.enqueue("random", if (randomState.value) "0" else "1", onFinish)

    fun toggleRepeatState(onFinish: ((MPDTextResponse) -> Unit)? = null) =
        client.enqueue("repeat", if (repeatState.value) "0" else "1", onFinish)

    fun toggleStopAfterCurrent() {
        _stopAfterCurrent.value = !_stopAfterCurrent.value
    }

    fun updateDatabase(onFinish: (MPDTextResponse) -> Unit, onUpdateFinish: () -> Unit) =
        client.enqueue("update") { response ->
            log("updateDatabase, ${response.extractMap()}")
            response.extractMap()["updating_db"]?.toInt()?.let { id ->
                onUpdateFinishCallbacks[id] = onUpdateFinish
                onFinish(response)
            }
        }

    private fun disableStopAfterCurrent() {
        _stopAfterCurrent.value = false
    }

    private fun ensurePlayerState(callback: () -> Unit) {
        if (playerState.value == PlayerState.UNKNOWN) {
            loadStatus {
                if (playerState.value != PlayerState.UNKNOWN) callback()
            }
        } else callback()
    }

    private fun getAlbumsWithSongs(
        artistTag: String,
        albums: Collection<MPDAlbum>,
        onFinish: (List<MPDAlbumWithSongs>) -> Unit,
    ) {
        val commands = albums.map { it.getSearchFilter(artistTag, connectedServer.value?.protocolVersion).find() }
        val pendingCommand =
            pendingAlbumsWithSongsCommands[artistTag]?.takeIf { it.status == BaseMPDCommand.Status.PENDING }
        val callback = { response: MPDBatchTextResponse ->
            val songLists = response.extractNestedSongs()
            val albumsWithSongs =
                songLists.flatMap { it.groupByAlbum() }.filter { albums.contains(it.album) }
            onFinish(albumsWithSongs)
        }

        if (pendingCommand != null) {
            pendingCommand.addCommands(commands)
            pendingCommand.addCallback(callback)
        } else {
            pendingAlbumsWithSongsCommands[artistTag] = client.enqueueBatch(commands, callback)
        }
    }

    private fun loadCurrentSong(onFinish: ((MPDSong) -> Unit)? = null) =
        client.enqueue("currentsong") { response ->
            val song = response.extractSong()
            updateCurrentSong(song)
            if (song != null) onFinish?.invoke(song)
        }

    private fun loadPlaylists(forceReload: Boolean = false, onFinish: ((List<MPDPlaylist>) -> Unit)? = null) {
        client.enqueue("listplaylists") { response ->
            response.extractPlaylists().also { playlists ->
                val updatedPlaylists = if (!forceReload) {
                    _playlists.value?.toMutableList()?.apply {
                        removeIf { playlist -> !playlists.map { it.name }.contains(playlist.name) }
                        addAll(playlists.filter { playlist -> !map { it.name }.contains(playlist.name) })
                    } ?: playlists
                } else playlists

                updatedPlaylists.sortedBy { it.name.lowercase() }.also {
                    _playlists.value = it
                    onFinish?.invoke(it)
                }
            }
        }
    }

    /*
    private fun loadQueue(onFinish: ((List<MPDSong>) -> Unit)? = null, onDataStoreUpdated: () -> Unit) =
        client.enqueue("playlistinfo") { response ->
            response.extractSongs().also { songs ->
                ioScope.launch {
                    context.queueDataStore.updateData { currentQueue ->
                        currentQueue.toBuilder()
                            .clearSongs()
                            .addAllSongs(songs.toProto())
                            .build()
                    }
                    onDataStoreUpdated()
                }
                onFinish?.invoke(songs)
            }
        }
     */

    private fun loadStatus(onFinish: ((MPDStatus) -> Unit)? = null) =
        client.enqueue("status") { response ->
            response.extractStatus()?.also { status ->
                updateStatus(status)
                onFinish?.invoke(status)
            }
        }

    private fun serverHasCapability(capability: MPDServerCapability) =
        connectedServer.value?.hasCapability(capability) ?: false

    private fun reset() {
        updateCurrentSong(null)
        updateStatus(MPDStatus())
        _albums.value = emptyList()
        _outputs.value = emptyList()
        _playlists.value = null
        _queue.value = emptyList()
        _stats.value = null
    }

    private fun updateCurrentSong(songOrNull: MPDSong?) {
        _currentSong.value = songOrNull?.also { song ->
            song.position?.let { _currentSongPosition.value = it }
            song.duration?.let { _currentSongDuration.value = it }
            song.id?.let { _currentSongId.value = it }
            song.audioFormat?.let { _currentAudioFormat.value = it }
        }
    }

    private fun updateStatus(status: MPDStatus) {
        status.volume?.let { _volume.value = it }
        status.repeat?.let { _repeatState.value = it }
        status.random?.let { _randomState.value = it }
        status.playerState?.let { _playerState.value = it }
        status.currentSongDuration?.let { _currentSongDuration.value = it }

        _error.value = status.error
        _currentSongElapsed.value = status.currentSongElapsed
        _currentSongId.value = status.currentSongId
        _currentBitrate.value = status.bitrate
        _currentAudioFormat.value = status.audioFormat
        _currentSongPosition.value = status.currentSongPosition

        if (dbUpdateId != status.dbUpdateId) {
            if (dbUpdateId != null) onUpdateFinishCallbacks.remove(dbUpdateId)?.invoke()
            dbUpdateId = status.dbUpdateId
        }
    }

    override fun onMPDChanged(subsystems: List<String>) {
        if (subsystems.containsAny("player", "mixer", "options", "update")) loadStatus()
        if (subsystems.contains("database")) loadStats()
        if (subsystems.contains("stored_playlist")) loadPlaylists()
    }

    override fun onMPDClientError(client: BaseMPDClient, exception: Throwable, command: BaseMPDCommand<*>?) {
        logError("$exception, $client, $command", exception)
    }
}

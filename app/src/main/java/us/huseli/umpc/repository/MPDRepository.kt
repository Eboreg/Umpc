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
import us.huseli.umpc.data.MPDDirectory
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
import us.huseli.umpc.mpd.MPDFilter
import us.huseli.umpc.mpd.OnMPDChangeListener
import us.huseli.umpc.mpd.client.BaseMPDClient
import us.huseli.umpc.mpd.client.MPDBinaryClient
import us.huseli.umpc.mpd.client.MPDClient
import us.huseli.umpc.mpd.client.MPDClientListener
import us.huseli.umpc.mpd.client.MPDIdleClient
import us.huseli.umpc.mpd.mpdFilter
import us.huseli.umpc.mpd.mpdFind
import us.huseli.umpc.mpd.mpdList
import us.huseli.umpc.mpd.request.BaseMPDRequest
import us.huseli.umpc.mpd.request.MPDBatchRequest
import us.huseli.umpc.mpd.response.MPDBatchTextResponse
import us.huseli.umpc.mpd.response.MPDBinaryResponse
import us.huseli.umpc.mpd.response.MPDTextResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MPDRepository @Inject constructor(
    val client: MPDClient,
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
    private val pendingAlbumsWithSongsRequests = mutableMapOf<String, MPDBatchRequest>()
    private var statusJob: Job? = null

    val albums = _albums.asStateFlow()
    val connectedServer = idleClient.connectedServer
    val currentAudioFormat = _currentAudioFormat.asStateFlow()
    val currentBitrate = _currentBitrate.asStateFlow()
    val currentSong = _currentSong.asStateFlow()
    val currentSongDuration = _currentSongDuration.asStateFlow()
    val currentSongElapsed = _currentSongElapsed.asStateFlow()
    val currentSongPosition = _currentSongPosition.asStateFlow()
    val error = _error.asStateFlow()
    val isConnected = _isConnected.asStateFlow()
    val isIOError = _isIOError.asStateFlow()
    val outputs = _outputs.asStateFlow()
    val playerState = _playerState.asStateFlow()
    val playlists = _playlists.asStateFlow()
    val protocolVersion = idleClient.protocolVersion
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
            idleClient.connectedServer.collect {
                if (it == null) {
                    reset()
                } else {
                    loadStatus {}
                    loadCurrentSong()
                    loadQueue()
                    loadAlbums()
                    loadPlaylists {}
                }
            }
        }

        ioScope.launch {
            /**
             * If any client object reports IO error, set isIOError = true until
             * that changes.
             */
            combine(idleClient.state, client.state, binaryClient.state) { s1, s2, s3 ->
                listOf(s1, s2, s3).any { it == BaseMPDClient.State.IO_ERROR }
            }.distinctUntilChanged().collect {
                _isIOError.value = it
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
                                loadStatus {}
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

    fun addAlbumToPlaylist(album: MPDAlbum, playlistName: String, onFinish: (MPDBatchTextResponse) -> Unit) =
        addAlbumsToPlaylist(listOf(album), playlistName, onFinish)

    inline fun addAlbumsToPlaylist(
        albums: Collection<MPDAlbum>,
        playlistName: String,
        crossinline onFinish: (MPDBatchTextResponse) -> Unit,
    ) = client.enqueueBatch(
        commands = albums.map { album ->
            album.getMPDFilter().searchaddpl(protocolVersion.value, playlistName)
        },
        onFinish = onFinish,
    )

    fun addQueueToPlaylist(playlistName: String, onFinish: ((MPDTextResponse) -> Unit)? = null) =
        client.enqueue("save", playlistName) { response ->
            if (!response.isSuccess && serverHasCapability(MPDServerCapability.SAVE_APPEND_REPLACE))
                client.enqueue("save", listOf(playlistName, "append"), onFinish = onFinish)
            else onFinish?.invoke(response)
        }

    inline fun addSongToPlaylist(
        song: MPDSong,
        playlistName: String,
        crossinline onFinish: (MPDBatchTextResponse) -> Unit,
    ) = addSongsToPlaylist(listOf(song), playlistName, onFinish)

    inline fun addSongsToPlaylist(
        songs: Collection<MPDSong>,
        playlistName: String,
        crossinline onFinish: (MPDBatchTextResponse) -> Unit,
    ) = client.enqueueBatch(
        commands = songs.map { formatMPDCommand("playlistadd", playlistName, it.filename) },
        onFinish = onFinish,
    )

    inline fun addSongsToPlaylistPositioned(
        songs: Collection<MPDSong>,
        playlistName: String,
        crossinline onFinish: (MPDBatchTextResponse) -> Unit,
    ) = client.enqueueBatch(
        commands = songs.sortedBy { it.position }.map {
            if (it.position != null) formatMPDCommand("playlistadd", playlistName, it.filename, it.position)
            else formatMPDCommand("playlistadd", playlistName, it.filename)
        },
        onFinish = onFinish,
    )

    fun clearError() = client.enqueue("clearerror")

    fun clearQueue(onFinish: ((MPDTextResponse) -> Unit)? = null) =
        client.enqueue("clear", onFinish = onFinish)

    fun deletePlaylist(playlistName: String, onFinish: ((MPDTextResponse) -> Unit)? = null) =
        client.enqueue("rm", playlistName, onFinish = onFinish)

    inline fun enqueueAlbum(album: MPDAlbum, crossinline onFinish: (MPDBatchTextResponse) -> Unit) =
        enqueueAlbums(listOf(album), onFinish)

    inline fun enqueueAlbums(albums: Collection<MPDAlbum>, crossinline onFinish: (MPDBatchTextResponse) -> Unit) =
        client.enqueueBatch(
            albums.map { it.getMPDFilter().findadd(protocolVersion.value) },
            onFinish,
        )

    fun enqueuePlaylist(playlistName: String, onFinish: ((MPDTextResponse) -> Unit)? = null) =
        client.enqueue("load", playlistName, onFinish = onFinish)

    inline fun enqueueSong(song: MPDSong, crossinline onFinish: (MPDBatchTextResponse) -> Unit) =
        enqueueSongs(listOf(song.filename), onFinish)

    inline fun enqueueSongsPositioned(
        songs: Collection<MPDSong>,
        crossinline onFinish: (MPDBatchTextResponse) -> Unit,
    ) {
        client.enqueueBatch(
            songs.map {
                if (it.position != null) formatMPDCommand("addid", it.filename, it.position)
                else formatMPDCommand("addid", it.filename)
            },
            onFinish,
        )
    }

    inline fun enqueueSongs(filenames: Collection<String>, crossinline onFinish: (MPDBatchTextResponse) -> Unit) =
        client.enqueueBatch(filenames.map { formatMPDCommand("add", it) }, onFinish)

    fun getAlbumArt(key: AlbumArtKey, onFinish: (MPDBinaryResponse) -> Unit) {
        if (!fetchedAlbumArtKeys.contains(key)) {
            binaryClient.enqueue("albumart", key.filename) { response ->
                fetchedAlbumArtKeys.add(key)
                onFinish(response)
            }
        }
    }

    inline fun getAlbumWithSongs(album: MPDAlbum, crossinline onFinish: (MPDAlbumWithSongs) -> Unit) =
        getAlbumsWithSongs(listOf(album)) { onFinish(it.first()) }

    inline fun getAlbumsByAlbumArtist(artistName: String, crossinline onFinish: (List<MPDAlbum>) -> Unit) =
        client.enqueue(mpdList("album") { "albumartist" eq artistName }) { response ->
            onFinish(response.extractAlbums(artistName))
        }

    inline fun getAlbumsByArtist(artistName: String, crossinline onFinish: (List<MPDAlbum>) -> Unit) {
        client.enqueue(mpdList("album", "albumartist") { "artist" eq artistName }) { response ->
            if (response.mpdError?.type == MPDError.Type.ARG) {
                client.enqueue(mpdFind { "artist" eq artistName }) { response2 ->
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

    inline fun getDirectory(path: String, crossinline onFinish: (MPDDirectory) -> Unit) {
        client.enqueue("lsinfo", path) { response ->
            response.extractDirectory(path).also(onFinish)
        }
    }

    fun loadAlbums() {
        /**
         * Mopidy-MPD doesn't support the "group" parameter, but we have no way
         * to tell if it's a Mopidy server. So we just have to try and fail!
         */
        client.enqueue("list album group albumartist") { response ->
            if (response.mpdError?.type == MPDError.Type.ARG) {
                client.enqueue("list albumartist") { response2 ->
                    val albumArtists = response2.extractValues("albumartist")

                    client.enqueueBatch(
                        albumArtists.map { mpdList("album") { "albumartist" eq it } }
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
                    }
                }
            } else _albums.value = response.extractAlbums().sorted()
        }
    }

    fun loadOutputs() =
        client.enqueue("outputs") { response ->
            response.extractOutputs().let { outputs ->
                _outputs.value = outputs
            }
        }

    fun loadPlaylistsWithSongs(forceReload: Boolean = false) {
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
                        updatedPlaylists.sortedBy { it.name.lowercase() }.also { _playlists.value = it }
                    }
                }
            }
        }
    }

    fun loadPlaylistSongs(playlist: MPDPlaylist) {
        client.enqueue("listplaylistinfo", playlist.name) { response ->
            if (response.isSuccess) {
                val songs = response.extractPositionedSongs()

                _playlists.value = _playlists.value?.toMutableList()?.apply {
                    indexOfFirst { it.name == playlist.name }.takeIf { it > -1 }?.also { index ->
                        add(index, removeAt(index).copy(songs = songs))
                    }
                }
            }
        }
    }

    fun loadQueue() {
        client.enqueue("playlistinfo") { response ->
            response.extractSongs().also { _queue.value = it }
        }
    }

    fun loadStats() = client.enqueue("stats") { response ->
        if (response.isSuccess) _stats.value = response.extractMap().toMPDStats()
    }

    fun moveSongInQueue(fromIdx: Int, toIdx: Int) = client.enqueue("move $fromIdx $toIdx")

    fun moveSongInPlaylist(playlistName: String, fromIdx: Int, toIdx: Int) =
        client.enqueue("playlistmove", listOf(playlistName, fromIdx, toIdx))

    inline fun mpdFind(filter: MPDFilter.() -> MPDFilter) = mpdFind(protocolVersion.value, null, filter)

    inline fun mpdList(type: String, group: String? = null, filter: MPDFilter.() -> MPDFilter): String =
        mpdList(type, protocolVersion.value, group, filter)

    fun next() = ensurePlayerState {
        disableStopAfterCurrent()
        client.enqueue("next")
    }

    fun pause() = ensurePlayerState { client.enqueue("pause 1") }

    fun play() = ensurePlayerState {
        _currentSongId.value?.let {
            playSongById(it)
        } ?: kotlin.run {
            disableStopAfterCurrent()
            playSongByPosition(0)
        }
    }

    fun playAlbum(album: MPDAlbum) = playAlbums(listOf(album))

    fun playAlbums(albums: Collection<MPDAlbum>) {
        val commands = listOf("clear") +
                albums.reversed().map { it.getMPDFilter().findadd(protocolVersion.value, 0) } +
                listOf("play 0")

        disableStopAfterCurrent()
        client.enqueueBatch(commands) {}
    }

    fun playOrPause() {
        when (playerState.value) {
            PlayerState.PLAY -> pause()
            PlayerState.STOP -> play()
            PlayerState.PAUSE -> client.enqueue("pause 0")
            PlayerState.UNKNOWN -> loadStatus {
                if (playerState.value != PlayerState.UNKNOWN) playOrPause()
            }
        }
    }

    fun playPlaylist(playlistName: String, startIndex: Int = 0) {
        val commands = listOf("clear", formatMPDCommand("load", playlistName), "play $startIndex")

        disableStopAfterCurrent()
        client.enqueueBatch(commands) {}
    }

    fun playSongById(id: Int) {
        if (_currentSongId.value != id) disableStopAfterCurrent()
        client.enqueue("playid $id")
    }

    fun playSongByPosition(pos: Int) {
        if (currentSongPosition.value != pos) disableStopAfterCurrent()
        client.enqueue("play $pos")
    }

    fun playSongs(songs: Collection<MPDSong>, startIndex: Int = 0) {
        /** Replaces queue with `songs` and starts playing. */
        val commands = listOf("clear") +
                songs.map { formatMPDCommand("add", it.filename) } +
                listOf("play $startIndex")

        disableStopAfterCurrent()
        client.enqueueBatch(commands) {}
    }

    fun previousOrRestart() {
        if (currentSongElapsed.value?.takeIf { it > 2 } != null) seek(0.0)
        else if (currentSongPosition.value?.takeIf { it > 0 } != null) {
            disableStopAfterCurrent()
            client.enqueue("previous")
        }
    }

    fun registerOnMPDChangeListener(listener: OnMPDChangeListener) =
        idleClient.registerOnMPDChangeListener(listener)

    fun removeSongFromPlaylist(playlistName: String, song: MPDSong) =
        song.position?.let { removeSongsFromPlaylist(playlistName, listOf(it)) {} }

    inline fun removeSongsFromPlaylist(
        playlistName: String,
        positions: Collection<Int>,
        crossinline onFinish: (MPDBatchTextResponse) -> Unit,
    ) = client.enqueueBatch(
        positions.sortedDescending().map { formatMPDCommand("playlistdelete", playlistName, it) },
        onFinish,
    )

    fun removeSongFromQueue(song: MPDSong) = removeSongsFromQueue(listOf(song)) {}

    inline fun removeSongsFromQueue(songs: Collection<MPDSong>, crossinline onFinish: (MPDBatchTextResponse) -> Unit) =
        client.enqueueBatch(songs.mapNotNull { it.id }.map { "deleteid $it" }, onFinish)

    fun renamePlaylist(playlistName: String, newName: String, onFinish: ((MPDTextResponse) -> Unit)? = null) =
        client.enqueue("rename", listOf(playlistName, newName), onFinish)

    fun search(term: String, onFinish: (MPDBatchTextResponse) -> Unit) {
        /**
         * MPD cannot combine search terms with logical OR for some reason, so
         * we cannot select a list of tags to search, but must use "any". As
         * this may give a lot of search results we don't want, additional
         * filtering must be applied.
         *
         * TODO: Maybe disable search all together in <0.21, or implement
         * it in some completely different way
         * TODO: Do "or" search with batch request instead
         */
        searchOr(listOf(mpdFilter { "any" contains term }, mpdFilter { "file" contains term }), onFinish)
    }

    fun search(filter: MPDFilter, onFinish: (MPDTextResponse) -> Unit) =
        client.enqueue(command = filter.search(protocolVersion.value), onFinish = onFinish)

    fun searchAnd(filters: Iterable<MPDFilter>, onFinish: (MPDTextResponse) -> Unit) =
        search(filters.reduce { acc, mpdFilter -> acc and mpdFilter }, onFinish)

    inline fun searchOr(filters: Iterable<MPDFilter>, crossinline onFinish: (MPDBatchTextResponse) -> Unit) =
        client.enqueueBatch(filters.map { it.search(protocolVersion.value) }, onFinish)

    fun seek(time: Double) = client.enqueue("seekcur $time")

    fun seekRelative(time: Double) {
        val timeString = if (time >= 0) "+$time" else time.toString()
        client.enqueue("seekcur $timeString")
    }

    fun setOutputEnabled(id: Int, isEnabled: Boolean) =
        client.enqueue(if (isEnabled) "enableoutput $id" else "disableoutput $id")

    fun setVolume(@IntRange(0, 100) value: Int) = client.enqueue("setvol $value")

    fun stop() = ensurePlayerState {
        disableStopAfterCurrent()
        client.enqueue("stop")
    }

    fun toggleRandomState() = client.enqueue("random", if (randomState.value) "0" else "1")

    fun toggleRepeatState() = client.enqueue("repeat", if (repeatState.value) "0" else "1")

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

    private inline fun ensurePlayerState(crossinline callback: () -> Unit) {
        if (playerState.value == PlayerState.UNKNOWN) {
            loadStatus {
                if (playerState.value != PlayerState.UNKNOWN) callback()
            }
        } else callback()
    }

    private inline fun getAlbumsWithSongs(
        artistTag: String,
        albums: Collection<MPDAlbum>,
        crossinline onFinish: (List<MPDAlbumWithSongs>) -> Unit,
    ) {
        val commands = albums.map { it.getMPDFilter(artistTag).find(protocolVersion.value) }
        val pendingRequest =
            pendingAlbumsWithSongsRequests[artistTag]?.takeIf { it.status == BaseMPDRequest.Status.PENDING }
        val callback = { response: MPDBatchTextResponse ->
            val songLists = response.extractNestedSongs()
            val albumsWithSongs =
                songLists.flatMap { it.groupByAlbum() }.filter { albums.contains(it.album) }
            onFinish(albumsWithSongs)
        }

        if (pendingRequest != null) {
            pendingRequest.addCommands(commands)
            pendingRequest.addCallback(callback)
        } else {
            pendingAlbumsWithSongsRequests[artistTag] = client.enqueueBatch(commands, callback)
        }
    }

    private fun loadCurrentSong() = client.enqueue("currentsong") { response ->
        updateCurrentSong(response.extractSong())
    }

    private inline fun loadPlaylists(forceReload: Boolean = false, crossinline onFinish: (List<MPDPlaylist>) -> Unit) {
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
                    onFinish(it)
                }
            }
        }
    }

    private inline fun loadStatus(crossinline onFinish: (MPDStatus) -> Unit) =
        client.enqueue("status") { response ->
            response.extractStatus()?.also { status ->
                updateStatus(status)
                onFinish(status)
            }
        }

    @Suppress("SameParameterValue")
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
        if (subsystems.containsAny("player", "mixer", "options", "update")) loadStatus {}
        if (subsystems.containsAny("player", "playlist")) loadCurrentSong()
        if (subsystems.contains("database")) loadStats()
        if (subsystems.contains("stored_playlist")) loadPlaylists {}
    }

    override fun onMPDClientError(client: BaseMPDClient, exception: Throwable, request: BaseMPDRequest<*>?) {
        logError("$exception, $client, $request", exception)
    }
}

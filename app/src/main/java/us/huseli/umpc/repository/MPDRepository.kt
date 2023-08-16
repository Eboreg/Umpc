package us.huseli.umpc.repository

import android.content.Context
import androidx.annotation.IntRange
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import us.huseli.umpc.PlayerState
import us.huseli.umpc.data.AlbumArtKey
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.data.MPDAudioFormat
import us.huseli.umpc.data.MPDError
import us.huseli.umpc.data.MPDOutput
import us.huseli.umpc.data.MPDPlaylist
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.data.MPDStatus
import us.huseli.umpc.data.MPDVersion
import us.huseli.umpc.data.queueDataStore
import us.huseli.umpc.data.sorted
import us.huseli.umpc.data.toMPDStatus
import us.huseli.umpc.data.toNative
import us.huseli.umpc.data.toProto
import us.huseli.umpc.formatMPDCommand
import us.huseli.umpc.mpd.BaseMPDFilter
import us.huseli.umpc.mpd.OnMPDChangeListener
import us.huseli.umpc.mpd.client.MPDBinaryClient
import us.huseli.umpc.mpd.client.MPDClient
import us.huseli.umpc.mpd.client.MPDIdleClient
import us.huseli.umpc.mpd.mpdFilter
import us.huseli.umpc.mpd.mpdFilterPre021
import us.huseli.umpc.mpd.mpdFindAdd
import us.huseli.umpc.mpd.mpdFindAddPre021
import us.huseli.umpc.mpd.mpdFindAddRelative
import us.huseli.umpc.mpd.response.MPDBatchMapResponse
import us.huseli.umpc.mpd.response.MPDBinaryResponse
import us.huseli.umpc.mpd.response.MPDMapResponse
import us.huseli.umpc.mpd.response.MPDMultiMapResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MPDRepository @Inject constructor(
    private val client: MPDClient,
    private val binaryClient: MPDBinaryClient,
    private val idleClient: MPDIdleClient,
    private val ioScope: CoroutineScope,
    @ApplicationContext private val context: Context,
) : OnMPDChangeListener {
    private val _albums = MutableStateFlow<List<MPDAlbum>>(emptyList())
    private val _currentAudioFormat = MutableStateFlow<MPDAudioFormat?>(null)
    private val _currentBitrate = MutableStateFlow<Int?>(null)
    private val _currentSong = MutableStateFlow<MPDSong?>(null)
    private val _currentSongDuration = MutableStateFlow<Double?>(null)
    private val _currentSongElapsed = MutableStateFlow<Double?>(null)
    private val _currentSongId = MutableStateFlow<Int?>(null)
    private val _currentSongPosition = MutableStateFlow<Int?>(null)
    private val _error = MutableStateFlow<String?>(null)
    private val _fetchedAlbumArtKeys = mutableListOf<AlbumArtKey>()
    private val _outputs = MutableStateFlow<List<MPDOutput>>(emptyList())
    private val _playerState = MutableStateFlow(PlayerState.UNKNOWN)
    private val _playlists = MutableStateFlow<List<MPDPlaylist>>(emptyList())
    private val _randomState = MutableStateFlow(false)
    private val _repeatState = MutableStateFlow(false)
    private val _queue = MutableStateFlow<List<MPDSong>>(emptyList())
    private val _stopAfterCurrent = MutableStateFlow(false)
    private val _volume = MutableStateFlow(100)

    private var statusJob: Job? = null

    val albums = _albums.asStateFlow()
    val currentAudioFormat = _currentAudioFormat.asStateFlow()
    val currentBitrate = _currentBitrate.asStateFlow()
    val currentSong = _currentSong.asStateFlow()
    val currentSongDuration = _currentSongDuration.asStateFlow()
    val currentSongElapsed = _currentSongElapsed.asStateFlow()
    val currentSongPosition = _currentSongPosition.asStateFlow()
    val error = _error.asStateFlow()
    val outputs = _outputs.asStateFlow()
    val playerState = _playerState.asStateFlow()
    val protocolVersion = client.protocolVersion
    val playlists = _playlists.asStateFlow()
    val queue = _queue.asStateFlow()
    val randomState = _randomState.asStateFlow()
    val repeatState = _repeatState.asStateFlow()
    val server = client.server
    val stopAfterCurrent = _stopAfterCurrent.asStateFlow()
    val volume = _volume.asStateFlow()

    init {
        idleClient.registerOnMPDChangeListener(this)

        ioScope.launch {
            context.queueDataStore.data.collect {
                _queue.value = it.toNative()
            }
        }

        ioScope.launch {
            // While playing, query for status every 10 seconds.
            _playerState.collect { playerState ->
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
                }
            }
        }

        ioScope.launch {
            currentSong.filterNotNull().distinctUntilChanged().collect { song ->
                song.duration?.let { _currentSongDuration.value = it }
                song.id?.let { _currentSongId.value = it }
                song.audioFormat?.let { _currentAudioFormat.value = it }
            }
        }
    }

    fun addAlbumToPlaylist(album: MPDAlbum, playlistName: String, onFinish: ((MPDBatchMapResponse) -> Unit)? = null) =
        addAlbumsToPlaylist(listOf(album), playlistName, onFinish)

    fun addAlbumsToPlaylist(
        albums: Collection<MPDAlbum>,
        playlistName: String,
        onFinish: ((MPDBatchMapResponse) -> Unit)? = null,
    ) =
        client.enqueueBatch(
            commands = albums
                .map { it.getSearchFilter(protocolVersion.value) }
                .map { formatMPDCommand("searchaddpl", playlistName, it) },
            onFinish = onFinish,
        )

    fun addQueueToPlaylist(playlistName: String, onFinish: (MPDMapResponse) -> Unit) =
        client.enqueue("save", playlistName) { response ->
            if (!response.isSuccess && protocolVersion.value >= MPDVersion("0.24"))
                client.enqueue("save", listOf(playlistName, "append"), onFinish)
            else onFinish(response)
        }

    fun addSongToPlaylist(song: MPDSong, playlistName: String, onFinish: ((MPDBatchMapResponse) -> Unit)? = null) =
        addSongsToPlaylist(listOf(song), playlistName, onFinish)

    fun addSongsToPlaylist(
        songs: Collection<MPDSong>,
        playlistName: String,
        onFinish: ((MPDBatchMapResponse) -> Unit)? = null,
    ) = client.enqueueBatch(
        commands = songs.map { formatMPDCommand("playlistadd", playlistName, it.filename) },
        onFinish = onFinish,
    )

    fun addSongsToPlaylistPositioned(
        songs: Collection<MPDSong>,
        playlistName: String,
        onFinish: ((MPDBatchMapResponse) -> Unit)? = null,
    ) = client.enqueueBatch(
        commands = songs.sortedBy { it.position }.map {
            if (it.position != null) formatMPDCommand("playlistadd", playlistName, it.filename, it.position)
            else formatMPDCommand("playlistadd", playlistName, it.filename)
        },
        onFinish = onFinish,
    )

    fun clearError(onFinish: ((MPDMapResponse) -> Unit)? = null) = client.enqueue("clearerror", onFinish)

    fun clearQueue(onFinish: ((MPDMapResponse) -> Unit)? = null) = client.enqueue("clear", onFinish)

    fun deletePlaylist(playlistName: String, onFinish: ((MPDMapResponse) -> Unit)? = null) =
        client.enqueue("rm", playlistName, onFinish)

    fun enqueueAlbumNextAndPlay(album: MPDAlbum, onFinish: ((MPDMapResponse) -> Unit)? = null) {
        val relativePosition = if (currentSongPosition.value != null) 0 else null
        val position =
            if (protocolVersion.value >= MPDVersion("0.23.5")) currentSongPosition.value ?: _queue.value.size
            else _queue.value.size
        val command =
            if (protocolVersion.value >= MPDVersion("0.23.5") && relativePosition != null)
                mpdFindAddRelative(relativePosition) {
                    equals("album", album.name) and equals("albumartist", album.artist)
                }
            else if (protocolVersion.value >= MPDVersion("0.23"))
                mpdFindAdd(position) { equals("album", album.name) and equals("albumartist", album.artist) }
            else if (protocolVersion.value >= MPDVersion("0.21"))
                mpdFindAdd { equals("album", album.name) and equals("albumartist", album.artist) }
            else mpdFindAddPre021 { equals("album", album.name) and equals("albumartist", album.artist) }

        client.enqueue(command) { response ->
            if (response.isSuccess) playSongByPosition(position, onFinish)
        }
    }

    fun enqueueAlbumsLast(albums: Collection<MPDAlbum>, onFinish: ((MPDBatchMapResponse) -> Unit)? = null) =
        client.enqueueBatch(albums.map { it.getSearchFilter(protocolVersion.value).findadd() }, onFinish)

    fun enqueueAlbumLast(album: MPDAlbum, onFinish: ((MPDBatchMapResponse) -> Unit)? = null) =
        enqueueAlbumsLast(listOf(album), onFinish)

    fun enqueuePlaylistNext(playlistName: String, onFinish: ((MPDMapResponse) -> Unit)? = null) {
        if (protocolVersion.value >= MPDVersion("0.23.1")) {
            val position = currentSongPosition.value ?: 0
            client.enqueue(formatMPDCommand("load", playlistName, "0:", position), onFinish)
        } else {
            client.enqueue("load", playlistName, onFinish)
        }
    }

    fun enqueueSongLast(song: MPDSong, onFinish: ((MPDBatchMapResponse) -> Unit)? = null) =
        enqueueSongsLast(listOf(song.filename), onFinish)

    fun enqueueSongNext(song: MPDSong, onFinish: ((MPDMapResponse) -> Unit)? = null) {
        val args =
            if (currentSongPosition.value != null) listOf(song.filename, currentSongPosition.value)
            else listOf(song.filename)

        client.enqueue("addid", args, onFinish)
    }

    fun enqueueSongs(songs: Collection<MPDSong>, onFinish: ((MPDBatchMapResponse) -> Unit)? = null) =
        client.enqueueBatch(
            songs.map {
                formatMPDCommand(
                    "addid",
                    if (it.position != null) listOf(it.filename, it.position) else listOf(it.filename)
                )
            },
            onFinish,
        )

    fun enqueueSongsLast(songs: Collection<MPDSong>, onFinish: ((MPDBatchMapResponse) -> Unit)? = null) =
        enqueueSongsLast(songs.map { it.filename }, onFinish)

    fun enqueueSongsLast(filenames: Collection<String>, onFinish: ((MPDBatchMapResponse) -> Unit)? = null) =
        client.enqueueBatch(filenames.map { formatMPDCommand("add", it) }, onFinish)

    fun getAlbumArt(key: AlbumArtKey, onFinish: ((MPDBinaryResponse) -> Unit)? = null) {
        if (!_fetchedAlbumArtKeys.contains(key)) {
            binaryClient.enqueueBinary("albumart", key.filename, onFinish)
        }
    }

    fun getAlbumWithSongs(album: MPDAlbum, onFinish: (MPDAlbumWithSongs) -> Unit) =
        getAlbumsWithSongs(listOf(album)) { onFinish(it.first()) }

    fun getAlbumsWithSongs(albums: Collection<MPDAlbum>, onFinish: (List<MPDAlbumWithSongs>) -> Unit) =
        client.enqueueBatchMultiMap(albums.map { it.getSearchFilter(protocolVersion.value).find() }) { response ->
            onFinish(
                albums.mapIndexed { index, album ->
                    MPDAlbumWithSongs(album, response.subResponses[index].extractSongs())
                }
            )
        }

    fun getAlbumsByAlbumArtist(artistName: String, onFinish: (List<MPDAlbum>) -> Unit) =
        client.enqueueMultiMap("list album", equalsFilter("albumartist", artistName)) { response ->
            onFinish(response.extractAlbums(artistName))
        }

    fun getAlbumsByArtist(artistName: String, onFinish: (List<MPDAlbum>) -> Unit) {
        client.enqueueMultiMap(
            "list album",
            listOf(equalsFilter("artist", artistName), "group albumartist")
        ) { response ->
            if (response.mpdError?.type == MPDError.Type.ARG) {
                client.enqueueMultiMap("find", equalsFilter("artist", artistName)) { response2 ->
                    onFinish(response2.extractSongs().map { it.album }.toSet().toList())
                }
            } else onFinish(response.extractAlbums())
        }
    }

    fun getPlaylistSongs(playlistName: String, onFinish: (List<MPDSong>) -> Unit) =
        client.enqueueMultiMap("listplaylistinfo", playlistName) { response ->
            onFinish(response.extractSongsWithPosition())
        }

    fun loadAlbums(onFinish: ((List<MPDAlbum>) -> Unit)? = null) {
        /**
         * Mopidy-MPD doesn't support the "group" parameter, but we have no way
         * to tell if it's a Mopidy server. So we just have to try and fail!
         */
        client.enqueueMultiMap("list album group albumartist") { response ->
            if (response.mpdError?.type == MPDError.Type.ARG) {
                client.enqueueList("list albumartist", "AlbumArtist") { response2 ->
                    val albumArtists = response2.values
                    client.enqueueBatch(
                        albumArtists.map { "list album ${equalsFilter("albumartist", it)}" }
                    ) { response3 ->
                        _albums.value = albumArtists
                            .flatMapIndexed { index, artist ->
                                response3.responseMaps[index].getValue("Album").map { MPDAlbum(artist, it) }
                            }
                            .sorted()
                            .also { onFinish?.invoke(it) }
                    }
                }
            }
            _albums.value = response.extractAlbums().sorted().also { onFinish?.invoke(it) }
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun loadCurrentSong(onFinish: ((MPDSong) -> Unit)? = null) =
        client.enqueue("currentsong") { response ->
            _currentSong.value = response.extractSong()?.also { song ->
                song.position?.let { _currentSongPosition.value = it }
                onFinish?.invoke(song)
            }
        }

    fun loadOutputs(onFinish: ((List<MPDOutput>) -> Unit)? = null) =
        client.enqueueMultiMap("outputs") { response ->
            response.extractOutputs().let { outputs ->
                _outputs.value = outputs
                onFinish?.invoke(outputs)
            }
        }

    fun loadPlaylists(onFinish: ((List<MPDPlaylist>) -> Unit)? = null) =
        client.enqueueMultiMap("listplaylists") { response ->
            response.extractPlaylists().sortedBy { it.name.lowercase() }.let { playlists ->
                _playlists.value = playlists
                onFinish?.invoke(playlists)
            }
        }

    fun loadQueue(onFinish: ((List<MPDSong>) -> Unit)? = null) =
        client.enqueueMultiMap("playlistinfo") { response ->
            response.extractSongs().also { songs ->
                ioScope.launch {
                    context.queueDataStore.updateData { currentQueue ->
                        currentQueue.toBuilder()
                            .clearSongs()
                            .addAllSongs(songs.toProto())
                            .build()
                    }
                }
                onFinish?.invoke(songs)
            }
        }

    fun loadStatus(onFinish: ((MPDStatus) -> Unit)? = null) =
        client.enqueue("status") { response ->
            response.responseMap.mapValues { it.value.first() }.toMPDStatus()?.also { status ->
                status.volume?.let { _volume.value = it }
                status.repeat?.let { _repeatState.value = it }
                status.random?.let { _randomState.value = it }
                status.playerState?.let { _playerState.value = it }

                _error.value = status.error
                _currentSongElapsed.value = status.currentSongElapsed
                _currentSongDuration.value = status.currentSongDuration
                _currentSongId.value = status.currentSongId
                _currentBitrate.value = status.bitrate
                _currentAudioFormat.value = status.audioFormat
                setCurrentSongPosition(status.currentSongPosition)

                onFinish?.invoke(status)
            }
        }

    fun moveSongInQueue(fromIdx: Int, toIdx: Int, onFinish: ((MPDMapResponse) -> Unit)? = null) =
        client.enqueue("move $fromIdx $toIdx", onFinish)

    fun moveSongInPlaylist(
        playlistName: String,
        fromIdx: Int,
        toIdx: Int,
        onFinish: ((MPDMapResponse) -> Unit)? = null,
    ) =
        client.enqueue(formatMPDCommand("playlistmove", playlistName, fromIdx, toIdx), onFinish)

    fun next(onFinish: ((MPDMapResponse) -> Unit)? = null) = ensurePlayerState {
        disableStopAfterCurrent()
        client.enqueue("next", onFinish)
    }

    fun pause(onFinish: ((MPDMapResponse) -> Unit)? = null) =
        ensurePlayerState { client.enqueue("pause 1", onFinish) }

    fun play(onFinish: ((MPDMapResponse) -> Unit)? = null) = ensurePlayerState {
        currentSong.value?.let {
            playSong(it, onFinish)
        } ?: kotlin.run {
            disableStopAfterCurrent()
            playSongByPosition(0, onFinish)
        }
    }

    fun playOrPause(onFinish: ((MPDMapResponse) -> Unit)? = null) {
        when (_playerState.value) {
            PlayerState.PLAY -> pause(onFinish)
            PlayerState.STOP -> play(onFinish)
            PlayerState.PAUSE -> client.enqueue("pause 0", onFinish)
            PlayerState.UNKNOWN -> loadStatus {
                if (_playerState.value != PlayerState.UNKNOWN) playOrPause(onFinish)
            }
        }
    }

    fun playSong(song: MPDSong, onFinish: ((MPDMapResponse) -> Unit)? = null) {
        /**
         * Only plays song if it's already in the queue.
         */
        (song.id ?: _queue.value.firstOrNull { it.filename == song.filename }?.id)?.let { songId ->
            if (_currentSongId.value != songId) disableStopAfterCurrent()
            client.enqueue("playid $songId", onFinish)
        }
    }

    fun playSongByPosition(pos: Int, onFinish: ((MPDMapResponse) -> Unit)? = null) {
        if (_currentSongPosition.value != pos) disableStopAfterCurrent()
        client.enqueue("play $pos", onFinish)
    }

    fun previousOrRestart(onFinish: ((MPDMapResponse) -> Unit)? = null) =
        if (_currentSongElapsed.value?.takeIf { it > 2 } != null) seek(0.0)
        else {
            disableStopAfterCurrent()
            client.enqueue("previous", onFinish)
        }

    fun registerOnMPDChangeListener(listener: OnMPDChangeListener) =
        idleClient.registerOnMPDChangeListener(listener)

    fun removeSongFromPlaylist(playlistName: String, song: MPDSong, onFinish: ((MPDBatchMapResponse) -> Unit)? = null) =
        song.position?.let { removeSongsFromPlaylist(playlistName, listOf(it), onFinish) }

    fun removeSongsFromPlaylist(
        playlistName: String,
        positions: Collection<Int>,
        onFinish: ((MPDBatchMapResponse) -> Unit)? = null,
    ) =
        client.enqueueBatch(
            positions.sortedDescending().map { formatMPDCommand("playlistdelete", playlistName, it) },
            onFinish,
        )

    fun removeSongFromQueue(song: MPDSong, onFinish: ((MPDBatchMapResponse) -> Unit)? = null) =
        removeSongsFromQueue(listOf(song), onFinish)

    fun removeSongsFromQueue(songs: Collection<MPDSong>, onFinish: ((MPDBatchMapResponse) -> Unit)? = null) =
        client.enqueueBatch(songs.mapNotNull { it.id }.map { "deleteid $it" }, onFinish)

    fun renamePlaylist(playlistName: String, newName: String, onFinish: ((MPDMapResponse) -> Unit)? = null) =
        client.enqueue("rename", listOf(playlistName, newName), onFinish)

    fun search(term: String, onFinish: ((MPDMultiMapResponse) -> Unit)? = null) {
        /**
         * MPD cannot combine search terms with logical OR for some reason, so
         * we cannot select a list of tags to search, but must use "any". As
         * this may give a lot of search results we don't want, additional
         * filtering must be applied.
         *
         * TODO: Maybe disable search all together in <0.21, or implement
         * it in some completely different way
         */
        val filter =
            if (protocolVersion.value < MPDVersion("0.21")) mpdFilterPre021 { equals("any", term) }
            else mpdFilter { contains("any", term) }

        search(filter, onFinish)
    }

    fun search(filter: BaseMPDFilter, onFinish: ((MPDMultiMapResponse) -> Unit)? = null) =
        client.enqueueMultiMap(filter.search(), onFinish)

    fun seek(time: Double, onFinish: ((MPDMapResponse) -> Unit)? = null) =
        client.enqueue("seekcur $time", onFinish)

    fun seekRelative(time: Double, onFinish: ((MPDMapResponse) -> Unit)? = null) {
        val timeString = if (time >= 0) "+$time" else time.toString()
        client.enqueue("seekcur $timeString", onFinish)
    }

    fun setOutputEnabled(id: Int, isEnabled: Boolean, onFinish: ((MPDMapResponse) -> Unit)? = null) =
        client.enqueue(if (isEnabled) "enableoutput $id" else "disableoutput $id", onFinish)

    fun setVolume(@IntRange(0, 100) value: Int, onFinish: ((MPDMapResponse) -> Unit)? = null) =
        client.enqueue("setvol $value", onFinish)

    fun stop(onFinish: ((MPDMapResponse) -> Unit)? = null) = ensurePlayerState {
        disableStopAfterCurrent()
        client.enqueue("stop", onFinish)
    }

    fun toggleRandomState(onFinish: ((MPDMapResponse) -> Unit)? = null) =
        client.enqueue("random", if (_randomState.value) "0" else "1", onFinish)

    fun toggleRepeatState(onFinish: ((MPDMapResponse) -> Unit)? = null) =
        client.enqueue("repeat", if (_repeatState.value) "0" else "1", onFinish)

    fun toggleStopAfterCurrent() {
        _stopAfterCurrent.value = !_stopAfterCurrent.value
    }

    private fun disableStopAfterCurrent() {
        _stopAfterCurrent.value = false
    }

    private fun ensurePlayerState(callback: () -> Unit) {
        if (_playerState.value == PlayerState.UNKNOWN) {
            loadStatus {
                if (_playerState.value != PlayerState.UNKNOWN) callback()
            }
        } else callback()
    }

    private fun equalsFilter(tag: String, value: String) =
        if (protocolVersion.value < MPDVersion("0.21")) mpdFilterPre021 { equals(tag, value) }
        else mpdFilter { equals(tag, value) }

    private fun setCurrentSongPosition(value: Int?) {
        if (value != null) {
            _currentSongPosition.value = value
            loadCurrentSong()
        } else _currentSong.value = null
    }

    override fun onMPDChanged(subsystems: List<String>) {
        if (subsystems.contains("player") || subsystems.contains("mixer") || subsystems.contains("options")) {
            loadStatus()
        }
    }
}

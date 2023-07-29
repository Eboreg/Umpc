package us.huseli.umpc.repository

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.scale
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import us.huseli.umpc.Constants
import us.huseli.umpc.Constants.PREF_ACTIVE_DYNAMIC_PLAYLIST
import us.huseli.umpc.Constants.PREF_DYNAMIC_PLAYLISTS
import us.huseli.umpc.DynamicPlaylistState
import us.huseli.umpc.ImageRequestType
import us.huseli.umpc.InstantAdapter
import us.huseli.umpc.PlayerState
import us.huseli.umpc.data.AlbumArtKey
import us.huseli.umpc.data.DynamicPlaylist
import us.huseli.umpc.data.DynamicPlaylistFilter
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDAlbumArt
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.data.MPDAudioFormat
import us.huseli.umpc.data.MPDOutput
import us.huseli.umpc.data.MPDPlaylist
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.data.queueDataStore
import us.huseli.umpc.data.sorted
import us.huseli.umpc.data.toMPDSong
import us.huseli.umpc.data.toMPDStatus
import us.huseli.umpc.data.toNative
import us.huseli.umpc.data.toProto
import us.huseli.umpc.mpd.OnMPDChangeListener
import us.huseli.umpc.mpd.client.MPDBinaryClient
import us.huseli.umpc.mpd.client.MPDClient
import us.huseli.umpc.mpd.client.MPDIdleClient
import us.huseli.umpc.mpd.response.MPDMapResponse
import us.huseli.umpc.toBitmap
import java.io.ByteArrayInputStream
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

@Singleton
class MPDRepository @Inject constructor(
    val client: MPDClient,
    val messageRepository: MessageRepository,
    private val binaryClient: MPDBinaryClient,
    private val idleClient: MPDIdleClient,
    private val ioScope: CoroutineScope,
    @ApplicationContext private val context: Context,
) : OnMPDChangeListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private val _activeDynamicPlaylist = MutableStateFlow<DynamicPlaylist?>(null)
    private val _albumsWithSongs = MutableStateFlow<List<MPDAlbumWithSongs>>(listOf())
    private val _albums = MutableStateFlow<List<MPDAlbum>>(emptyList())
    private val _currentAudioFormat = MutableStateFlow<MPDAudioFormat?>(null)
    private val _currentBitrate = MutableStateFlow<Int?>(null)
    private val _currentSong = MutableStateFlow<MPDSong?>(null)
    private val _currentSongAlbumArt = MutableStateFlow<MPDAlbumArt?>(null)
    private val _currentSongDuration = MutableStateFlow<Double?>(null)
    private val _currentSongElapsed = MutableStateFlow<Double?>(null)
    private val _currentSongId = MutableStateFlow<Int?>(null)
    private val _currentSongPosition = MutableStateFlow<Int?>(null)
    private val _dynamicPlaylists = MutableStateFlow<List<DynamicPlaylist>>(emptyList())
    private val _fetchedAlbumArtKeys = mutableListOf<AlbumArtKey>()
    private val _loadingDynamicPlaylist = MutableStateFlow(false)
    private val _outputs = MutableStateFlow<List<MPDOutput>>(emptyList())
    private val _playerState = MutableStateFlow(PlayerState.UNKNOWN)
    private val _queue = MutableStateFlow<List<MPDSong>>(emptyList())
    private val _randomState = MutableStateFlow(false)
    private val _repeatState = MutableStateFlow(false)
    private val _stopAfterCurrent = MutableStateFlow(false)
    private val _storedPlaylists = MutableStateFlow<List<MPDPlaylist>>(emptyList())
    private val _volume = MutableStateFlow(100)

    private var dynamicPlaylistState: DynamicPlaylistState? = null
    private val dynamicPlaylistType = object : TypeToken<DynamicPlaylist>() {}
    private val gson: Gson = GsonBuilder().registerTypeAdapter(Instant::class.java, InstantAdapter()).create()
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private var statusJob: Job? = null

    val albumArtDirectory = File(context.cacheDir, "albumArt").apply { mkdirs() }
    val thumbnailDirectory = File(albumArtDirectory, "thumbnails").apply { mkdirs() }

    val activeDynamicPlaylist = _activeDynamicPlaylist.asStateFlow()
    val albums = _albums.asStateFlow()
    val albumsWithSongs = _albumsWithSongs.asStateFlow()
    val currentAudioFormat = _currentAudioFormat.asStateFlow()
    val currentBitrate = _currentBitrate.asStateFlow()
    val currentSong = _currentSong.asStateFlow()
    val currentSongAlbumArt = _currentSongAlbumArt.asStateFlow()
    val currentSongDuration = _currentSongDuration.asStateFlow()
    val currentSongElapsed = _currentSongElapsed.asStateFlow()
    val currentSongId = _currentSongId.asStateFlow()
    val currentSongPosition = _currentSongPosition.asStateFlow()
    val dynamicPlaylists = _dynamicPlaylists.asStateFlow()
    val loadingDynamicPlaylist = _loadingDynamicPlaylist.asStateFlow()
    val outputs = _outputs.asStateFlow()
    val playerState = _playerState.asStateFlow()
    val protocolVersion = client.protocolVersion
    val queue = _queue.asStateFlow()
    val randomState = _randomState.asStateFlow()
    val repeatState = _repeatState.asStateFlow()
    val stopAfterCurrent = _stopAfterCurrent.asStateFlow()
    val storedPlaylists = _storedPlaylists.asStateFlow()
    val volume = _volume.asStateFlow()

    init {
        idleClient.registerOnMPDChangeListener(this)
        preferences.registerOnSharedPreferenceChangeListener(this)

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
            _currentSongPosition.collect {
                if (_stopAfterCurrent.value) {
                    stop()
                    disableStopAfterCurrent()
                }
                if (it != null) loadCurrentSong()
                else _currentSong.value = null
            }
        }

        ioScope.launch {
            _currentSong.map { it?.albumArtKey }.filterNotNull().distinctUntilChanged().collect { key ->
                _currentSongAlbumArt.value = null
                getAlbumArt(key, ImageRequestType.BOTH) {
                    _currentSongAlbumArt.value = it
                }
            }
        }
    }

    fun addAlbumsWithSongs(aws: List<MPDAlbumWithSongs>) {
        _albumsWithSongs.value = _albumsWithSongs.value.plus(aws)
    }

    fun addDynamicPlaylist(playlist: DynamicPlaylist) {
        _dynamicPlaylists.value = _dynamicPlaylists.value.toMutableList().apply { add(playlist) }
    }

    fun clearQueue(onFinish: ((MPDMapResponse) -> Unit)? = null) = client.enqueue("clear", onFinish)

    private fun createThumbnail(fullImage: Bitmap, imageFilename: String): Bitmap {
        val factor = Constants.THUMBNAIL_SIZE.toFloat() / max(fullImage.width, fullImage.height)
        val width = (fullImage.width * factor).roundToInt()
        val height = (fullImage.height * factor).roundToInt()
        val file = File(thumbnailDirectory, imageFilename)
        val thumbnail = fullImage.scale(width, height)

        file.outputStream().use { outputStream ->
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        }
        return thumbnail
    }

    fun deactivateDynamicPlaylist() {
        preferences
            .edit()
            .remove(PREF_ACTIVE_DYNAMIC_PLAYLIST)
            .apply()
    }

    fun deleteDynamicPlaylist(playlist: DynamicPlaylist) {
        _dynamicPlaylists.value = _dynamicPlaylists.value.toMutableList().apply { remove(playlist) }
    }

    fun disableStopAfterCurrent() {
        _stopAfterCurrent.value = false
    }

    fun enqueueSongLast(filename: String, onFinish: (MPDMapResponse) -> Unit) =
        client.enqueue("add", filename, onFinish)

    private fun ensurePlayerState(callback: () -> Unit) {
        if (_playerState.value == PlayerState.UNKNOWN) {
            loadStatus {
                if (_playerState.value != PlayerState.UNKNOWN) callback()
            }
        } else callback()
    }

    fun getAlbumArt(
        key: AlbumArtKey,
        requestType: ImageRequestType,
        callback: (MPDAlbumArt) -> Unit
    ) {
        val fullImage: Bitmap? = File(albumArtDirectory, key.imageFilename).toBitmap()
        var thumbnail: Bitmap? = null

        if (requestType == ImageRequestType.THUMBNAIL || requestType == ImageRequestType.BOTH)
            thumbnail = File(thumbnailDirectory, key.imageFilename).toBitmap()

        if (fullImage != null && requestType == ImageRequestType.FULL)
            callback(MPDAlbumArt(key, fullImage = fullImage.asImageBitmap()))
        else if (thumbnail != null && fullImage != null)
            callback(MPDAlbumArt(key, fullImage = fullImage.asImageBitmap(), thumbnail = thumbnail.asImageBitmap()))
        else if (!_fetchedAlbumArtKeys.contains(key)) {
            binaryClient.enqueueBinary("albumart", key.filename) { response ->
                _fetchedAlbumArtKeys.add(key)
                if (response.isSuccess) {
                    ioScope.launch {
                        saveAlbumArt(key = key, stream = response.stream, callback = callback)
                    }
                }
            }
        }
    }

    fun loadActiveDynamicPlaylist(playOnLoad: Boolean, replaceCurrentQueue: Boolean) {
        val playlist =
            gson.fromJson(preferences.getString(PREF_ACTIVE_DYNAMIC_PLAYLIST, null), dynamicPlaylistType)
        _activeDynamicPlaylist.value = playlist

        ioScope.launch {
            dynamicPlaylistState?.close()
            dynamicPlaylistState =
                if (playlist != null) {
                    _loadingDynamicPlaylist.value = true
                    DynamicPlaylistState(
                        context = context,
                        playlist = playlist,
                        repo = this@MPDRepository,
                        ioScope = ioScope,
                        replaceCurrentQueue = replaceCurrentQueue,
                        playOnLoad = playOnLoad,
                        onLoaded = { _loadingDynamicPlaylist.value = false },
                    )
                } else null
        }
    }

    /** Caches every album artist and their albums, no songs. */
    fun loadAlbums() = client.enqueueMultiMap("list albumsort group albumartistsort") { response ->
        _albums.value = response.extractAlbums().sorted()
    }

    private fun loadCurrentSong() = client.enqueue("currentsong") { response ->
        _currentSong.value = response.responseMap.mapValues { it.value.first() }.toMPDSong()?.also { song ->
            song.duration?.let { _currentSongDuration.value = it }
        }
    }

    fun loadDynamicPlaylists() {
        val listType = object : TypeToken<List<DynamicPlaylist>>() {}

        gson.fromJson(preferences.getString(PREF_DYNAMIC_PLAYLISTS, "[]"), listType)?.let {
            _dynamicPlaylists.value = it
        }
    }

    fun loadOutputs(onFinish: ((List<MPDOutput>) -> Unit)? = null) =
        client.enqueueMultiMap("outputs") { response ->
            response.extractOutputs().also {
                _outputs.value = it
                onFinish?.invoke(it)
            }
        }

    fun loadQueue() = client.enqueueMultiMap("playlistinfo") { response ->
        response.extractSongs().also { songs ->
            ioScope.launch {
                context.queueDataStore.updateData { currentQueue ->
                    currentQueue.toBuilder()
                        .clearSongs()
                        .addAllSongs(songs.toProto())
                        .build()
                }
            }
        }
    }

    fun loadStatus(onFinish: ((MPDMapResponse) -> Unit)? = null) =
        client.enqueue("status") { response ->
            response.responseMap.mapValues { it.value.first() }.toMPDStatus()?.also { status ->
                status.volume?.let { _volume.value = it }
                status.repeat?.let { _repeatState.value = it }
                status.random?.let { _randomState.value = it }
                // status.single?.let { _singleState.value = it }
                // status.consume?.let { _consumeState.value = it }
                status.playerState?.let { _playerState.value = it }

                messageRepository.addError(status.error)
                _currentSongElapsed.value = status.currentSongElapsed
                _currentSongDuration.value = status.currentSongDuration
                _currentSongId.value = status.currentSongId
                _currentBitrate.value = status.bitrate
                _currentSongPosition.value = status.currentSongPosition
                _currentAudioFormat.value = status.audioFormat
            }
            onFinish?.invoke(response)
        }

    fun loadStoredPlaylists() = client.enqueueMultiMap("listplaylists") { response ->
        _storedPlaylists.value = response.extractPlaylists().sortedBy { it.name.lowercase() }
    }

    fun next() = ensurePlayerState {
        disableStopAfterCurrent()
        client.enqueue("next")
    }

    fun pause() = ensurePlayerState { client.enqueue("pause 1") }

    fun play() = ensurePlayerState {
        if (_currentSongPosition.value == null) disableStopAfterCurrent()
        playSongByPosition(_currentSongPosition.value ?: 0)
    }

    fun playOrPause() {
        when (_playerState.value) {
            PlayerState.PLAY -> pause()
            PlayerState.STOP -> play()
            PlayerState.PAUSE -> client.enqueue("pause 0")
            PlayerState.UNKNOWN -> loadStatus {
                if (_playerState.value != PlayerState.UNKNOWN) playOrPause()
            }
        }
    }

    fun playSongByPosition(pos: Int) {
        if (_currentSongPosition.value != pos) disableStopAfterCurrent()
        client.enqueue("play $pos")
    }

    fun previousOrRestart() =
        if (_currentSongElapsed.value?.takeIf { it > 2 } != null) seek(0.0)
        else {
            disableStopAfterCurrent()
            client.enqueue("previous")
        }

    fun registerOnMPDChangeListener(listener: OnMPDChangeListener) =
        idleClient.registerOnMPDChangeListener(listener)

    private fun saveAlbumArt(
        key: AlbumArtKey,
        stream: ByteArrayInputStream,
        callback: (MPDAlbumArt) -> Unit,
    ) {
        BitmapFactory.decodeStream(stream)?.also { fullImage ->
            val fullImageFile = File(albumArtDirectory, key.imageFilename)
            val factor = Constants.ALBUM_ART_MAXSIZE.toFloat() / max(fullImage.width, fullImage.height)
            val thumbnail = createThumbnail(fullImage, key.imageFilename)

            // Scale down full image if it happens to be very large:
            if (factor < 0) {
                val width = (fullImage.width * factor).roundToInt()
                val height = (fullImage.height * factor).roundToInt()
                val scaledFullImage = fullImage.scale(width, height)

                callback(MPDAlbumArt(key, scaledFullImage.asImageBitmap(), thumbnail.asImageBitmap()))
                fullImageFile.outputStream().use { outputStream ->
                    scaledFullImage.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                }
            } else {
                callback(MPDAlbumArt(key, fullImage.asImageBitmap(), thumbnail.asImageBitmap()))
                fullImageFile.outputStream().use { outputStream -> outputStream.write(stream.readBytes()) }
            }
        }
    }

    fun saveDynamicPlaylists() {
        val json = gson.toJson(_dynamicPlaylists.value)

        preferences
            .edit()
            .putString(PREF_DYNAMIC_PLAYLISTS, json)
            .apply()
    }

    fun seek(time: Double) = client.enqueue("seekcur $time")

    fun stop() = ensurePlayerState {
        disableStopAfterCurrent()
        client.enqueue("stop")
    }

    fun toggleStopAfterCurrent() {
        _stopAfterCurrent.value = !_stopAfterCurrent.value
    }

    fun updateDynamicPlaylist(
        playlist: DynamicPlaylist,
        filter: DynamicPlaylistFilter? = null,
        shuffle: Boolean? = null,
        songCount: Int? = null,
    ) {
        deleteDynamicPlaylist(playlist)
        addDynamicPlaylist(
            playlist.copy(
                filter = filter ?: playlist.filter,
                shuffle = shuffle ?: playlist.shuffle,
                songCount = songCount ?: playlist.songCount,
            )
        )
        saveDynamicPlaylists()
    }

    override fun onMPDChanged(subsystems: List<String>) {
        if (subsystems.contains("player") || subsystems.contains("mixer") || subsystems.contains("options")) {
            loadStatus()
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PREF_DYNAMIC_PLAYLISTS -> loadDynamicPlaylists()
            PREF_ACTIVE_DYNAMIC_PLAYLIST -> loadActiveDynamicPlaylist(playOnLoad = true, replaceCurrentQueue = true)
        }
    }
}

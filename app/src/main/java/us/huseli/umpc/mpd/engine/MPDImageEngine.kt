package us.huseli.umpc.mpd.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.scale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.umpc.Constants
import us.huseli.umpc.ImageRequestType
import us.huseli.umpc.data.AlbumArtKey
import us.huseli.umpc.data.MPDAlbumArt
import us.huseli.umpc.data.filterByAlbum
import us.huseli.umpc.mpd.MPDRepository
import us.huseli.umpc.toBitmap
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

class MPDImageEngine(
    context: Context,
    private val repo: MPDRepository,
    private val ioScope: CoroutineScope,
) {
    private val albumArtDir = File(context.cacheDir, "albumArt").apply { mkdirs() }
    private val thumbnailDir = File(albumArtDir, "thumbnails").apply { mkdirs() }
    private val _fetchedAlbumArtKeys = mutableListOf<AlbumArtKey>()
    private val _currentSongAlbumArt = MutableStateFlow<MPDAlbumArt?>(null)

    private val _currentAlbumArtKey = repo.currentSong.map { song ->
        song?.let { AlbumArtKey(it.albumArtist, it.album, it.filename) }
    }

    init {
        ioScope.launch {
            _currentAlbumArtKey.filterNotNull().distinctUntilChanged().collect { key ->
                _currentSongAlbumArt.value = null
                getAlbumArt(key, ImageRequestType.FULL) {
                    _currentSongAlbumArt.value = it
                }
            }
        }
    }

    suspend fun getAlbumArt(
        key: AlbumArtKey,
        requestType: ImageRequestType,
        callback: (MPDAlbumArt) -> Unit
    ) {
        val fullImage: Bitmap? = File(albumArtDir, key.imageFilename).toBitmap()
        var thumbnail: Bitmap? = null

        if (requestType == ImageRequestType.THUMBNAIL || requestType == ImageRequestType.BOTH)
            thumbnail = File(thumbnailDir, key.imageFilename).toBitmap()

        if (fullImage != null && requestType == ImageRequestType.FULL)
            callback(MPDAlbumArt(key, fullImage = fullImage.asImageBitmap()))
        else if (thumbnail != null && fullImage != null)
            callback(MPDAlbumArt(key, fullImage = fullImage.asImageBitmap(), thumbnail = thumbnail.asImageBitmap()))
        else if (!_fetchedAlbumArtKeys.contains(key)) {
            if (key.filename == null) {
                // First we must have the filename of a song. Then try again.
                // Perhaps a song already exists in _songs?
                val song = repo.songs.value.filterByAlbum(key.albumArtist, key.album).firstOrNull()
                if (song != null) {
                    ioScope.launch {
                        getAlbumArt(key.copy(filename = song.filename), requestType, callback)
                    }
                } else {
                    // Otherwise, fetch songs.
                    repo.fetchSongs(key.albumArtist, key.album) { songs ->
                        songs.firstOrNull()?.let { song ->
                            ioScope.launch {
                                getAlbumArt(key.copy(filename = song.filename), requestType, callback)
                            }
                        }
                    }
                }
            } else {
                // We have a song filename, now we can fetch albumart:
                repo.binaryClient.value?.enqueue("albumart", listOf(key.filename)) { response ->
                    _fetchedAlbumArtKeys.add(key)
                    if (response.isSuccess) {
                        ioScope.launch {
                            saveAlbumArt(key = key, data = response.binaryResponse, callback = callback)
                        }
                    }
                }
            }
        }
    }

    @Suppress("RedundantSuspendModifier")
    private suspend fun createThumbnail(fullImage: Bitmap, imageFilename: String): Bitmap {
        val factor = Constants.THUMBNAIL_SIZE.toFloat() / max(fullImage.width, fullImage.height)
        val width = (fullImage.width * factor).roundToInt()
        val height = (fullImage.height * factor).roundToInt()
        val file = File(thumbnailDir, imageFilename)
        val thumbnail = fullImage.scale(width, height)

        file.outputStream().use { outputStream ->
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        }
        return thumbnail
    }

    private suspend fun saveAlbumArt(
        key: AlbumArtKey,
        data: ByteArray,
        callback: (MPDAlbumArt) -> Unit,
    ) {
        BitmapFactory.decodeByteArray(data, 0, data.size)?.also { fullImage ->
            val fullImageFile = File(albumArtDir, key.imageFilename)
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
                fullImageFile.outputStream().use { outputStream -> outputStream.write(data) }
            }
        }
    }
}

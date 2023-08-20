package us.huseli.umpc.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.scale
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.umpc.Constants
import us.huseli.umpc.LoggerInterface
import us.huseli.umpc.data.AlbumArtKey
import us.huseli.umpc.data.MPDAlbumArt
import us.huseli.umpc.toBitmap
import java.io.ByteArrayInputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

@Singleton
class AlbumArtRepository @Inject constructor(
    private val mpdRepo: MPDRepository,
    private val ioScope: CoroutineScope,
    @ApplicationContext context: Context,
) : LoggerInterface {
    private val _currentSongAlbumArt = MutableStateFlow<MPDAlbumArt?>(null)
    private val albumArtDirectory = File(context.cacheDir, "albumArt").apply { mkdirs() }
    private val thumbnailDirectory = File(albumArtDirectory, "thumbnails").apply { mkdirs() }
    private val onReadyCallbacks = mutableMapOf<AlbumArtKey, List<(MPDAlbumArt) -> Unit>>()
    private val pendingKeys = mutableListOf<AlbumArtKey>()

    init {
        ioScope.launch {
            mpdRepo.isIOError.collect {
                if (it) _currentSongAlbumArt.value = null
            }
        }
        ioScope.launch {
            mpdRepo.currentSong.map { it?.albumArtKey }.filterNotNull().distinctUntilChanged().collect { key ->
                _currentSongAlbumArt.value = null
                getAlbumArt(key) {
                    _currentSongAlbumArt.value = it
                }
            }
        }
    }

    val currentSongAlbumArt = _currentSongAlbumArt.asStateFlow()

    private fun addOnReadyCallback(key: AlbumArtKey, callback: (MPDAlbumArt) -> Unit) {
        onReadyCallbacks[key] = onReadyCallbacks[key]?.let { it + callback } ?: listOf(callback)
    }

    private fun runOnReadyCallbacks(key: AlbumArtKey, albumArt: MPDAlbumArt) {
        pendingKeys.remove(key)
        onReadyCallbacks.remove(key)?.forEach { it.invoke(albumArt) }
    }

    fun getAlbumArt(key: AlbumArtKey, onFinish: (MPDAlbumArt) -> Unit) = ioScope.launch {
        if (pendingKeys.contains(key)) {
            addOnReadyCallback(key, onFinish)
        } else {
            pendingKeys.add(key)

            val fullImage: Bitmap? = File(albumArtDirectory, key.imageFilename).toBitmap()
            val thumbnail: Bitmap? = File(thumbnailDirectory, key.imageFilename).toBitmap()

            if (thumbnail != null && fullImage != null) {
                val albumArt =
                    MPDAlbumArt(key, fullImage = fullImage.asImageBitmap(), thumbnail = thumbnail.asImageBitmap())
                onFinish(albumArt)
                runOnReadyCallbacks(key, albumArt)
            } else {
                mpdRepo.getAlbumArt(key) { response ->
                    if (response.isSuccess) saveAlbumArt(key = key, stream = response.stream) { albumArt ->
                        onFinish(albumArt)
                        runOnReadyCallbacks(key, albumArt)
                    }
                }
            }
        }
    }

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

    private fun saveAlbumArt(
        key: AlbumArtKey,
        stream: ByteArrayInputStream,
        onFinish: (MPDAlbumArt) -> Unit,
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

                onFinish(MPDAlbumArt(key, scaledFullImage.asImageBitmap(), thumbnail.asImageBitmap()))
                fullImageFile.outputStream().use { outputStream ->
                    scaledFullImage.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                }
            } else {
                onFinish(MPDAlbumArt(key, fullImage.asImageBitmap(), thumbnail.asImageBitmap()))
                fullImageFile.outputStream().use { outputStream ->
                    stream.reset()
                    outputStream.write(stream.readBytes())
                }
            }
        }
    }
}
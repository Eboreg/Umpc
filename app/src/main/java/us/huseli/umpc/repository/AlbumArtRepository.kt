package us.huseli.umpc.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.ImageView
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.scale
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import us.huseli.umpc.BuildConfig
import us.huseli.umpc.Constants.ALBUM_ART_MAXSIZE
import us.huseli.umpc.Constants.THUMBNAIL_SIZE
import us.huseli.umpc.LoggerInterface
import us.huseli.umpc.data.AlbumArtKey
import us.huseli.umpc.data.MPDAlbumArt
import us.huseli.umpc.toBitmap
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URLEncoder
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.max
import kotlin.math.roundToInt

data class SpotifyAccessToken(
    val token: String,
    private val expiresIn: Long,
    val expires: Instant = Instant.now().plusSeconds(expiresIn)
)

@Singleton
class AlbumArtRepository @Inject constructor(
    private val mpdRepo: MPDRepository,
    private val settingsRepo: SettingsRepository,
    private val ioScope: CoroutineScope,
    @ApplicationContext private val context: Context,
) : LoggerInterface {
    private val _currentSongAlbumArt = MutableStateFlow<MPDAlbumArt?>(null)
    private val albumArtDirectory = File(context.cacheDir, "albumArt").apply { mkdirs() }
    private val thumbnailDirectory = File(albumArtDirectory, "thumbnails").apply { mkdirs() }
    private val onReadyCallbacks = mutableMapOf<AlbumArtKey, List<(MPDAlbumArt) -> Unit>>()
    private val pendingKeys = mutableListOf<AlbumArtKey>()
    private val mutex = Mutex()
    private val gson: Gson = GsonBuilder().create()
    private var spotifyAccessToken: SpotifyAccessToken? = null

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

    private suspend fun runOnReadyCallbacks(key: AlbumArtKey, albumArt: MPDAlbumArt) {
        mutex.withLock {
            pendingKeys.remove(key)
            onReadyCallbacks.remove(key)?.forEach { it.invoke(albumArt) }
        }
    }

    fun getAlbumArt(key: AlbumArtKey, onFinish: (MPDAlbumArt) -> Unit) = ioScope.launch {
        var runNow = false

        mutex.withLock {
            onReadyCallbacks[key] = onReadyCallbacks[key]?.let { it + onFinish } ?: listOf(onFinish)
            if (!pendingKeys.contains(key)) {
                pendingKeys.add(key)
                runNow = true
            }
        }

        if (runNow) {
            val fullImage: Bitmap? = File(albumArtDirectory, key.imageFilename).toBitmap()
            val thumbnail: Bitmap? = File(thumbnailDirectory, key.imageFilename).toBitmap()

            if (thumbnail != null && fullImage != null) {
                val albumArt =
                    MPDAlbumArt(key, fullImage = fullImage.asImageBitmap(), thumbnail = thumbnail.asImageBitmap())
                runOnReadyCallbacks(key, albumArt)
            } else {
                mpdRepo.getAlbumArt(key) { response ->
                    if (response.isSuccess) {
                        saveAlbumArt(key = key, stream = response.stream) { albumArt ->
                            ioScope.launch { runOnReadyCallbacks(key, albumArt) }
                        }
                    } else if (settingsRepo.fetchSpotifyAlbumArt.value) {
                        getSpotifyAlbumArt(key) { bitmap ->
                            saveAlbumArt(key = key, fullImage = bitmap) { albumArt ->
                                ioScope.launch { runOnReadyCallbacks(key, albumArt) }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createThumbnail(fullImage: Bitmap, imageFilename: String): Bitmap {
        val factor = THUMBNAIL_SIZE.toFloat() / max(fullImage.width, fullImage.height)
        val width = (fullImage.width * factor).roundToInt()
        val height = (fullImage.height * factor).roundToInt()
        val file = File(thumbnailDirectory, imageFilename)
        val thumbnail = fullImage.scale(width, height)

        file.outputStream().use { outputStream ->
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        }
        return thumbnail
    }

    private inline fun saveAlbumArt(key: AlbumArtKey, fullImage: Bitmap, onFinish: (MPDAlbumArt) -> Unit) {
        val fullImageFile = File(albumArtDirectory, key.imageFilename)
        val factor = ALBUM_ART_MAXSIZE.toFloat() / max(fullImage.width, fullImage.height)
        val thumbnail = createThumbnail(fullImage, key.imageFilename)
        // Scale down full image if it happens to be very large:
        val scaledFullImage: Bitmap = if (factor < 0) {
            val width = (fullImage.width * factor).roundToInt()
            val height = (fullImage.height * factor).roundToInt()
            fullImage.scale(width, height)
        } else {
            fullImage
        }

        onFinish(MPDAlbumArt(key, scaledFullImage.asImageBitmap(), thumbnail.asImageBitmap()))
        fullImageFile.outputStream().use { outputStream ->
            scaledFullImage.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        }
    }

    private inline fun saveAlbumArt(
        key: AlbumArtKey,
        stream: ByteArrayInputStream,
        onFinish: (MPDAlbumArt) -> Unit,
    ) {
        BitmapFactory.decodeStream(stream)?.also { fullImage -> saveAlbumArt(key, fullImage, onFinish) }
    }

    @Suppress("UnnecessaryOptInAnnotation")
    @OptIn(ExperimentalEncodingApi::class)
    private fun getSpotifyAccessToken(onSuccess: (SpotifyAccessToken) -> Unit) {
        spotifyAccessToken?.takeIf { it.expires > Instant.now() }?.let(onSuccess) ?: kotlin.run {
            val authString =
                Base64.encode("${BuildConfig.spotifyClientId}:${BuildConfig.spotifyClientSecret}".toByteArray())
            val queue = Volley.newRequestQueue(context)
            val request: StringRequest = object : StringRequest(
                Method.POST,
                "https://accounts.spotify.com/api/token",
                { response ->
                    log("getSpotifyAccessToken: response: $response")
                    gson.fromJson<Map<String, Any>>(response, Map::class.java)?.also { json ->
                        val token = json["access_token"] as? String
                        val expiresIn = (json["expires_in"] as? Double)?.toLong()
                        if (token != null && expiresIn != null) {
                            SpotifyAccessToken(token, expiresIn).also {
                                spotifyAccessToken = it
                                onSuccess(it)
                            }
                        }
                    }
                },
                { error -> logError("response error: $error", error) },
            ) {
                override fun getBodyContentType() = "application/x-www-form-urlencoded"

                override fun getHeaders(): Map<String, String> = mapOf(
                    "Content-Type" to "application/x-www-form-urlencoded",
                    "Accept" to "application/json",
                    "Authorization" to "Basic $authString",
                )

                override fun getParams(): Map<String, String> = mapOf("grant_type" to "client_credentials")
            }
            queue.add(request)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getSpotifyAlbumArtUrl(key: AlbumArtKey, onSuccess: (String) -> Unit) {
        getSpotifyAccessToken { token ->
            val q = URLEncoder.encode("artist:${key.artist} album:${key.album}", "utf8")
            val queue = Volley.newRequestQueue(context)
            val request: StringRequest = object : StringRequest(
                Method.GET,
                "https://api.spotify.com/v1/search?q=$q&type=album",
                { response ->
                    log("getSpotifyAlbumArtUrl: key=$key, q=$q, response: $response")
                    val map = gson.fromJson<Map<String, Any>>(response, Map::class.java)
                    val albums = (map?.get("albums") as? Map<String, Any>)?.get("items") as? List<Map<String, Any>>
                    val exactMatch = albums?.firstOrNull { it["name"] == key.album }
                    val images = (exactMatch ?: albums?.firstOrNull())?.get("images") as? List<Map<String, Any>>
                    (images?.firstOrNull()?.get("url") as? String)?.let(onSuccess)
                },
                { error -> logError("response: $error", error) },
            ) {
                override fun getHeaders(): Map<String, String> = mapOf("Authorization" to "Bearer ${token.token}")
            }
            queue.add(request)
        }
    }

    private fun getSpotifyAlbumArt(key: AlbumArtKey, onSuccess: (Bitmap) -> Unit) {
        getSpotifyAlbumArtUrl(key) { url ->
            log("getSpotifyAlbumArt: artist=${key.artist}, album=${key.album}, got url=$url")
            val queue = Volley.newRequestQueue(context)
            val request =
                ImageRequest(url, { onSuccess(it) }, 0, 0, ImageView.ScaleType.CENTER_INSIDE, Bitmap.Config.ALPHA_8) {}
            queue.add(request)
        }
    }
}

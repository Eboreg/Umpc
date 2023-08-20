package us.huseli.umpc.mpd.response

import android.util.Log
import us.huseli.umpc.LoggerInterface
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDOutput
import us.huseli.umpc.data.MPDPlaylist
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.data.MPDStatus
import us.huseli.umpc.data.toMPDAlbums
import us.huseli.umpc.data.toMPDOutput
import us.huseli.umpc.data.toMPDPlaylist
import us.huseli.umpc.data.toMPDSong
import us.huseli.umpc.data.toMPDStatus

class MPDTextResponse : BaseMPDResponse(), LoggerInterface {
    fun extractAlbums(): List<MPDAlbum> = _responseLines.responseToMultimapList().flatMap { it.toMPDAlbums() }

    fun extractAlbums(artist: String): List<MPDAlbum> = _responseLines.responseToMultimap().toMPDAlbums(artist)

    fun extractOutputs(): List<MPDOutput> = _responseLines.responseToMapList().mapNotNull { it.toMPDOutput() }

    fun extractPlaylists(): List<MPDPlaylist> = _responseLines.responseToMapList().mapNotNull { it.toMPDPlaylist() }

    fun extractSong(): MPDSong? = _responseLines.responseToMap().toMPDSong()

    fun extractSongs(): List<MPDSong> = _responseLines.responseToMapList().mapNotNull { it.toMPDSong() }

    fun extractPositionedSongs(): List<MPDSong> =
        _responseLines.responseToMapList().mapIndexedNotNull { index, map ->
            map.toMPDSong(position = index)
        }

    fun extractStatus(): MPDStatus? = _responseLines.responseToMap().toMPDStatus()

    fun extractValues(key: String): List<String> = extractValuesOrNull(key) ?: emptyList()

    fun extractValuesOrNull(key: String): List<String>? = _responseLines.responseToMultimap()[key]

    fun extractMap(): Map<String, String> =
        _responseLines.responseToMap().also { log("extractMap: $it", Log.DEBUG) }

    override fun toString() =
        "${javaClass.simpleName}[status=$status, error=$error, mpdError=$mpdError, response=$_responseLines]"
}

fun List<String>.responseToMultimapList(): List<Map<String, List<String>>> {
    /** Split lines where keys start to repeat. */
    var currentMap = mutableMapOf<String, List<String>>()
    val maps = mutableListOf<Map<String, List<String>>>()
    var startKey: String? = null

    forEach { line ->
        if (BaseMPDResponse.responseRegex.matches(line)) {
            BaseMPDResponse.parseResponseLine(line)?.let { (key, value) ->
                if (startKey == null) startKey = key.lowercase()
                else if (key.lowercase() == startKey) {
                    maps.add(currentMap)
                    currentMap = mutableMapOf()
                }
                currentMap[key.lowercase()] = currentMap[key.lowercase()]?.let { it + value } ?: listOf(value)
            }
        }
    }
    maps.add(currentMap)
    return maps
}

fun List<String>.responseToMapList(): List<Map<String, String>> =
    responseToMultimapList().map { map -> map.mapValues { it.value.first() } }

fun List<String>.responseToMultimap(): Map<String, List<String>> {
    val map = mutableMapOf<String, List<String>>()

    forEach { line ->
        BaseMPDResponse.parseResponseLine(line)?.let { (key, value) ->
            map[key.lowercase()] = map[key.lowercase()]?.let { it + value } ?: listOf(value)
        }
    }
    return map
}

fun List<String>.responseToMap(): Map<String, String> = responseToMultimap().mapValues { it.value.first() }

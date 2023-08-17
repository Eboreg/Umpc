package us.huseli.umpc.mpd.response

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
import us.huseli.umpc.data.toMPDSongs
import us.huseli.umpc.data.toMPDStatus

class MPDTextResponse : BaseMPDResponse(), LoggerInterface {
    private val _responseLines = mutableListOf<String>()

    val batchResponseMaps: List<Map<String, List<String>>>
        get() = extractListMapList(_responseLines)

    fun putLine(line: String) {
        _responseLines.add(line)
    }

    fun extractAlbums(): List<MPDAlbum> = extractListMapList(_responseLines).flatMap { it.toMPDAlbums() }

    fun extractAlbums(artist: String): List<MPDAlbum> = extractListMap(_responseLines).toMPDAlbums(artist)

    fun extractMap(): Map<String, String> =
        extractListMap(_responseLines).mapValues { it.value.first() }.also { log("extractMap: $it") }

    fun extractOutputs(): List<MPDOutput> = extractMapList(_responseLines).mapNotNull { it.toMPDOutput() }

    fun extractPlaylists(): List<MPDPlaylist> = extractMapList(_responseLines).mapNotNull { it.toMPDPlaylist() }

    fun extractSong(): MPDSong? = extractMap().toMPDSong()

    fun extractSongs(): List<MPDSong> = extractMapList(_responseLines).mapNotNull { it.toMPDSong() }

    fun extractNestedSongs(): List<List<MPDSong>> = extractMapListList(_responseLines).map { it.toMPDSongs() }

    fun extractPositionedNestedSongs(): List<List<MPDSong>> = extractMapListList(_responseLines).map { maps ->
        maps.mapIndexedNotNull { index, map ->  map.toMPDSong(position = index) }
    }

    fun extractPositionedSongs(): List<MPDSong> =
        extractMapList(_responseLines).mapIndexedNotNull { index, map ->
            map.toMPDSong(position = index)
        }

    fun extractStatus(): MPDStatus? = extractMap().toMPDStatus()

    fun extractValues(key: String) = _responseLines.mapNotNull { line ->
        parseResponseLine(line)?.takeIf { it.first == key }?.second
    }

    private fun extractListMap(lines: List<String>): Map<String, List<String>> {
        val map = mutableMapOf<String, List<String>>()

        lines.forEach { line ->
            if (responseRegex.matches(line)) {
                parseResponseLine(line)?.let { (key, value) ->
                    map[key] = map[key]?.let { it + value } ?: listOf(value)
                }
            }
        }
        log("extractListMap: $map")
        return map
    }

    private fun extractMapList(lines: List<String>): List<Map<String, String>> =
        extractMapListList(lines).flatten().also { log("extractMapList: $it") }

    private fun extractMapListList(lines: List<String>): List<List<Map<String, String>>> {
        /** The outermost list separates the "list_OK" separated sections. */
        var currentMap = mutableMapOf<String, String>()
        var currentList = mutableListOf<Map<String, String>>()
        val lists = mutableListOf<List<Map<String, String>>>()
        var startKey: String? = null
        var lastLine: String? = null
        var lastKey: String? = null

        lines.forEach { line ->
            if (responseRegex.matches(line)) {
                parseResponseLine(line)?.let { (key, value) ->
                    if (startKey == null) startKey = key
                    else if (key == startKey) {
                        currentList.add(currentMap)
                        currentMap = mutableMapOf()
                    }
                    currentMap[key] = value
                    lastKey = key
                }
            } else if (line == "list_OK") {
                currentList.add(currentMap)
                lists.add(currentList)
                currentList = mutableListOf()
                currentMap = mutableMapOf()
                startKey = null
            }
            lastLine = line
        }
        if (lastLine != "list_OK" && lastLine != null) {
            if (lastKey != startKey) currentList.add(currentMap)
            lists.add(currentList)
        }
        return lists
    }

    private fun extractListMapList(lines: List<String>): List<Map<String, List<String>>> {
        var currentMap = mutableMapOf<String, List<String>>()
        val maps = mutableListOf<Map<String, List<String>>>()
        var lastLine: String? = null
        var startKey: String? = null

        lines.forEach { line ->
            if (responseRegex.matches(line)) {
                parseResponseLine(line)?.let { (key, value) ->
                    if (startKey == null) startKey = key
                    else if (key == startKey) {
                        maps.add(currentMap)
                        currentMap = mutableMapOf()
                    }
                    currentMap[key] = currentMap[key]?.let { it + value } ?: listOf(value)
                }
            } else if (line == "list_OK") {
                maps.add(currentMap)
                currentMap = mutableMapOf()
                startKey = null
            }
            lastLine = line
        }
        if (lastLine != "list_OK" && lastLine != null) maps.add(currentMap)
        log("extractBatchListMapList: $maps")
        return maps
    }

    override fun toString() = "${javaClass.simpleName}[status=$status, error=$error, response=$_responseLines]"
}
